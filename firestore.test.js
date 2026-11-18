const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SUPER_ADMIN_UID = "super_admin_001";
const SUPER_ADMIN_EMAIL = "buildolive4@gmail.com";
const CO_ADMIN_UID = "co_admin_002";
const CO_ADMIN_EMAIL = "coadmin@justus.app";
const ALICE_UID = "alice_123";
const STAFF_UID = "staff_rahul_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Multi-admin email whitelist managed in-app by Super Admin grants co-admin access", async () => {
  const superDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const coAdminDb = testEnv
    .authenticatedContext(CO_ADMIN_UID, {
      email: CO_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  const past = new Date(Date.now() - 5000);

  // Regular user cannot add admin email
  await assertFails(
    aliceDb.collection("admin_emails").doc("alice@example.com").set({
      email: "alice@example.com",
      addedBy: "alice@example.com",
      createdAt: past,
    })
  );

  // Super Admin adds co-admin in-app
  await assertSucceeds(
    superDb.collection("admin_emails").doc(CO_ADMIN_EMAIL).set({
      email: CO_ADMIN_EMAIL,
      addedBy: SUPER_ADMIN_EMAIL,
      createdAt: past,
    })
  );

  // Co-admin can now publish a VIP 16:9 companion profile + private contact
  await assertSucceeds(
    coAdminDb.collection("companion_profiles").doc("vip_01").set({
      name: "Meera",
      age: 23,
      price: "₹999",
      role: "GF",
      category: "VIP",
      tier: "vip_16_9",
      bio: "Evening coffee & conversation",
      photoUrls: ["https://i.ibb.co/vip/meera.jpg"],
      coverUrl: "https://i.ibb.co/vip/meera.jpg",
      aspectRatio: 16 / 9,
      hiddenTimed: true,
      refreshEpochMs: 0,
      status: "published",
      ownerUid: "",
      staffVerified: true,
      createdByUid: CO_ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    coAdminDb
      .collection("companion_profiles")
      .doc("vip_01")
      .collection("private")
      .doc("contact")
      .set({
        phone: "+919876543210",
        address: "Bandra West, Mumbai",
        updatedAt: past,
      })
  );

  // Public & normal users can read companion profile, but NEVER private phone/address
  const publicDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(
    publicDb.collection("companion_profiles").where("status", "==", "published").get()
  );
  await assertFails(
    publicDb
      .collection("companion_profiles")
      .doc("vip_01")
      .collection("private")
      .doc("contact")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("companion_profiles")
      .doc("vip_01")
      .collection("private")
      .doc("contact")
      .get()
  );
});

test("User PII isolation, self-escalation prevention, and Admin CRM moderation", async () => {
  const superDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const otherDb = testEnv
    .authenticatedContext(STAFF_UID, {
      email: "rahul@example.com",
      email_verified: true,
    })
    .firestore();

  const past = new Date(Date.now() - 5000);

  // User cannot self-approve as staff or set leadTag
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      name: "Alice",
      email: "alice@example.com",
      photoUrl: "https://i.ibb.co/a/pic.jpg",
      phone: "9988776655",
      age: 22,
      address: "Delhi",
      completionPercent: 100,
      online: true,
      lastActiveEpochMs: Date.now(),
      hasInquiryNotification: false,
      leadTag: "hot",
      moderationStatus: "active",
      staffStatus: "approved",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid user profile creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      name: "Alice",
      email: "alice@example.com",
      photoUrl: "https://i.ibb.co/a/pic.jpg",
      phone: "9988776655",
      age: 22,
      address: "Delhi",
      completionPercent: 100,
      online: true,
      lastActiveEpochMs: Date.now(),
      hasInquiryNotification: false,
      leadTag: "none",
      moderationStatus: "active",
      staffStatus: "none",
      staffProfileId: "",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Another user cannot read Alice's PII or list /users
  await assertFails(otherDb.collection("users").doc(ALICE_UID).get());
  await assertFails(otherDb.collection("users").get());

  // Alice can submit staff application when 100% complete
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      staffStatus: "pending",
      staffRole: "GF",
      staffPrice: "₹499",
      updatedAt: past,
    })
  );

  // Alice cannot approve herself
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      staffStatus: "approved",
      updatedAt: past,
    })
  );

  // Admin can list users, tag Hot Lead, and approve staff
  await assertSucceeds(superDb.collection("users").get());
  await assertSucceeds(
    superDb.collection("users").doc(ALICE_UID).update({
      leadTag: "hot",
      staffStatus: "approved",
      staffProfileId: "staff_alice_01",
      updatedAt: past,
    })
  );
});

test("Chat threads allow client, approved staff owner, and admin, while blocking staff contact leaks", async () => {
  const superDb = testEnv
    .authenticatedContext(SUPER_ADMIN_UID, {
      email: SUPER_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const staffDb = testEnv
    .authenticatedContext(STAFF_UID, {
      email: "rahul@example.com",
      email_verified: true,
    })
    .firestore();

  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      name: "Alice",
      email: "alice@example.com",
      photoUrl: "",
      phone: "9988776655",
      age: 22,
      address: "Mumbai",
      completionPercent: 60,
      online: true,
      lastActiveEpochMs: Date.now(),
      hasInquiryNotification: false,
      leadTag: "none",
      moderationStatus: "active",
      staffStatus: "none",
      staffProfileId: "",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    staffDb.collection("users").doc(STAFF_UID).set({
      name: "Rahul",
      email: "rahul@example.com",
      photoUrl: "https://i.ibb.co/r/pic.jpg",
      phone: "9111222333",
      age: 25,
      address: "Pune",
      completionPercent: 100,
      online: true,
      lastActiveEpochMs: Date.now(),
      hasInquiryNotification: false,
      leadTag: "none",
      moderationStatus: "active",
      staffStatus: "pending",
      staffProfileId: "",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Admin approves staff
  await assertSucceeds(
    superDb.collection("users").doc(STAFF_UID).update({
      staffStatus: "approved",
      staffProfileId: "prof_rahul",
      updatedAt: past,
    })
  );

  const chatId = `${ALICE_UID}_prof_rahul`;
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      clientUid: ALICE_UID,
      clientName: "Alice",
      clientPhotoUrl: "",
      profileId: "prof_rahul",
      profileName: "Rahul",
      profilePhotoUrl: "https://i.ibb.co/r/pic.jpg",
      profilePrice: "₹499",
      staffOwnerUid: STAFF_UID,
      lastMessage: "Hi Rahul!",
      lastSenderRole: "client",
      unreadForAdmin: 1,
      unreadForStaff: 1,
      unreadForClient: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Client sends inquiry
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      senderUid: ALICE_UID,
      senderRole: "client",
      senderName: "Alice",
      text: "Hi Rahul, available tonight?",
      createdAt: past,
    })
  );

  // Approved staff sends normal message -> allowed
  await assertSucceeds(
    staffDb.collection("chats").doc(chatId).collection("messages").doc("msg_2").set({
      senderUid: STAFF_UID,
      senderRole: "staff",
      senderName: "Rahul",
      text: "Yes! Happy to chat here with you.",
      createdAt: past,
    })
  );

  // Staff tries to send phone number / Instagram / email -> BLOCKED by rules
  await assertFails(
    staffDb.collection("chats").doc(chatId).collection("messages").doc("msg_leak_1").set({
      senderUid: STAFF_UID,
      senderRole: "staff",
      senderName: "Rahul",
      text: "Call me on 9876543210",
      createdAt: past,
    })
  );
  await assertFails(
    staffDb.collection("chats").doc(chatId).collection("messages").doc("msg_leak_2").set({
      senderUid: STAFF_UID,
      senderRole: "staff",
      senderName: "Rahul",
      text: "DM @rahul_official on instagram",
      createdAt: past,
    })
  );

  // Admin can reply in the same chat thread
  await assertSucceeds(
    superDb.collection("chats").doc(chatId).collection("messages").doc("msg_admin").set({
      senderUid: SUPER_ADMIN_UID,
      senderRole: "admin",
      senderName: "JustUs Concierge",
      text: "Booking details confirmed here!",
      createdAt: past,
    })
  );

  // Admin blocks Alice -> Alice can no longer send messages
  await assertSucceeds(
    superDb.collection("users").doc(ALICE_UID).update({
      moderationStatus: "blocked",
      updatedAt: past,
    })
  );
  await assertFails(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_blocked").set({
      senderUid: ALICE_UID,
      senderRole: "client",
      senderName: "Alice",
      text: "Am I blocked?",
      createdAt: past,
    })
  );
});
