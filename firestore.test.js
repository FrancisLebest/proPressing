const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_tenant_01";
const BOB_UID = "bob_tenant_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = (offsetMs = 5000) => new Date(Date.now() - offsetMs);
const futureDate = () => new Date(Date.now() + 60000);

const validCompanyPayload = (uid, ts = pastDate()) => ({
  userId: uid,
  companyName: "Pressing Nouvelle Etoile",
  legalName: "Diana Services SARL",
  rccm: "CI-ABJ-2026-B-1234",
  nif: "1234567890",
  description: "Pressing et blanchisserie moderne",
  phone: "+225 07 58 42 19 63",
  whatsapp: "+225 07 58 42 19 63",
  email: "contact@pressingetoile.ci",
  country: "Cote d'Ivoire",
  city: "Abidjan",
  commune: "Cocody",
  address: "Cocody Angre 8e tranche",
  latitude: 5.394,
  longitude: -3.987,
  currency: "FCFA",
  taxRatePercent: 0.0,
  openingHours: "Lun-Sam: 07h30 - 19h30",
  standardDelayHours: 48,
  expressDelayHours: 24,
  expressSurchargePercent: 50.0,
  orderPrefix: "PRESS-2026",
  receiptFooter: "Vetements conserves 30 jours maximum.",
  receiptFormat: "THERMAL_80",
  acceptedPayments: ["ESPECES", "ORANGE_MONEY", "WAVE", "MTN_MOMO"],
  agencies: ["Agence Principale - Cocody"],
  smsNotificationsEnabled: true,
  whatsappNotificationsEnabled: true,
  subscriptionPlan: "BUSINESS",
  subscriptionBilling: "MONTHLY",
  subscriptionStatus: "ACTIVE",
  setupStep: 10,
  setupCompleted: true,
  createdAt: ts,
  updatedAt: ts,
});

const validOrderPayload = (uid, ts = pastDate()) => ({
  userId: uid,
  orderNumber: "PRESS-2026-0001",
  agencyName: "Agence Principale - Cocody",
  clientId: "client_1",
  clientName: "Kouassi Yao",
  clientPhone: "+225 07 58 42 19 63",
  clientAddress: "Cocody Riviera 3",
  status: "PENDING",
  priority: "STANDARD",
  items: [
    {
      garmentName: "Chemise blanche",
      serviceName: "Lavage + Repassage",
      fabric: "Coton",
      color: "Blanc",
      soilLevel: "Moyen",
      defects: "Tache au col",
      notes: "",
      quantity: 2,
      unitPrice: 1000,
      lineTotal: 2000,
      barcode: "PR-001-1",
      photoBase64: "",
      aiDiagnosis: "",
    },
  ],
  itemCount: 2,
  subtotal: 2000,
  discountAmount: 0,
  expressFee: 0,
  deliveryFee: 0,
  taxAmount: 0,
  totalAmount: 2000,
  paidAmount: 1000,
  remainingAmount: 1000,
  paymentStatus: "PARTIAL",
  paymentMethod: "ORANGE_MONEY",
  deliveryRequested: false,
  deliveryDriver: "",
  deliveryZone: "",
  assignedEmployee: "Aminata",
  qualityChecked: false,
  clientSignatureName: "Kouassi Yao",
  observations: "Attention au col",
  dueDateMillis: Date.now() + 172800000,
  statusHistory: ["2026-11-19 10:00 - Commande enregistree"],
  createdAt: ts,
  updatedAt: ts,
});

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any company or subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validCompanyPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("orders").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});

test("CompanyConfig: owner create, update, shadow-field rejection, and cross-tenant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validCompanyPayload(ALICE_UID, created))
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      companyName: "Pressing Etoile d'Or",
      setupStep: 10,
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost/shadow field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuperAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      companyName: "Tampered",
      updatedAt: futureDate(),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Cross-tenant isolation: Bob cannot read or modify Alice's company
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      companyName: "Hijacked",
      updatedAt: pastDate(500),
    })
  );
});

test("Services, Clients, Stocks, Employees, Claims subcollections: CRUD & isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  // Service create + list + update + reject extra @DocumentId 'id'
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("services").doc("srv_1").set({
      userId: ALICE_UID,
      name: "Chemise",
      category: "Vetements",
      treatmentType: "Lavage + Repassage",
      billingUnit: "PIECE",
      unitPrice: 1000,
      expressPrice: 1500,
      delayHours: 48,
      available: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("services").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("services").doc("srv_bad").set({
      id: "srv_bad",
      userId: ALICE_UID,
      name: "Chemise",
      category: "Vetements",
      treatmentType: "Lavage",
      billingUnit: "PIECE",
      unitPrice: 1000,
      expressPrice: 1500,
      delayHours: 48,
      available: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("services").get()
  );

  // Client create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("clients").doc("cli_1").set({
      userId: ALICE_UID,
      fullName: "Amani Diallo",
      phone: "+225 07 12 34 56",
      whatsapp: "+225 07 12 34 56",
      email: "amani@example.ci",
      category: "VIP",
      address: "Cocody",
      latitude: 5.35,
      longitude: -4.0,
      loyaltyPoints: 120,
      discountPercent: 10.0,
      creditLimit: 50000,
      balanceDue: 0,
      preferences: "Sans amidon",
      notes: "",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("clients").doc("cli_1").update({
      loyaltyPoints: 150,
      updatedAt: pastDate(1000),
    })
  );

  // Stock & Employee & Claim create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stocks").doc("stk_1").set({
      userId: ALICE_UID,
      name: "Lessive Pro 25kg",
      itemKind: "CONSUMABLE",
      category: "Detergents",
      quantity: 12,
      minThreshold: 5,
      unit: "kg",
      unitCost: 15000,
      supplierName: "Ivoire Chimie",
      machineStatus: "OPERATIONAL",
      maintenanceNotes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_1").set({
      userId: ALICE_UID,
      fullName: "Traore Amadou",
      phone: "+225 05 00 11 22",
      role: "LIVREUR",
      agencyName: "Agence Principale",
      monthlySalary: 120000,
      commissionRate: 5.0,
      presentToday: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("claims").doc("clm_1").set({
      userId: ALICE_UID,
      orderNumber: "PRESS-2026-0001",
      clientName: "Amani Diallo",
      subject: "Bouton manquant",
      description: "Un bouton de manche a ete perdu.",
      severity: "MEDIUM",
      status: "OPEN",
      resolutionNote: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Resolve claim, then verify terminal lock prevents further edits
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("claims").doc("clm_1").update({
      status: "RESOLVED",
      resolutionNote: "Bouton remplace gratuitement",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("claims").doc("clm_1").update({
      status: "OPEN",
      updatedAt: pastDate(500),
    })
  );
});

test("Orders workflow: action-scoped updates, payment settlement, terminal lock, and immutable transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("orders")
      .doc("ord_1")
      .set(validOrderPayload(ALICE_UID, ts))
  );

  // Action 1: status progression
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").update({
      status: "WASHING",
      qualityChecked: false,
      statusHistory: ["2026-11-19 10:00 - Enregistree", "2026-11-19 11:00 - En lavage"],
      updatedAt: pastDate(2000),
    })
  );

  // Action 2: payment settlement
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").update({
      paidAmount: 2000,
      remainingAmount: 0,
      paymentStatus: "PAID",
      paymentMethod: "WAVE",
      statusHistory: ["2026-11-19 10:00 - Enregistree", "2026-11-19 11:00 - En lavage", "Solde Wave"],
      updatedAt: pastDate(1500),
    })
  );

  // Reject silent delete of orders
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").delete()
  );

  // Cancel order, then verify terminal lock blocks subsequent edits
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").update({
      status: "CANCELLED",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").update({
      status: "READY",
      updatedAt: pastDate(500),
    })
  );

  // Accounting entry: create succeeds, update & delete forbidden (SYSCOHADA audit trail)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      reference: "ECR-2026-0001",
      entryType: "REVENUE",
      category: "Prestation Pressing",
      syscohadaAccount: "706",
      label: "Acompte PRESS-2026-0001",
      amount: 1000,
      paymentMethod: "ORANGE_MONEY",
      orderId: "ord_1",
      counterparty: "Kouassi Yao",
      agencyName: "Agence Principale - Cocody",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      amount: 500,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").delete()
  );
});
