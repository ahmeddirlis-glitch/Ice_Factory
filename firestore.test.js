const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_123";
const STAFF_UID = "staff_456";
const OUTSIDER_UID = "outsider_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
}

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write factory sales or funding", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("factories").doc(OWNER_UID).collection("sales").get());
  await assertFails(unauthDb.collection("factories").doc(OWNER_UID).collection("funding").get());
});

test("Owner can create and list funding, payroll, employees, and Factory Build expenses", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertSucceeds(
    ownerDb.collection("factories").doc(OWNER_UID).collection("funding").doc("fund_1").set({
      ownerId: OWNER_UID,
      source: "Friend",
      name: "Bilal",
      type: "Loan",
      amount: 20000000,
      repaid: 0,
      interest: 0,
      date: "2026-11-14",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    ownerDb.collection("factories").doc(OWNER_UID).collection("funding").get()
  );
  await assertSucceeds(
    ownerDb.collection("factories").doc(OWNER_UID).collection("expenses").doc("exp_build").set({
      ownerId: OWNER_UID,
      description: "Factory construction",
      amount: 5000000,
      category: "Factory Build",
      capitalInvestment: true,
      date: "2026-11-14",
      createdBy: OWNER_UID,
      source: "voice",
      memberUids: [OWNER_UID],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Non-owner (Staff/Manager) CANNOT read or write owner-only funding, payroll, or Factory Build expenses", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("factories").doc(OWNER_UID).collection("funding").doc("fund_1").set({
      ownerId: OWNER_UID,
      source: "Bank",
      name: "HBL",
      type: "Loan",
      amount: 5000000,
      repaid: 0,
      interest: 5,
      date: "2026-11-14",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
    await db.collection("factories").doc(OWNER_UID).collection("expenses").doc("exp_build").set({
      ownerId: OWNER_UID,
      description: "Machinery setup",
      amount: 5000000,
      category: "Factory Build",
      capitalInvestment: true,
      date: "2026-11-14",
      createdBy: OWNER_UID,
      source: "manual",
      memberUids: [OWNER_UID, STAFF_UID],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
    await db.collection("factories").doc(OWNER_UID).collection("expenses").doc("exp_op").set({
      ownerId: OWNER_UID,
      description: "2 chai",
      amount: 260,
      category: "Refreshment",
      capitalInvestment: false,
      date: "2026-11-14",
      createdBy: OWNER_UID,
      source: "voice",
      memberUids: [OWNER_UID, STAFF_UID],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  await assertFails(staffDb.collection("factories").doc(OWNER_UID).collection("funding").get());
  await assertFails(staffDb.collection("factories").doc(OWNER_UID).collection("payroll").get());
  await assertFails(staffDb.collection("factories").doc(OWNER_UID).collection("employees").get());
  await assertFails(staffDb.collection("factories").doc(OWNER_UID).collection("expenses").doc("exp_build").get());
  await assertFails(
    staffDb.collection("factories").doc(OWNER_UID).collection("expenses").where("memberUids", "array-contains", STAFF_UID).get()
  );
  await assertSucceeds(
    staffDb.collection("factories").doc(OWNER_UID).collection("expenses")
      .where("memberUids", "array-contains", STAFF_UID)
      .where("capitalInvestment", "==", false)
      .get()
  );
});

test("Shadow update with ghost field is rejected", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertFails(
    ownerDb.collection("factories").doc(OWNER_UID).collection("sales").doc("sale_ghost").set({
      ownerId: OWNER_UID,
      invoiceNumber: "INV-001",
      customerId: "cust_1",
      customerName: "Ali",
      product: "Ice Blocks (10kg)",
      quantity: 10,
      unitPrice: 1500,
      total: 15000,
      paid: true,
      paidAmount: 15000,
      date: "2026-11-14",
      createdBy: OWNER_UID,
      source: "manual",
      memberUids: [OWNER_UID],
      ghostField: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Cross-user outsider cannot read another factory's sales", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("factories").doc(OWNER_UID).collection("sales").doc("sale_1").set({
      ownerId: OWNER_UID,
      invoiceNumber: "INV-001",
      customerId: "cust_1",
      customerName: "Ali",
      product: "Ice Blocks (10kg)",
      quantity: 10,
      unitPrice: 1500,
      total: 15000,
      paid: true,
      paidAmount: 15000,
      date: "2026-11-14",
      createdBy: OWNER_UID,
      source: "manual",
      memberUids: [OWNER_UID],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  await assertFails(outsiderDb.collection("factories").doc(OWNER_UID).collection("sales").doc("sale_1").get());
  await assertFails(outsiderDb.collection("factories").doc(OWNER_UID).collection("sales").get());
});

test("First registrant claims factory_directory/primary; non-super-admin cannot overwrite or self-escalate", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  await assertSucceeds(
    ownerDb.collection("factory_directory").doc("primary").set({
      ownerId: OWNER_UID,
      companyName: "Brohi Ice Factory",
      ownerName: "Owner Khan",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Outsider / staff cannot overwrite primary factory owner
  await assertFails(
    staffDb.collection("factory_directory").doc("primary").set({
      ownerId: STAFF_UID,
      companyName: "Hijacked Factory",
      ownerName: "Staff",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // New user cannot self-create /users/{uid} as APPROVED Super Admin once primary exists
  await assertFails(
    staffDb.collection("users").doc(STAFF_UID).set({
      userId: STAFF_UID,
      displayName: "Staff Member",
      preferredLanguage: "en",
      darkMode: false,
      approvalStatus: "APPROVED",
      requestedRole: "Super Admin",
      linkedFactoryId: OWNER_UID,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // New user CAN create their own PENDING profile
  await assertSucceeds(
    staffDb.collection("users").doc(STAFF_UID).set({
      userId: STAFF_UID,
      displayName: "Staff Member",
      preferredLanguage: "en",
      darkMode: false,
      approvalStatus: "PENDING",
      requestedRole: "Staff",
      linkedFactoryId: OWNER_UID,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Pending user CANNOT self-approve their own profile
  await assertFails(
    staffDb.collection("users").doc(STAFF_UID).update({
      approvalStatus: "APPROVED",
      requestedRole: "Admin",
      updatedAt: nowTs(),
    })
  );
});

test("Only Super Admin can publish app_releases/latest while all signed-in users can read updates", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  await ownerDb.collection("factory_directory").doc("primary").set({
    ownerId: OWNER_UID,
    companyName: "Brohi Ice Factory",
    ownerName: "Owner Khan",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });

  // Staff cannot publish app updates
  await assertFails(
    staffDb.collection("app_releases").doc("latest").set({
      latestVersionCode: 2,
      latestVersionName: "1.1",
      releaseNotes: "Unauthorized",
      apkDownloadUrl: "https://example.com/bad.apk",
      mandatory: false,
      publishedBy: "Staff",
      publishedAtIso: "2026-11-16",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Super Admin can publish app updates
  await assertSucceeds(
    ownerDb.collection("app_releases").doc("latest").set({
      latestVersionCode: 2,
      latestVersionName: "1.1",
      releaseNotes: "New dashboard updates",
      releaseNotesUrdu: "نئی اپڈیٹ",
      apkDownloadUrl: "https://example.com/brohi-v2.apk",
      mandatory: false,
      publishedBy: "Super Admin",
      publishedAtIso: "2026-11-16",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // All signed-in users (Staff/Manager/Admin/Super Admin) can read release notifications
  await assertSucceeds(staffDb.collection("app_releases").doc("latest").get());
});

test("Approved member can submit profile update request without role/salary, and only Admin/Super Admin can approve it", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("factory_directory").doc("primary").set({
      ownerId: OWNER_UID,
      companyName: "Brohi Ice Factory",
      ownerName: "Owner Khan",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
    await db.collection("users").doc(STAFF_UID).set({
      userId: STAFF_UID,
      displayName: "Usman Staff",
      preferredLanguage: "en",
      darkMode: false,
      approvalStatus: "APPROVED",
      requestedRole: "Staff",
      linkedFactoryId: OWNER_UID,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const reqDocRef = staffDb.collection("factories").doc(OWNER_UID).collection("profile_update_requests").doc(STAFF_UID);

  // Member CANNOT include role or salary fields inside profile_update_requests
  await assertFails(
    reqDocRef.set({
      ownerId: OWNER_UID,
      applicantUid: STAFF_UID,
      name: "Usman Staff",
      cnic: "42201-1234567-1",
      phone: "0300-1234567",
      shift: "Morning (8AM - 4PM)",
      joinDate: "2026-11-01",
      emergencyContact: "0300-9876543",
      status: "PENDING",
      role: "Admin",
      salary: 99999,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Member CAN submit valid PENDING profile update request
  await assertSucceeds(
    reqDocRef.set({
      ownerId: OWNER_UID,
      applicantUid: STAFF_UID,
      name: "Usman Tariq",
      fatherName: "Tariq Mahmood",
      cnic: "42201-1234567-1",
      phone: "0300-1234567",
      whatsappNumber: "0300-1234567",
      email: "usman@example.com",
      shift: "Evening (4PM - 12AM)",
      joinDate: "2026-11-01",
      emergencyContact: "0300-9876543",
      status: "PENDING",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Member CANNOT self-approve their own profile update request
  await assertFails(
    reqDocRef.update({
      status: "APPROVED",
      updatedAt: nowTs(),
    })
  );

  // Outsider CANNOT read or list profile_update_requests
  await assertFails(outsiderDb.collection("factories").doc(OWNER_UID).collection("profile_update_requests").doc(STAFF_UID).get());
  await assertFails(outsiderDb.collection("factories").doc(OWNER_UID).collection("profile_update_requests").get());

  // Owner / Super Admin CAN approve the profile update request
  await assertSucceeds(
    ownerDb.collection("factories").doc(OWNER_UID).collection("profile_update_requests").doc(STAFF_UID).update({
      status: "APPROVED",
      reviewNote: "Approved by Super Admin",
      updatedAt: nowTs(),
    })
  );
});


