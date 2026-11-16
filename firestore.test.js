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
const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function safePastDate() {
  return new Date(Date.now() - 2000);
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

test("Unauthenticated user cannot read or write scout profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      scoutName: "أمين",
      ageGroup: "scouts",
      dialect: "dz",
      totalPoints: 0,
      completedCount: 0,
      badgesCount: 0,
      createdAt: safePastDate(),
      updatedAt: safePastDate(),
    })
  );
});

test("Owner can create and update their own scout profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const created = safePastDate();
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      scoutName: "أمين الكشاف",
      ageGroup: "scouts",
      dialect: "dz",
      totalPoints: 0,
      completedCount: 0,
      badgesCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    profileRef.update({
      dialect: "ar",
      totalPoints: 100,
      completedCount: 1,
      updatedAt: safePastDate(),
    })
  );
});

test("Cross-user access to profile, progress, and chat is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = safePastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      scoutName: "أمين",
      ageGroup: "cubs",
      dialect: "dz",
      totalPoints: 0,
      completedCount: 0,
      badgesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("gameProgress").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("chatMessages").get()
  );
});

test("Shadow field and invalid score bounds are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = safePastDate();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      scoutName: "أمين",
      ageGroup: "cubs",
      dialect: "dz",
      totalPoints: 0,
      completedCount: 0,
      badgesCount: 0,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gameProgress")
      .doc("game_0")
      .set({
        userId: ALICE_UID,
        gameIndex: 0,
        axisIndex: 0,
        bestScore: 99,
        attempts: 1,
        passed: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Owner can write and list gameProgress and chatMessages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = safePastDate();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gameProgress")
      .doc("game_0")
      .set({
        userId: ALICE_UID,
        gameIndex: 0,
        axisIndex: 0,
        bestScore: 9,
        attempts: 1,
        passed: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("gameProgress")
      .get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chatMessages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        sender: "user",
        text: "كيفاش ندير كلمة سر قوية؟",
        dialect: "dz",
        isWarning: false,
        suggestedGameIndex: -1,
        createdAt: ts,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chatMessages")
      .get()
  );
});

test("Digital badges enforce axis completion (4/4), featuring rules, and owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = safePastDate();
  const badgeRef = aliceDb.collection("users").doc(ALICE_UID).collection("badges").doc("badge_0");

  // Cannot unlock badge with fewer than 4 passed games
  await assertFails(
    badgeRef.set({
      userId: ALICE_UID,
      axisIndex: 0,
      badgeCode: "CS-DZ-AX0-ALICE1",
      titleDz: "حصن السرية",
      titleAr: "حصن السرية",
      icon: "🔐",
      passedGamesCount: 2,
      totalGamesInAxis: 4,
      axisPoints: 180,
      unlocked: true,
      isFeatured: false,
      earnedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot feature a locked badge
  await assertFails(
    badgeRef.set({
      userId: ALICE_UID,
      axisIndex: 0,
      badgeCode: "CS-DZ-AX0-ALICE1",
      titleDz: "حصن السرية",
      titleAr: "حصن السرية",
      icon: "🔐",
      passedGamesCount: 2,
      totalGamesInAxis: 4,
      axisPoints: 180,
      unlocked: false,
      isFeatured: true,
      earnedAt: null,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner can create in-progress badge
  await assertSucceeds(
    badgeRef.set({
      userId: ALICE_UID,
      axisIndex: 0,
      badgeCode: "CS-DZ-AX0-ALICE1",
      titleDz: "حصن السرية",
      titleAr: "حصن السرية",
      icon: "🔐",
      passedGamesCount: 2,
      totalGamesInAxis: 4,
      axisPoints: 180,
      unlocked: false,
      isFeatured: false,
      earnedAt: null,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner can unlock badge at 4/4 and then pin it as featured
  const earnedTime = safePastDate();
  await assertSucceeds(
    badgeRef.update({
      passedGamesCount: 4,
      axisPoints: 360,
      unlocked: true,
      earnedAt: earnedTime,
      updatedAt: earnedTime,
    })
  );
  await assertSucceeds(
    badgeRef.update({
      isFeatured: true,
      updatedAt: safePastDate(),
    })
  );

  // Cross-user read/write is denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("badges").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("badges").doc("badge_0").update({
      isFeatured: false,
      updatedAt: safePastDate(),
    })
  );
});

