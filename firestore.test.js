const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_001";
const ADMIN_EMAIL = "buildolive4@gmail.com";
const ALICE_UID = "alice_123";

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

test("Public user panel can read published gallery_images without sign-up, but cannot read archived or write", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    adminDb.collection("gallery_images").doc("img_001").set({
      title: "Golden Hour Architecture",
      description: "Warm light across brutalist concrete",
      category: "Architecture",
      imageUrl: "https://i.ibb.co/sample/golden-hour.jpg",
      thumbUrl: "https://i.ibb.co/sample/golden-hour-thumb.jpg",
      aspectRatio: 1.33,
      status: "published",
      featured: true,
      uploadedBy: ADMIN_UID,
      uploaderName: "Pixora Admin",
      tags: ["architecture", "golden"],
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    adminDb.collection("gallery_images").doc("img_002").set({
      title: "Hidden Draft",
      category: "Minimal",
      imageUrl: "https://i.ibb.co/sample/draft.jpg",
      aspectRatio: 1.0,
      status: "archived",
      featured: false,
      uploadedBy: ADMIN_UID,
      uploaderName: "Pixora Admin",
      createdAt: past,
      updatedAt: past,
    })
  );

  const publicDb = testEnv.unauthenticatedContext().firestore();

  // Filtered query for status == 'published' succeeds without login
  await assertSucceeds(
    publicDb.collection("gallery_images").where("status", "==", "published").get()
  );
  await assertSucceeds(
    publicDb.collection("gallery_images").doc("img_001").get()
  );

  // Unfiltered list or archived doc read fails
  await assertFails(publicDb.collection("gallery_images").get());
  await assertFails(publicDb.collection("gallery_images").doc("img_002").get());

  // Writes without admin auth fail
  await assertFails(
    publicDb.collection("gallery_images").doc("img_003").set({
      title: "Unauthorized Upload",
      category: "Nature",
      imageUrl: "https://i.ibb.co/sample/unauth.jpg",
      status: "published",
      featured: false,
      uploadedBy: "anon",
      uploaderName: "Anon",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    publicDb.collection("gallery_images").doc("img_001").delete()
  );
});

test("Regular signed-in non-admin cannot write gallery_images or spoof admin email", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const spoofedDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  const past = new Date(Date.now() - 5000);
  const payload = {
    title: "Unauthorized",
    category: "Abstract",
    imageUrl: "https://i.ibb.co/sample/test.jpg",
    status: "published",
    featured: false,
    uploadedBy: ALICE_UID,
    uploaderName: "Alice",
    createdAt: past,
    updatedAt: past,
  };

  await assertFails(aliceDb.collection("gallery_images").doc("img_x").set(payload));
  await assertFails(
    spoofedDb
      .collection("gallery_images")
      .doc("img_y")
      .set({ ...payload, uploadedBy: "spoof_uid" })
  );
});
