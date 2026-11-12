const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
});

test("Alice can create valid product in her own store", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").set({
      userId: ALICE_UID,
      name: "Widget Pro",
      sku: "WID-001",
      costPrice: 15.0,
      sellingPrice: 25.0,
      currentStock: 50,
      minStockThreshold: 10,
      createdAt: new Date(),
    })
  );
});

test("Bob cannot read or write Alice's product", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("products").doc("prod1").set({
      userId: ALICE_UID,
      name: "Widget Pro",
      sku: "WID-001",
      costPrice: 15.0,
      sellingPrice: 25.0,
      currentStock: 50,
      minStockThreshold: 10,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").update({
      currentStock: 0,
    })
  );
});

test("Alice cannot create product with missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("invalid_prod").set({
      userId: ALICE_UID,
      name: "Incomplete Widget",
      // missing sku, costPrice, sellingPrice, etc.
      createdAt: new Date(),
    })
  );
});
