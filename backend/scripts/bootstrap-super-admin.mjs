/**
 * GESTION PRO AFRICA — Pressing Pro
 * Programmatic Firebase Auth (Email/Password) & Initial Super-Admin Firestore Bootstrap Script
 *
 * Security:
 * - Zero hardcoded passwords or service-account keys in source code.
 * - Uses Google Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS or gcloud ADC).
 * - Targets Firebase project `gen-lang-client-0761158344` and Firestore database
 *   `ai-studio-android-pressing-bfcbafdf-ef9f-414c-b2a1-dde862b44f8e`.
 *
 * Usage:
 *   npm install firebase-admin
 *   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/serviceAccountKey.json"
 *   export SUPER_ADMIN_EMAIL="best2london@gmail.com"
 *   export SUPER_ADMIN_PASSWORD="<your-chosen-password>"
 *   node backend/scripts/bootstrap-super-admin.mjs
 */

import admin from "firebase-admin";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || "gen-lang-client-0761158344";
const DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-pressing-bfcbafdf-ef9f-414c-b2a1-dde862b44f8e";
const SUPER_ADMIN_EMAIL = String(process.env.SUPER_ADMIN_EMAIL || "").trim().toLowerCase();
const SUPER_ADMIN_PASSWORD = String(process.env.SUPER_ADMIN_PASSWORD || "");

const ALLOWED_SUPER_ADMIN_EMAILS = new Set([
  "lebest2london@gmail.com",
  "best2london@gmail.com",
]);

if (!SUPER_ADMIN_EMAIL || !ALLOWED_SUPER_ADMIN_EMAILS.has(SUPER_ADMIN_EMAIL)) {
  console.error(
    "ERROR: Set SUPER_ADMIN_EMAIL to an authorized Super-Admin address (lebest2london@gmail.com or best2london@gmail.com)."
  );
  process.exit(1);
}

if (SUPER_ADMIN_PASSWORD.length < 8) {
  console.error(
    "ERROR: Set SUPER_ADMIN_PASSWORD (minimum 8 characters) in your environment before running this script."
  );
  process.exit(1);
}

const app = admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: PROJECT_ID,
});

const auth = admin.auth(app);
const db = getFirestore(app, DATABASE_ID);

async function enableEmailPasswordProvider() {
  const accessTokenObj = await app.options.credential.getAccessToken();
  const token = accessTokenObj?.access_token;
  if (!token) {
    throw new Error("Unable to acquire Google OAuth access token from Application Default Credentials.");
  }

  const url = `https://identitytoolkit.googleapis.com/admin/v2/projects/${encodeURIComponent(
    PROJECT_ID
  )}/config?updateMask=signIn.email.enabled,signIn.email.passwordRequired`;

  const response = await fetch(url, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      signIn: {
        email: {
          enabled: true,
          passwordRequired: true,
        },
      },
    }),
  });

  if (!response.ok) {
    const bodyText = await response.text();
    throw new Error(`Failed to enable Email/Password provider (HTTP ${response.status}): ${bodyText}`);
  }
  console.log("✓ Firebase Auth Email/Password provider enabled on project:", PROJECT_ID);
}

async function provisionSuperAdminUser() {
  let userRecord;
  try {
    userRecord = await auth.getUserByEmail(SUPER_ADMIN_EMAIL);
    userRecord = await auth.updateUser(userRecord.uid, {
      password: SUPER_ADMIN_PASSWORD,
      emailVerified: true,
      disabled: false,
      displayName: userRecord.displayName || "Super-Admin Gestion Pro Africa",
    });
    console.log("✓ Updated existing Firebase Auth Super-Admin user:", userRecord.uid);
  } catch (err) {
    if (err?.code !== "auth/user-not-found") throw err;
    userRecord = await auth.createUser({
      email: SUPER_ADMIN_EMAIL,
      password: SUPER_ADMIN_PASSWORD,
      displayName: "Super-Admin Gestion Pro Africa",
      emailVerified: true,
      disabled: false,
    });
    console.log("✓ Created new Firebase Auth Super-Admin user:", userRecord.uid);
  }

  await auth.setCustomUserClaims(userRecord.uid, {
    role: "DIRECTEUR",
    platformRole: "SUPER_ADMIN",
    superAdmin: true,
  });
  console.log("✓ Assigned SUPER_ADMIN custom claims to UID:", userRecord.uid);

  await db.collection("platformRoles").doc(userRecord.uid).set(
    {
      uid: userRecord.uid,
      email: SUPER_ADMIN_EMAIL,
      role: "SUPER_ADMIN",
      status: "ACTIVE",
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  const userRef = db.collection("users").doc(userRecord.uid);
  const existingUserDoc = await userRef.get();
  if (!existingUserDoc.exists) {
    await userRef.set({
      userId: userRecord.uid,
      companyName: "Gestion Pro Africa — Siège",
      legalName: "SAS",
      rccm: "",
      nif: "",
      description: "Compte Super-Admin Plateforme Pressing Pro",
      phone: "+225 07 00 00 00 00",
      whatsapp: "+225 07 00 00 00 00",
      email: SUPER_ADMIN_EMAIL,
      country: "Côte d'Ivoire",
      city: "Abidjan",
      commune: "Cocody",
      address: "Abidjan, Côte d'Ivoire",
      latitude: 5.359952,
      longitude: -4.008256,
      currency: "FCFA",
      taxRatePercent: 0.0,
      openingHours: "Lun-Sam : 07h30 - 19h30",
      standardDelayHours: 48,
      expressDelayHours: 24,
      expressSurchargePercent: 50.0,
      ultraExpressDelayHours: 6,
      ultraExpressSurchargePercent: 100.0,
      maxCashierDiscountPercent: 10.0,
      corporateDiscountPercent: 15.0,
      orderPrefix: "PRESS-2026",
      receiptFooter: "Merci pour votre confiance. Conservation maximale 30 jours.",
      receiptFormat: "THERMAL_80",
      autoCutPaper: true,
      autoPrintLabelsOnPayment: false,
      defaultPrinterName: "",
      defaultPrinterAddress: "",
      lastCashCloseDate: "",
      acceptedPayments: ["ESPECES", "ORANGE_MONEY", "MTN_MOMO", "WAVE", "MOOV_MONEY"],
      agencies: ["Agence Principale"],
      smsNotificationsEnabled: true,
      whatsappNotificationsEnabled: true,
      subscriptionPlan: "ENTERPRISE",
      subscriptionBilling: "ANNUAL",
      subscriptionStatus: "ACTIVE",
      setupStep: 10,
      setupCompleted: true,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    console.log("✓ Seeded initial Super-Admin Firestore company profile at users/" + userRecord.uid);
  } else {
    console.log("✓ Existing Super-Admin Firestore document preserved at users/" + userRecord.uid);
  }
}

await enableEmailPasswordProvider();
await provisionSuperAdminUser();
console.log("✓ Super-Admin provisioning completed successfully for:", SUPER_ADMIN_EMAIL);
