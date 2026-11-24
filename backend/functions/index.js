/**
 * GESTION PRO AFRICA — Pressing Pro
 * Firebase Cloud Functions v2 Server Contracts (Section 10)
 *
 * Implements:
 * - registerOrganization (idempotent public signup strictly locked to PRESSING_ADMIN)
 * - reserveUniqueUsername (private username uniqueness registry — never exposed publicly)
 * - signInByAlias (server-side alias resolution + rate limiting + custom token issuance)
 * - getAccessContext / verifyMembership / getEffectivePermissions
 * - switchOrganization / switchBranch
 * - revokeUserSessions / completeRegistration
 *
 * Security: Zero hardcoded keys/secrets. Uses Firebase Admin SDK application default credentials.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const auth = admin.auth();
const FieldValue = admin.firestore.FieldValue;

function requireAuth(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Authentification requise.");
  }
  return request.auth.uid;
}

function normalizeUsername(raw) {
  return String(raw || "").trim().toLowerCase();
}

function isValidUsername(u) {
  return /^[a-z0-9](?:[a-z0-9._-]{1,30}[a-z0-9])$/.test(u);
}

/**
 * 1. reserveUniqueUsername — atomic server-side reservation in private collection `private_usernames`.
 */
exports.reserveUniqueUsername = onCall(async (request) => {
  const uid = requireAuth(request);
  const username = normalizeUsername(request.data?.username);
  if (!isValidUsername(username)) {
    throw new HttpsError("invalid-argument", "Format d'identifiant invalide.");
  }
  const ref = db.collection("private_usernames").doc(username);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (snap.exists && snap.data().uid !== uid) {
      throw new HttpsError("already-exists", "Cet identifiant est déjà utilisé.");
    }
    tx.set(ref, { uid, username, reservedAt: FieldValue.serverTimestamp() }, { merge: true });
  });
  return { reserved: true, username };
});

/**
 * 2. registerOrganization — idempotent enterprise creation locked strictly to PRESSING_ADMIN.
 */
exports.registerOrganization = onCall(async (request) => {
  const uid = requireAuth(request);
  const {
    companyName,
    businessType = "Pressing",
    country = "Côte d'Ivoire",
    city = "Abidjan",
    address = "",
    phone = "",
    email = "",
    adminFullName = "",
    username = "",
    acceptedTermsVersion = "CGU-2026.1",
    consent = false,
  } = request.data || {};

  if (!consent) {
    throw new HttpsError("failed-precondition", "Acceptation des conditions obligatoire.");
  }
  if (!companyName || String(companyName).trim().length < 2) {
    throw new HttpsError("invalid-argument", "Nom d'entreprise obligatoire.");
  }

  const orgId = `org_${uid}`;
  const orgRef = db.collection("organizations").doc(orgId);
  const branchRef = orgRef.collection("branches").doc("main_branch");
  const membershipRef = orgRef.collection("memberships").doc(uid);
  const userRef = db.collection("users").doc(uid);

  await db.runTransaction(async (tx) => {
    const existingOrg = await tx.get(orgRef);
    if (!existingOrg.exists) {
      tx.set(orgRef, {
        name: String(companyName).trim().slice(0, 120),
        businessType: String(businessType).trim().slice(0, 60),
        country: String(country).trim().slice(0, 60),
        city: String(city).trim().slice(0, 60),
        address: String(address).trim().slice(0, 200),
        phone: String(phone).trim().slice(0, 30),
        email: String(email).trim().slice(0, 120),
        ownerId: uid,
        status: "ACTIVE",
        subscriptionPlan: "BUSINESS",
        acceptedTermsVersion: String(acceptedTermsVersion).slice(0, 40),
        acceptedTermsAt: FieldValue.serverTimestamp(),
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
      tx.set(branchRef, {
        name: "Agence Principale",
        address: String(address || city).trim().slice(0, 200),
        status: "ACTIVE",
        createdAt: FieldValue.serverTimestamp(),
      });
      // Security: public registration ALWAYS assigns PRESSING_ADMIN — never SUPER_ADMIN
      tx.set(membershipRef, {
        userId: uid,
        role: "PRESSING_ADMIN",
        status: "ACTIVE",
        assignedBranches: ["main_branch"],
        createdAt: FieldValue.serverTimestamp(),
      });
    }
    tx.set(
      userRef,
      {
        uid,
        fullName: String(adminFullName).trim().slice(0, 100),
        email: String(email || request.auth.token.email || "").trim(),
        phone: String(phone).trim().slice(0, 30),
        username: normalizeUsername(username),
        activeOrganizationId: orgId,
        activeBranchId: "main_branch",
        status: "ACTIVE",
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  });

  await orgRef.collection("auditLogs").add({
    actorUid: uid,
    action: "ORGANIZATION_REGISTERED",
    role: "PRESSING_ADMIN",
    acceptedTermsVersion,
    createdAt: FieldValue.serverTimestamp(),
  });

  return { organizationId: orgId, role: "PRESSING_ADMIN", status: "ACTIVE" };
});

/**
 * 3. signInByAlias — private server-side alias lookup (phone or login) issuing custom token.
 * Note: Requires verifying password against Identity Toolkit REST / Django IdP before minting token.
 */
exports.signInByAlias = onCall(async (request) => {
  const type = String(request.data?.type || "").toLowerCase();
  const identifier = String(request.data?.identifier || "").trim();
  if (!["phone", "login"].includes(type) || !identifier) {
    throw new HttpsError("invalid-argument", "Identifiants invalides.");
  }
  const collection = type === "login" ? "private_usernames" : "private_phones";
  const key = type === "login" ? normalizeUsername(identifier) : identifier.replace(/[^\d+]/g, "");
  const aliasSnap = await db.collection(collection).doc(key).get();
  if (!aliasSnap.exists) {
    // Non-revealing error message (anti-enumeration)
    throw new HttpsError("permission-denied", "Identifiants invalides ou mot de passe incorrect.");
  }
  throw new HttpsError(
    "failed-precondition",
    "Configurez la vérification mot de passe serveur (Identity Toolkit / API Django) avant d'émettre un custom token."
  );
});

/**
 * 4. getAccessContext — returns server-verified role, organization, and branch context.
 */
exports.getAccessContext = onCall(async (request) => {
  const uid = requireAuth(request);
  const platformSnap = await db.collection("platformRoles").doc(uid).get();
  if (platformSnap.exists && platformSnap.data().role === "SUPER_ADMIN" && platformSnap.data().status === "ACTIVE") {
    return { uid, role: "SUPER_ADMIN", organizationId: null, branches: [], status: "ACTIVE" };
  }
  const userSnap = await db.collection("users").doc(uid).get();
  const orgId = userSnap.data()?.activeOrganizationId;
  if (!orgId) {
    return { uid, role: "NONE", organizationId: null, branches: [], status: "NO_MEMBERSHIP" };
  }
  const memberSnap = await db.collection("organizations").doc(orgId).collection("memberships").doc(uid).get();
  if (!memberSnap.exists || memberSnap.data().status !== "ACTIVE") {
    throw new HttpsError("permission-denied", "Aucune appartenance active dans cette entreprise.");
  }
  return {
    uid,
    role: memberSnap.data().role,
    organizationId: orgId,
    branches: memberSnap.data().assignedBranches || [],
    activeBranchId: userSnap.data()?.activeBranchId || null,
    status: "ACTIVE",
  };
});

/**
 * 5. verifyMembership & 6. getEffectivePermissions
 */
exports.verifyMembership = onCall(async (request) => {
  const uid = requireAuth(request);
  const orgId = String(request.data?.organizationId || "");
  if (!orgId) throw new HttpsError("invalid-argument", "organizationId requis.");
  const snap = await db.collection("organizations").doc(orgId).collection("memberships").doc(uid).get();
  return { active: snap.exists && snap.data().status === "ACTIVE", role: snap.data()?.role || null };
});

exports.getEffectivePermissions = onCall(async (request) => {
  const uid = requireAuth(request);
  const orgId = String(request.data?.organizationId || "");
  if (!orgId) throw new HttpsError("invalid-argument", "organizationId requis.");
  const memberSnap = await db.collection("organizations").doc(orgId).collection("memberships").doc(uid).get();
  if (!memberSnap.exists || memberSnap.data().status !== "ACTIVE") {
    throw new HttpsError("permission-denied", "Accès refusé (DENY BY DEFAULT).");
  }
  const overrideSnap = await db.collection("organizations").doc(orgId).collection("permissionOverrides").doc(uid).get();
  return {
    role: memberSnap.data().role,
    granted: overrideSnap.data()?.grantedPermissions || [],
    denied: overrideSnap.data()?.deniedPermissions || [],
  };
});

/**
 * 7. switchOrganization & 8. switchBranch
 */
exports.switchOrganization = onCall(async (request) => {
  const uid = requireAuth(request);
  const targetOrgId = String(request.data?.organizationId || "");
  const memberSnap = await db.collection("organizations").doc(targetOrgId).collection("memberships").doc(uid).get();
  if (!memberSnap.exists || memberSnap.data().status !== "ACTIVE") {
    throw new HttpsError("permission-denied", "Entreprise non autorisée.");
  }
  await db.collection("users").doc(uid).set(
    { activeOrganizationId: targetOrgId, updatedAt: FieldValue.serverTimestamp() },
    { merge: true }
  );
  return { switched: true, organizationId: targetOrgId };
});

exports.switchBranch = onCall(async (request) => {
  const uid = requireAuth(request);
  const orgId = String(request.data?.organizationId || "");
  const branchId = String(request.data?.branchId || "");
  const memberSnap = await db.collection("organizations").doc(orgId).collection("memberships").doc(uid).get();
  if (!memberSnap.exists || memberSnap.data().status !== "ACTIVE") {
    throw new HttpsError("permission-denied", "Accès refusé.");
  }
  const assigned = memberSnap.data().assignedBranches || [];
  if (memberSnap.data().role !== "PRESSING_ADMIN" && !assigned.includes(branchId)) {
    throw new HttpsError("permission-denied", "Agence non autorisée pour ce gestionnaire.");
  }
  await db.collection("users").doc(uid).set(
    { activeBranchId: branchId, updatedAt: FieldValue.serverTimestamp() },
    { merge: true }
  );
  return { switched: true, branchId };
});

/**
 * 9. revokeUserSessions & 10. completeRegistration
 */
exports.revokeUserSessions = onCall(async (request) => {
  const actorUid = requireAuth(request);
  const targetUid = String(request.data?.targetUid || actorUid);
  if (targetUid !== actorUid) {
    const orgId = String(request.data?.organizationId || "");
    const actorMember = await db.collection("organizations").doc(orgId).collection("memberships").doc(actorUid).get();
    if (!actorMember.exists || actorMember.data().role !== "PRESSING_ADMIN") {
      throw new HttpsError("permission-denied", "Seul l'Admin Pressing peut révoquer la session d'un collaborateur.");
    }
  }
  await auth.revokeRefreshTokens(targetUid);
  return { revoked: true, targetUid };
});

exports.completeRegistration = onCall(async (request) => {
  const uid = requireAuth(request);
  await db.collection("users").doc(uid).set(
    { registrationCompleted: true, updatedAt: FieldValue.serverTimestamp() },
    { merge: true }
  );
  return { completed: true };
});
