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
const OWNER_UID = "owner_eyad_001";
const OWNER_EMAIL = "eyadhossam487@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 1000) {
  return new Date(Date.now() - msOffset);
}

function validUserPayload(uid, overrides = {}) {
  const now = pastDate(2000);
  return {
    userId: uid,
    displayName: "NeonViper",
    selectedSkinId: "cyber_viper",
    selectedPattern: "SOLID",
    preferredDifficulty: "MEDIUM",
    maxLength: 25,
    totalKills: 0,
    gamesPlayed: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validLeaderboardPayload(uid, overrides = {}) {
  const now = pastDate(2000);
  return {
    userId: uid,
    displayName: "NeonViper",
    skinId: "cyber_viper",
    pattern: "SOLID",
    maxLength: 140,
    totalKills: 3,
    visibility: "PUBLIC",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validRoomPayload(hostId, overrides = {}) {
  const now = pastDate(2000);
  return {
    hostId,
    name: "Cyber Grid 01",
    roomCode: "VIPER1",
    status: "WAITING",
    visibility: "PUBLIC",
    botDifficulty: "MEDIUM",
    botCount: 6,
    maxPlayers: 8,
    playerCount: 1,
    memberUids: [hostId],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validSnakePayload(ownerId, overrides = {}) {
  const now = pastDate(2000);
  return {
    ownerId,
    roomVisibility: "PUBLIC",
    displayName: "NeonViper",
    skinId: "cyber_viper",
    pattern: "SOLID",
    botControlled: false,
    alive: true,
    boosting: false,
    godMode: false,
    x: 1200.0,
    y: 1200.0,
    angle: 0.5,
    score: 25,
    kills: 0,
    points: [1200.0, 1200.0, 1188.0, 1196.0],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
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

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboard").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("rooms").where("visibility", "==", "PUBLIC").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("app_config").doc("owner").set({
      ownerUid: ALICE_UID,
      createdAt: pastDate(),
    })
  );
});

test("First account created claims /app_config/owner once; second account cannot overwrite or steal it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // First user creates /app_config/owner
  await assertSucceeds(
    aliceDb.collection("app_config").doc("owner").set({
      ownerUid: ALICE_UID,
      createdAt: pastDate(),
    })
  );

  // Second user cannot overwrite or delete /app_config/owner
  await assertFails(
    bobDb.collection("app_config").doc("owner").set({
      ownerUid: BOB_UID,
      createdAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("app_config").doc("owner").delete());
  await assertFails(aliceDb.collection("app_config").doc("owner").delete());

  // First-account owner can equip apex_sovereign skin & godMode
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validUserPayload(ALICE_UID, { selectedSkinId: "apex_sovereign" })
    )
  );

  // Second user (non-owner) cannot equip apex_sovereign skin
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set(
      validUserPayload(BOB_UID, { selectedSkinId: "apex_sovereign" })
    )
  );
});

test("Email spoofing test: unverified owner email is rejected for admin powers, verified succeeds", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", { email: OWNER_EMAIL, email_verified: false })
    .firestore();
  const verifiedOwnerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true })
    .firestore();

  // Seed Bob's leaderboard entry
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("leaderboard").doc(BOB_UID).set(
      validLeaderboardPayload(BOB_UID)
    );
  });

  // Unverified spoof cannot modify Bob's entry or equip apex_sovereign
  await assertFails(
    spoofDb.collection("leaderboard").doc(BOB_UID).update({
      maxLength: 9999,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    spoofDb.collection("users").doc("spoof_uid").set(
      validUserPayload("spoof_uid", { selectedSkinId: "apex_sovereign" })
    )
  );

  // Verified owner email succeeds
  await assertSucceeds(
    verifiedOwnerDb.collection("users").doc(OWNER_UID).set(
      validUserPayload(OWNER_UID, { selectedSkinId: "apex_sovereign" })
    )
  );
  await assertSucceeds(
    verifiedOwnerDb.collection("leaderboard").doc(BOB_UID).update({
      maxLength: 9999,
      updatedAt: pastDate(500),
    })
  );
});

test("User profiles enforce ownership isolation, valid schemas, and immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Partial update of allowed fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedSkinId: "plasma_cobra",
      selectedPattern: "STRIPED",
      preferredDifficulty: "HARD",
      updatedAt: pastDate(500),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(100),
      updatedAt: pastDate(100),
    })
  );

  // Cross-user read & write fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: pastDate(500),
    })
  );
});

test("Leaderboard enforces filtered query alignment and prevents cross-user tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID))
  );

  // Filtered query matching Kotlin repository succeeds
  await assertSucceeds(
    bobDb.collection("leaderboard").where("visibility", "==", "PUBLIC").get()
  );

  // Unfiltered list query is rejected
  await assertFails(bobDb.collection("leaderboard").get());

  // Bob cannot modify Alice's leaderboard score
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      maxLength: 10,
      updatedAt: pastDate(500),
    })
  );
});

test("Multiplayer rooms enforce create, join, leave, host updates, and terminal locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_1").set(validRoomPayload(ALICE_UID))
  );

  // Filtered room query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("rooms").where("visibility", "==", "PUBLIC").get()
  );
  await assertFails(bobDb.collection("rooms").get());

  // Bob joins room_1
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      playerCount: 2,
      updatedAt: pastDate(500),
    })
  );

  // Bob (non-host) cannot change botDifficulty
  await assertFails(
    bobDb.collection("rooms").doc("room_1").update({
      botDifficulty: "APEX",
      updatedAt: pastDate(400),
    })
  );

  // Host (Alice) updates botDifficulty & status to ACTIVE
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_1").update({
      botDifficulty: "HARD",
      status: "ACTIVE",
      updatedAt: pastDate(300),
    })
  );

  // Host marks room FINISHED -> terminal state locking prevents further non-admin updates
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_1").update({
      status: "FINISHED",
      updatedAt: pastDate(200),
    })
  );
  await assertFails(
    aliceDb.collection("rooms").doc("room_1").update({
      botDifficulty: "EASY",
      updatedAt: pastDate(100),
    })
  );
});

test("Room snakes subcollection enforces membership, bot host ownership, and godMode restriction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ownerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true })
    .firestore();

  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_2").set(validRoomPayload(ALICE_UID))
  );

  // Alice (host & member) creates her own snake and a bot snake
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_2").collection("snakes").doc(ALICE_UID)
      .set(validSnakePayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_2").collection("snakes").doc("bot_0")
      .set(validSnakePayload(ALICE_UID, { botControlled: true, displayName: "Bot Alpha" }))
  );

  // Bob (not a room member yet) cannot create a snake in room_2
  await assertFails(
    bobDb.collection("rooms").doc("room_2").collection("snakes").doc(BOB_UID)
      .set(validSnakePayload(BOB_UID))
  );

  // Bob joins room_2
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_2").update({
      memberUids: [ALICE_UID, BOB_UID],
      playerCount: 2,
      updatedAt: pastDate(500),
    })
  );

  // Bob can create his own human snake, CANNOT enable godMode, CANNOT create a bot snake
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_2").collection("snakes").doc(BOB_UID)
      .set(validSnakePayload(BOB_UID, { displayName: "BobViper" }))
  );
  await assertFails(
    bobDb.collection("rooms").doc("room_2").collection("snakes").doc(BOB_UID)
      .update({ godMode: true, updatedAt: pastDate(200) })
  );
  await assertFails(
    bobDb.collection("rooms").doc("room_2").collection("snakes").doc("bot_1")
      .set(validSnakePayload(BOB_UID, { botControlled: true }))
  );

  // Filtered subcollection query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_2").collection("snakes")
      .where("roomVisibility", "==", "PUBLIC").get()
  );
  await assertFails(
    bobDb.collection("rooms").doc("room_2").collection("snakes").get()
  );

  // Verified owner can join and enable godMode
  await assertSucceeds(
    ownerDb.collection("rooms").doc("room_2").update({
      memberUids: [ALICE_UID, BOB_UID, OWNER_UID],
      playerCount: 3,
      updatedAt: pastDate(300),
    })
  );
  await assertSucceeds(
    ownerDb.collection("rooms").doc("room_2").collection("snakes").doc(OWNER_UID)
      .set(validSnakePayload(OWNER_UID, { godMode: true, skinId: "apex_sovereign" }))
  );
});

test("Daily reward 24-hour cooldown, coin integrity, and unique skin unlocks are enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // New user profile with initial zero currency/streak/unlocks succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validUserPayload(ALICE_UID, {
        coins: 0,
        dailyStreak: 0,
        unlockedSkinIds: [],
      })
    )
  );

  // Cannot equip a locked daily reward skin or shop skin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedSkinId: "chrono_basilisk",
      updatedAt: pastDate(500),
    })
  );

  // Cannot mint arbitrary coins without a valid daily claim
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 5000,
      updatedAt: pastDate(500),
    })
  );

  // First-ever daily reward claim succeeds (Day 1 -> +100 coins)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 100,
      dailyStreak: 1,
      lastDailyClaimAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Second claim within 24 hours is rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 250,
      dailyStreak: 2,
      lastDailyClaimAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Simulate 25 hours passing since last claim, with 300 coins saved up
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      coins: 300,
      dailyStreak: 2,
      lastDailyClaimAt: new Date(Date.now() - 25 * 60 * 60 * 1000),
    });
  });

  // Claiming Day 3 after 24h unlocks chrono_basilisk + 50 coins
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 350,
      dailyStreak: 3,
      unlockedSkinIds: ["chrono_basilisk"],
      lastDailyClaimAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Now equipping chrono_basilisk succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedSkinId: "chrono_basilisk",
      updatedAt: pastDate(500),
    })
  );

  // Unlocking shop skin quantum_anaconda by spending 250 coins succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 100,
      unlockedSkinIds: ["chrono_basilisk", "quantum_anaconda"],
      selectedSkinId: "quantum_anaconda",
      updatedAt: pastDate(400),
    })
  );

  // Cannot buy daily-reward-only solar_eclipse with coins outside of a 24h claim
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      coins: 600,
    });
  });
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 350,
      unlockedSkinIds: ["chrono_basilisk", "quantum_anaconda", "solar_eclipse"],
      updatedAt: pastDate(300),
    })
  );
});
