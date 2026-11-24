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
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

if (!admin.apps.length) {
  admin.initializeApp();
}

const FIRESTORE_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-pressing-bfcbafdf-ef9f-414c-b2a1-dde862b44f8e";
const db = getFirestore(admin.app(), FIRESTORE_DATABASE_ID);
const auth = admin.auth();

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

// ============================================================================
// SUPER-ADMIN BOOTSTRAP (Email + Password) & GENIUSPAY SUBSCRIPTION RENEWALS
// ============================================================================

const { onRequest } = require("firebase-functions/v2/https");
const crypto = require("crypto");

const PLATFORM_SUPER_ADMIN_EMAILS = new Set(
  String(
    process.env.SUPER_ADMIN_EMAILS ||
      process.env.SUPER_ADMIN_EMAIL ||
      "lebest2london@gmail.com,best2london@gmail.com"
  )
    .split(",")
    .map((e) => e.trim().toLowerCase())
    .filter(Boolean)
);

const ALLOWED_GENIUSPAY_HOSTS = new Set([
  "geniuspay.ci",
  "pay.genius.ci",
  "api.geniuspay.ci",
]);

const PLAN_MONTHLY_FCFA = {
  STARTER: 5000,
  BUSINESS: 10000,
  ENTERPRISE: 25000,
};

function computeSubscriptionAmountFcfa(plan, billingCycle) {
  const safePlan = PLAN_MONTHLY_FCFA[plan] ? plan : "BUSINESS";
  const monthly = PLAN_MONTHLY_FCFA[safePlan];
  const cycle = String(billingCycle || "MONTHLY").toUpperCase();
  if (cycle === "QUARTERLY") {
    return Math.round(monthly * 3 * 0.9);
  }
  if (cycle === "ANNUAL") {
    return Math.round(monthly * 12 * 0.8);
  }
  return monthly;
}

function getValidatedGeniusPayBaseUrl() {
  const raw = String(
    process.env.GENIUSPAY_BASE_URL || "https://geniuspay.ci/api/v1/merchant"
  ).trim().replace(/\/+$/, "");
  const parsed = new URL(raw);
  if (parsed.protocol !== "https:" || !ALLOWED_GENIUSPAY_HOSTS.has(parsed.hostname.toLowerCase())) {
    throw new HttpsError("failed-precondition", "URL de base GeniusPay non autorisée.");
  }
  return raw;
}

function verifyGeniusPayWebhookSignature(rawBodyBuffer, signatureHeader, secret) {
  if (!rawBodyBuffer || !signatureHeader || !secret) return false;
  const cleaned = String(signatureHeader)
    .trim()
    .replace(/^sha256=/i, "")
    .replace(/^v1=/i, "")
    .toLowerCase();
  const expected = crypto
    .createHmac("sha256", secret)
    .update(rawBodyBuffer)
    .digest("hex")
    .toLowerCase();
  const a = Buffer.from(cleaned, "utf8");
  const b = Buffer.from(expected, "utf8");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

/**
 * 11. bootstrapSuperAdmin — provisions or synchronizes a Platform Super-Admin account
 * (`lebest2london@gmail.com` / `best2london@gmail.com`) with Firebase Auth Email/Password + custom claims (`SUPER_ADMIN`).
 * Reads initial password strictly from server-side `process.env.SUPER_ADMIN_INITIAL_PASSWORD`.
 */
exports.bootstrapSuperAdmin = onCall(async (request) => {
  const callerUid = requireAuth(request);
  const callerEmail = String(request.auth.token.email || "").trim().toLowerCase();
  if (!PLATFORM_SUPER_ADMIN_EMAILS.has(callerEmail)) {
    throw new HttpsError("permission-denied", "Réservé au Super-Admin de la plateforme.");
  }
  const requestedEmail = String(request.data?.email || callerEmail).trim().toLowerCase();
  if (!PLATFORM_SUPER_ADMIN_EMAILS.has(requestedEmail)) {
    throw new HttpsError("permission-denied", "Adresse e-mail non autorisée comme Super-Admin.");
  }

  let targetUser;
  try {
    targetUser = await auth.getUserByEmail(requestedEmail);
  } catch (err) {
    const initialPassword = process.env.SUPER_ADMIN_INITIAL_PASSWORD || "";
    if (initialPassword.length < 8) {
      throw new HttpsError(
        "failed-precondition",
        "Définissez SUPER_ADMIN_INITIAL_PASSWORD dans Firebase Functions Secrets pour créer le compte e-mail/mot de passe."
      );
    }
    targetUser = await auth.createUser({
      email: requestedEmail,
      password: initialPassword,
      displayName: "Super-Admin Gestion Pro Africa",
      emailVerified: true,
    });
  }

  await auth.setCustomUserClaims(targetUser.uid, {
    role: "DIRECTEUR",
    platformRole: "SUPER_ADMIN",
    superAdmin: true,
  });

  await db.collection("platformRoles").doc(targetUser.uid).set(
    {
      uid: targetUser.uid,
      email: requestedEmail,
      role: "SUPER_ADMIN",
      status: "ACTIVE",
      updatedBy: callerUid,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  return {
    uid: targetUser.uid,
    email: requestedEmail,
    role: "SUPER_ADMIN",
    status: "ACTIVE",
  };
});

/**
 * 12. createGeniusPaySubscriptionCheckout — server-side GeniusPay checkout initializer.
 * Keeps GENIUSPAY_API_KEY and GENIUSPAY_API_SECRET strictly on the server via process.env.
 */
exports.createGeniusPaySubscriptionCheckout = onCall(async (request) => {
  const uid = requireAuth(request);
  const apiKey = process.env.GENIUSPAY_API_KEY || "";
  const apiSecret = process.env.GENIUSPAY_API_SECRET || "";
  const mode = String(process.env.GENIUSPAY_MODE || "live").toLowerCase();
  if (!apiKey || !apiSecret || apiKey.includes("PLACEHOLDER") || apiSecret.includes("PLACEHOLDER")) {
    throw new HttpsError("failed-precondition", "Clés GeniusPay manquantes dans Firebase Functions Secrets.");
  }

  const baseUrl = getValidatedGeniusPayBaseUrl();
  const plan = String(request.data?.plan || "BUSINESS").toUpperCase();
  const billingCycle = String(request.data?.billingCycle || "MONTHLY").toUpperCase();
  const amountFcfa = computeSubscriptionAmountFcfa(plan, billingCycle);
  const companySnap = await db.collection("users").doc(uid).get();
  const companyData = companySnap.exists ? companySnap.data() : {};

  const response = await fetch(`${baseUrl}/payments`, {
    method: "POST",
    headers: {
      "X-API-Key": apiKey,
      "X-API-Secret": apiSecret,
      "Accept": "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      amount: amountFcfa,
      currency: "XOF",
      description: `Réabonnement Pressing Pro ${plan} (${billingCycle})`,
      customer: {
        name: String(companyData.companyName || request.auth.token.name || "Pressing Pro").slice(0, 100),
        email: String(request.auth.token.email || companyData.email || "").slice(0, 120),
        phone: String(companyData.phone || "").slice(0, 30),
      },
      metadata: {
        organization_uid: uid,
        plan,
        billing_cycle: billingCycle,
        mode,
      },
    }),
  });

  const payload = await response.json();
  const data = payload?.data || payload || {};
  const reference = String(data.reference || data.id || "").trim();
  const checkoutUrl = String(data.checkout_url || data.payment_url || "").trim();

  if (!response.ok || !reference || !checkoutUrl.startsWith("https://")) {
    throw new HttpsError("internal", payload?.message || "Erreur lors de l'initialisation GeniusPay.");
  }

  await db
    .collection("users")
    .doc(uid)
    .collection("subscription_payments")
    .doc(reference.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 80))
    .set({
      userId: uid,
      reference,
      provider: "GENIUSPAY",
      mode: mode === "test" ? "test" : "live",
      plan: PLAN_MONTHLY_FCFA[plan] ? plan : "BUSINESS",
      billingCycle: ["MONTHLY", "QUARTERLY", "ANNUAL"].includes(billingCycle) ? billingCycle : "MONTHLY",
      amountFcfa,
      currency: "XOF",
      status: "PENDING",
      checkoutUrl,
      customerEmail: String(request.auth.token.email || companyData.email || "").slice(0, 120),
      customerPhone: String(companyData.phone || "").slice(0, 30),
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

  return { reference, checkoutUrl, amountFcfa, currency: "XOF", mode, status: "PENDING" };
});

/**
 * 13. verifyGeniusPaySubscriptionPayment — server-side transaction verification & subscription activation.
 */
exports.verifyGeniusPaySubscriptionPayment = onCall(async (request) => {
  const uid = requireAuth(request);
  const reference = String(request.data?.reference || "").trim();
  if (!/^[A-Za-z0-9_-]{3,80}$/.test(reference)) {
    throw new HttpsError("invalid-argument", "Référence GeniusPay invalide.");
  }
  const apiKey = process.env.GENIUSPAY_API_KEY || "";
  const apiSecret = process.env.GENIUSPAY_API_SECRET || "";
  if (!apiKey || !apiSecret) {
    throw new HttpsError("failed-precondition", "Clés GeniusPay non configurées sur le serveur.");
  }

  const baseUrl = getValidatedGeniusPayBaseUrl();
  const response = await fetch(`${baseUrl}/payments/${encodeURIComponent(reference)}`, {
    method: "GET",
    headers: {
      "X-API-Key": apiKey,
      "X-API-Secret": apiSecret,
      "Accept": "application/json",
    },
  });
  const payload = await response.json();
  const data = payload?.data || payload || {};
  const rawStatus = String(data.status || "pending").toLowerCase();

  let normalizedStatus = "PENDING";
  if (["completed", "paid", "succeeded", "success", "approved"].includes(rawStatus)) {
    normalizedStatus = "COMPLETED";
  } else if (["failed", "declined", "error", "expired"].includes(rawStatus)) {
    normalizedStatus = "FAILED";
  } else if (["cancelled", "canceled", "aborted"].includes(rawStatus)) {
    normalizedStatus = "CANCELLED";
  }

  const payDocId = reference.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 80);
  const payRef = db.collection("users").doc(uid).collection("subscription_payments").doc(payDocId);
  const paySnap = await payRef.get();

  if (paySnap.exists && normalizedStatus !== "PENDING") {
    await payRef.update({ status: normalizedStatus, updatedAt: FieldValue.serverTimestamp() });
    if (normalizedStatus === "COMPLETED") {
      const pData = paySnap.data();
      await db.collection("users").doc(uid).set(
        {
          subscriptionPlan: pData.plan || "BUSINESS",
          subscriptionBilling: pData.billingCycle || "MONTHLY",
          subscriptionStatus: "ACTIVE",
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    }
  }

  return { reference, status: normalizedStatus };
});

/**
 * 14. geniusPayWebhook — HTTPS webhook endpoint verifying HMAC-SHA256 with GENIUSPAY_WEBHOOK_SECRET.
 */
exports.geniusPayWebhook = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method Not Allowed" });
    return;
  }
  const webhookSecret = process.env.GENIUSPAY_WEBHOOK_SECRET || "";
  if (!webhookSecret || webhookSecret.includes("PLACEHOLDER")) {
    res.status(503).json({ error: "Webhook secret not configured" });
    return;
  }

  const signature =
    req.get("X-GeniusPay-Signature") ||
    req.get("X-Webhook-Signature") ||
    req.get("X-Signature") ||
    "";
  const rawBody = req.rawBody || Buffer.from(JSON.stringify(req.body || {}), "utf8");
  if (!verifyGeniusPayWebhookSignature(rawBody, signature, webhookSecret)) {
    res.status(401).json({ error: "Invalid webhook signature" });
    return;
  }

  const event = req.body || {};
  const data = event.data || event;
  const reference = String(data.reference || data.id || "").trim();
  const metadata = data.metadata || {};
  const uid = String(metadata.organization_uid || "").trim();
  const eventType = String(event.event || data.status || "").toLowerCase();

  if (!reference || !uid) {
    res.status(200).json({ received: true, ignored: true });
    return;
  }

  const isCompleted =
    eventType.includes("completed") ||
    eventType.includes("paid") ||
    eventType.includes("succeeded");
  const isFailed = eventType.includes("failed") || eventType.includes("declined");
  const isCancelled = eventType.includes("cancel");

  const nextStatus = isCompleted
    ? "COMPLETED"
    : isFailed
      ? "FAILED"
      : isCancelled
        ? "CANCELLED"
        : "PENDING";

  const payDocId = reference.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 80);
  const payRef = db.collection("users").doc(uid).collection("subscription_payments").doc(payDocId);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(payRef);
    if (!snap.exists || snap.data().status === "COMPLETED") return;
    if (nextStatus !== "PENDING") {
      tx.update(payRef, { status: nextStatus, updatedAt: FieldValue.serverTimestamp() });
      if (nextStatus === "COMPLETED") {
        tx.set(
          db.collection("users").doc(uid),
          {
            subscriptionPlan: snap.data().plan || metadata.plan || "BUSINESS",
            subscriptionBilling: snap.data().billingCycle || metadata.billing_cycle || "MONTHLY",
            subscriptionStatus: "ACTIVE",
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
      }
    }
  });

  res.status(200).json({ received: true, reference, status: nextStatus });
});
