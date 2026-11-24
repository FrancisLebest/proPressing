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
  trackingToken: "TRK-AB12-CD34-EF56",
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

test("RBAC rules: non-financial roles blocked from accounting, salaries, financial order edits, and tariffs", async () => {
  const directorDb = testEnv.authenticatedContext(ALICE_UID, { role: "DIRECTEUR" }).firestore();
  const managerDb = testEnv.authenticatedContext(ALICE_UID, { role: "GERANT_AGENCE" }).firestore();
  const cashierDb = testEnv.authenticatedContext(ALICE_UID, { role: "CAISSIER" }).firestore();
  const operatorDb = testEnv.authenticatedContext(ALICE_UID, { role: "OPERATEUR" }).firestore();
  const qcDb = testEnv.authenticatedContext(ALICE_UID, { role: "CONTROLE_QUALITE" }).firestore();
  const driverDb = testEnv.authenticatedContext(ALICE_UID, { role: "LIVREUR" }).firestore();
  const ts = pastDate(10000);

  // Seed company, service, employee, order, and transaction as Directeur
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).set(validCompanyPayload(ALICE_UID, ts))
  );
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("services").doc("srv_1").set({
      userId: ALICE_UID,
      name: "Costume 2 pieces",
      category: "Costumes & Vestes",
      treatmentType: "Nettoyage a sec",
      billingUnit: "PIECE",
      unitPrice: 3000,
      expressPrice: 4500,
      delayHours: 48,
      available: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_1").set({
      userId: ALICE_UID,
      fullName: "Kone Moussa",
      phone: "+225 07 11 22 33",
      role: "OPERATEUR",
      agencyName: "Agence Principale - Cocody",
      monthlySalary: 150000,
      commissionRate: 0.0,
      presentToday: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").set(
      validOrderPayload(ALICE_UID, ts)
    )
  );
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      reference: "ECR-Z-01",
      entryType: "REVENUE",
      category: "Prestation Pressing",
      syscohadaAccount: "706",
      label: "Encaissement PRESS-2026-0001",
      amount: 1000,
      paymentMethod: "WAVE",
      orderId: "ord_1",
      counterparty: "Kouassi Yao",
      agencyName: "Agence Principale - Cocody",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-financial roles CANNOT read transactions or employees (salaries/accounting)
  for (const restrictedDb of [operatorDb, qcDb, driverDb]) {
    await assertFails(restrictedDb.collection("users").doc(ALICE_UID).collection("transactions").get());
    await assertFails(restrictedDb.collection("users").doc(ALICE_UID).collection("employees").get());
    // Cannot modify financial fields of an order (Action 2)
    await assertFails(
      restrictedDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").update({
        paidAmount: 2000,
        remainingAmount: 0,
        paymentStatus: "PAID",
        paymentMethod: "ESPECES",
        statusHistory: ["Tentative"],
        updatedAt: pastDate(1000),
      })
    );
    // Cannot modify tariffs
    await assertFails(
      restrictedDb.collection("users").doc(ALICE_UID).collection("services").doc("srv_1").update({
        unitPrice: 500,
        updatedAt: pastDate(1000),
      })
    );
    // Cannot modify agency settings
    await assertFails(
      restrictedDb.collection("users").doc(ALICE_UID).update({
        maxCashierDiscountPercent: 50.0,
        updatedAt: pastDate(1000),
      })
    );
  }

  // Operator CAN still advance production status without touching financial fields
  await assertSucceeds(
    operatorDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").update({
      status: "WASHING",
      statusHistory: ["2026-11-19 10:00 - Enregistree", "En lavage"],
      updatedAt: pastDate(800),
    })
  );

  // Cashier CANNOT modify tariffs or discount caps, but CAN configure local Bluetooth printer & cash close date
  await assertFails(
    cashierDb.collection("users").doc(ALICE_UID).collection("services").doc("srv_1").update({
      unitPrice: 100,
      updatedAt: pastDate(700),
    })
  );
  await assertFails(
    cashierDb.collection("users").doc(ALICE_UID).update({
      maxCashierDiscountPercent: 80.0,
      updatedAt: pastDate(700),
    })
  );
  await assertSucceeds(
    cashierDb.collection("users").doc(ALICE_UID).update({
      receiptFormat: "THERMAL_58",
      autoCutPaper: true,
      defaultPrinterName: "MPT-II 58mm",
      defaultPrinterAddress: "00:11:22:33:44:55",
      lastCashCloseDate: "2026-11-21",
      updatedAt: pastDate(600),
    })
  );

  // Gerant d'Agence CAN modify agency settings, discount caps, and tariffs
  await assertSucceeds(
    managerDb.collection("users").doc(ALICE_UID).update({
      maxCashierDiscountPercent: 12.0,
      ultraExpressDelayHours: 6,
      ultraExpressSurchargePercent: 100.0,
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    managerDb.collection("users").doc(ALICE_UID).collection("services").doc("srv_1").update({
      unitPrice: 3500,
      updatedAt: pastDate(400),
    })
  );
});

test("Security audit_logs: any tenant role can log denied access, only Directeur can read, immutable", async () => {
  const directorDb = testEnv.authenticatedContext(ALICE_UID, { role: "DIRECTEUR" }).firestore();
  const managerDb = testEnv.authenticatedContext(ALICE_UID, { role: "GERANT_AGENCE" }).firestore();
  const operatorDb = testEnv.authenticatedContext(ALICE_UID, { role: "OPERATEUR" }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { role: "DIRECTEUR" }).firestore();
  const ts = pastDate(5000);

  // Operator access-denial attempt is logged into audit_logs
  await assertSucceeds(
    operatorDb.collection("users").doc(ALICE_UID).collection("audit_logs").doc("log_1").set({
      userId: ALICE_UID,
      actor: "Kone Moussa",
      roleLabel: "Atelier",
      scopeLabel: "Agence Cocody",
      action: "Acces bloque vers finance (Consulter CA multi-agences & SYSCOHADA)",
      route: "finance",
      allowed: false,
      createdAt: ts,
    })
  );

  // Directeur can read audit_logs; Manager, Operator, and cross-tenant Bob CANNOT
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("audit_logs").get()
  );
  await assertFails(
    managerDb.collection("users").doc(ALICE_UID).collection("audit_logs").get()
  );
  await assertFails(
    operatorDb.collection("users").doc(ALICE_UID).collection("audit_logs").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("audit_logs").get()
  );

  // Audit logs are strictly immutable (cannot be updated or deleted even by Directeur)
  await assertFails(
    directorDb.collection("users").doc(ALICE_UID).collection("audit_logs").doc("log_1").update({
      allowed: true,
    })
  );
  await assertFails(
    directorDb.collection("users").doc(ALICE_UID).collection("audit_logs").doc("log_1").delete()
  );
});

test("Cross-agency isolation, Cashier salary protection, FCM tokens, and notifications", async () => {
  const directorDb = testEnv.authenticatedContext(ALICE_UID, { role: "DIRECTEUR" }).firestore();
  const cocodyCashierDb = testEnv
    .authenticatedContext(ALICE_UID, { role: "CAISSIER", agency: "Agence Principale - Cocody" })
    .firestore();
  const yopougonManagerDb = testEnv
    .authenticatedContext(ALICE_UID, { role: "GERANT_AGENCE", agency: "Agence Yopougon" })
    .firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { role: "DIRECTEUR" }).firestore();
  const ts = pastDate(5000);

  // Seed Cocody order & employee
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_cocody").set(
      validOrderPayload(ALICE_UID, ts)
    )
  );
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_cocody").set({
      userId: ALICE_UID,
      fullName: "Kouame Jean",
      phone: "+225 07 00 00 11",
      role: "CAISSIER",
      agencyName: "Agence Principale - Cocody",
      monthlySalary: 180000,
      commissionRate: 0.0,
      presentToday: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-agency: Yopougon manager CANNOT read or update Cocody order
  await assertFails(
    yopougonManagerDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_cocody").get()
  );
  await assertFails(
    yopougonManagerDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_cocody").update({
      status: "WASHING",
      updatedAt: pastDate(1000),
    })
  );

  // Same-agency cashier CAN read Cocody order, CANNOT read employees salaries
  await assertSucceeds(
    cocodyCashierDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_cocody").get()
  );
  await assertFails(
    cocodyCashierDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_cocody").get()
  );

  // FCM device token registration & cross-tenant protection
  await assertSucceeds(
    cocodyCashierDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("device_12345").set({
      userId: ALICE_UID,
      token: "fcm_token_example_android_1234567890",
      role: "CAISSIER",
      agencyName: "Agence Principale - Cocody",
      topics: ["pressing_all", "pressing_cashiers", "agency_agence_principale_cocody"],
      platform: "ANDROID",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("fcm_tokens").doc("device_12345").get()
  );

  // Push notification outbox: cashier creates NEW_ORDER_CASHIER for Cocody; Yopougon cannot read Cocody notification
  await assertSucceeds(
    cocodyCashierDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      eventType: "NEW_ORDER_CASHIER",
      targetRole: "CAISSIER",
      agencyName: "Agence Principale - Cocody",
      orderNumber: "PRESS-2026-0001",
      clientPhone: "+225 07 58 42 19 63",
      title: "Nouvelle commande PRESS-2026-0001",
      body: "Kouassi Yao - 2 pieces enregistrees",
      delivered: false,
      createdAt: ts,
    })
  );
  await assertFails(
    yopougonManagerDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").get()
  );
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").get()
  );
});

test("Anti-privilege escalation, Cash Close Z immutability, and trackingToken immutability", async () => {
  const directorDb = testEnv.authenticatedContext(ALICE_UID, { role: "DIRECTEUR" }).firestore();
  const cocodyManagerDb = testEnv
    .authenticatedContext(ALICE_UID, { role: "GERANT_AGENCE", agency: "Agence Principale - Cocody" })
    .firestore();
  const ts = pastDate(5000);

  // Directeur creates a GERANT_AGENCE and a DIRECTEUR employee
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_dir").set({
      userId: ALICE_UID,
      fullName: "Koffi Directeur",
      phone: "+225 07 99 99 99",
      role: "DIRECTEUR",
      agencyName: "Agence Principale - Cocody",
      monthlySalary: 600000,
      commissionRate: 0.0,
      presentToday: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_mgr").set({
      userId: ALICE_UID,
      fullName: "Awa Gerante",
      phone: "+225 07 88 88 88",
      role: "GERANT_AGENCE",
      agencyName: "Agence Principale - Cocody",
      monthlySalary: 320000,
      commissionRate: 2.5,
      presentToday: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Manager CAN create a CAISSIER in their own agency
  await assertSucceeds(
    cocodyManagerDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_cashier").set({
      userId: ALICE_UID,
      fullName: "Bamba Caissier",
      phone: "+225 05 11 22 33",
      role: "CAISSIER",
      agencyName: "Agence Principale - Cocody",
      monthlySalary: 160000,
      commissionRate: 0.0,
      presentToday: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Anti-escalation: Manager CANNOT create a DIRECTEUR or GERANT_AGENCE
  await assertFails(
    cocodyManagerDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_fake_dir").set({
      userId: ALICE_UID,
      fullName: "Fake Dir",
      phone: "+225 01 00 00 01",
      role: "DIRECTEUR",
      agencyName: "Agence Principale - Cocody",
      monthlySalary: 500000,
      commissionRate: 0.0,
      presentToday: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    cocodyManagerDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_fake_mgr").set({
      userId: ALICE_UID,
      fullName: "Peer Mgr",
      phone: "+225 01 00 00 02",
      role: "GERANT_AGENCE",
      agencyName: "Agence Principale - Cocody",
      monthlySalary: 300000,
      commissionRate: 0.0,
      presentToday: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Anti-escalation: Manager CANNOT promote CAISSIER to DIRECTEUR nor modify/delete DIRECTEUR
  await assertFails(
    cocodyManagerDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_cashier").update({
      role: "DIRECTEUR",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    cocodyManagerDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_dir").update({
      monthlySalary: 1000,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    cocodyManagerDb.collection("users").doc(ALICE_UID).collection("employees").doc("emp_dir").delete()
  );

  // Cash Close Z immutability: create succeeds, update/delete strictly forbidden even for DIRECTEUR
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("transactions").doc("z_close_1").set({
      userId: ALICE_UID,
      reference: "Z-20261121-001",
      entryType: "CASH_CLOSE",
      category: "Cloture Z Caisse",
      syscohadaAccount: "571",
      label: "Cloture Z 2026-11-21 (Agence Principale - Cocody) · Ecart 0",
      amount: 125000,
      paymentMethod: "ESPECES",
      orderId: "",
      counterparty: "Directeur",
      agencyName: "Agence Principale - Cocody",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    directorDb.collection("users").doc(ALICE_UID).collection("transactions").doc("z_close_1").update({
      amount: 999999,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    directorDb.collection("users").doc(ALICE_UID).collection("transactions").doc("z_close_1").delete()
  );

  // Order trackingToken is immutable after creation
  await assertSucceeds(
    directorDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_trk").set(
      validOrderPayload(ALICE_UID, ts)
    )
  );
  await assertFails(
    directorDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_trk").update({
      trackingToken: "TRK-HACK-HACK-HACK",
      updatedAt: pastDate(500),
    })
  );
});

test("admins collection: strictly restricted to authorized Platform Super-Admins with immutable delete protection", async () => {
  const ts = pastDate(2000);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const regularDb = testEnv.authenticatedContext(ALICE_UID, { email: "director@pressing.ci" }).firestore();
  const superAdminDb = testEnv.authenticatedContext("super_admin_uid", { email: "best2london@gmail.com" }).firestore();

  const validAdminDoc = {
    email: "best2london@gmail.com",
    displayName: "Super-Admin Gestion Pro Africa",
    role: "SUPER_ADMIN",
    active: true,
    provider: "CLIENT_DEBUG_BOOTSTRAP",
    bootstrapSource: "android_debug_bootstrap",
    createdByUid: "super_admin_uid",
    createdAt: ts,
    updatedAt: ts,
  };

  // Unauthenticated and regular users cannot read, list, or create in /admins
  await assertFails(unauthDb.collection("admins").doc("best2london_gmail_com").get());
  await assertFails(unauthDb.collection("admins").doc("best2london_gmail_com").set(validAdminDoc));
  await assertFails(regularDb.collection("admins").doc("best2london_gmail_com").get());
  await assertFails(regularDb.collection("admins").where("email", "==", "best2london@gmail.com").get());
  await assertFails(regularDb.collection("admins").doc("best2london_gmail_com").set({
    ...validAdminDoc,
    createdByUid: ALICE_UID,
  }));

  // Authorized Super-Admin can create, read, query, and update allowed fields
  await assertSucceeds(superAdminDb.collection("admins").doc("best2london_gmail_com").set(validAdminDoc));
  await assertSucceeds(superAdminDb.collection("admins").doc("best2london_gmail_com").get());
  await assertSucceeds(superAdminDb.collection("admins").where("email", "==", "best2london@gmail.com").limit(1).get());
  await assertSucceeds(superAdminDb.collection("admins").doc("best2london_gmail_com").update({
    displayName: "Super-Admin Principal",
    updatedAt: pastDate(500),
  }));

  // Cannot create unauthorized email or spoof createdByUid
  await assertFails(superAdminDb.collection("admins").doc("rogue_admin").set({
    ...validAdminDoc,
    email: "attacker@example.com",
  }));
  await assertFails(superAdminDb.collection("admins").doc("spoofed_uid").set({
    ...validAdminDoc,
    createdByUid: "other_uid",
  }));

  // Delete is strictly forbidden
  await assertFails(superAdminDb.collection("admins").doc("best2london_gmail_com").delete());
});



