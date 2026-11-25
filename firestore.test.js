const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PARENT_UID = "parent_alice";
const KID_UID = "kid_bob";
const STRANGER_UID = "stranger_eve";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(PARENT_UID).get());
  await assertFails(unauthDb.collection("devices").where("memberUids", "array-contains", PARENT_UID).get());
  await assertFails(unauthDb.collection("pairing_codes").doc("A7K9P2").get());
  await assertFails(
    unauthDb.collection("users").doc(PARENT_UID).set({
      userId: PARENT_UID,
      displayName: "Anon",
      role: "parent",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile: owner create, partial update, and cross-user isolation", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    parentDb.collection("users").doc(PARENT_UID).set({
      userId: PARENT_UID,
      displayName: "Parent Alice",
      email: "alice@example.com",
      role: "parent",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    parentDb.collection("users").doc(PARENT_UID).update({
      role: "kid",
      updatedAt: pastDate(500),
    })
  );

  // Reject ghost field / @DocumentId id field
  await assertFails(
    parentDb.collection("users").doc(PARENT_UID).update({
      id: PARENT_UID,
      updatedAt: pastDate(500),
    })
  );

  // Cross-user read & write denied
  await assertFails(strangerDb.collection("users").doc(PARENT_UID).get());
  await assertFails(strangerDb.collection("users").doc(PARENT_UID).update({ displayName: "Hack", updatedAt: pastDate() }));
});

test("Pairing codes: parent creates, kid claims by exact code, stranger cannot list", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const kidDb = testEnv.authenticatedContext(KID_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    parentDb.collection("pairing_codes").doc("FAM942").set({
      parentUid: PARENT_UID,
      parentName: "Alice",
      deviceNameHint: "Pixel Kid",
      status: "pending",
      ttlMinutes: 15,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Parent filtered query succeeds
  await assertSucceeds(
    parentDb.collection("pairing_codes").where("parentUid", "==", PARENT_UID).get()
  );

  // Unfiltered list fails
  await assertFails(strangerDb.collection("pairing_codes").get());

  // Kid claims by exact code
  await assertSucceeds(
    kidDb.collection("pairing_codes").doc("FAM942").update({
      status: "claimed",
      claimedByUid: KID_UID,
      claimedDeviceId: "dev_pixel_1",
      updatedAt: pastDate(500),
    })
  );
});

test("ManagedDevice & subcollections: parity with Kotlin payloads and memberUids queries", async () => {
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const kidDb = testEnv.authenticatedContext(KID_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = pastDate(10000);

  const devicePayload = {
    parentUid: PARENT_UID,
    childUid: KID_UID,
    memberUids: [PARENT_UID, KID_UID],
    deviceName: "REALME RMX3834",
    kidName: "Aarav Phone",
    manufacturer: "REALME",
    model: "RMX3834",
    androidVersion: "15",
    kidsAppVersion: "1.8.0",
    connectionState: "online",
    batteryPercent: 91,
    batteryCharging: false,
    batteryTempC: 31,
    batteryCurrentMa: -320,
    networkType: "Wi-Fi",
    activeAppName: "Quickstep",
    wallpaperId: "starstreak",
    brightnessPercent: 68,
    volumePercent: 55,
    ringerMode: "ring",
    dailyScreenTimeMinutes: 84,
    dailyLimitMinutes: 180,
    pinHash: "sha256_demo_hash",
    floatingPanelEnabled: true,
    activeLocationSession: false,
    activeCameraSession: false,
    activeAudioSession: false,
    activeScreenShareSession: false,
    permissionsGrantedCount: 5,
    permissionsTotalCount: 8,
    createdAt: created,
    updatedAt: created,
  };

  await assertSucceeds(parentDb.collection("devices").doc("dev_1").set(devicePayload));

  // Unfiltered list must fail
  await assertFails(parentDb.collection("devices").get());

  // Filtered memberUids list succeeds for parent and kid, fails for stranger
  await assertSucceeds(parentDb.collection("devices").where("memberUids", "array-contains", PARENT_UID).get());
  await assertSucceeds(kidDb.collection("devices").where("memberUids", "array-contains", KID_UID).get());
  await assertFails(strangerDb.collection("devices").doc("dev_1").get());

  // Parent updates wallpaper & sliders
  await assertSucceeds(
    parentDb.collection("devices").doc("dev_1").update({
      wallpaperId: "violet_aurora",
      brightnessPercent: 80,
      volumePercent: 60,
      updatedAt: pastDate(500),
    })
  );

  // Kid updates live telemetry
  await assertSucceeds(
    kidDb.collection("devices").doc("dev_1").update({
      batteryPercent: 89,
      activeLocationSession: true,
      updatedAt: pastDate(300),
    })
  );

  // Reject future timestamp or immutable createdAt mutation
  await assertFails(
    parentDb.collection("devices").doc("dev_1").update({
      createdAt: pastDate(100),
      updatedAt: pastDate(100),
    })
  );

  // Commands: parent creates, kid approves
  await assertSucceeds(
    parentDb.collection("devices").doc("dev_1").collection("commands").doc("cmd_1").set({
      parentUid: PARENT_UID,
      childUid: KID_UID,
      memberUids: [PARENT_UID, KID_UID],
      commandType: "SPEAK_MESSAGE",
      payloadText: "Time for dinner!",
      status: "pending",
      resultMessage: "Waiting for child device",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    kidDb.collection("devices").doc("dev_1").collection("commands").doc("cmd_1").update({
      status: "executed",
      resultMessage: "Spoken aloud via TTS",
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    parentDb.collection("devices").doc("dev_1").collection("commands").where("memberUids", "array-contains", PARENT_UID).get()
  );

  // Audit logs: create succeeds, update always fails (immutable!)
  await assertSucceeds(
    kidDb.collection("devices").doc("dev_1").collection("audit_logs").doc("log_1").set({
      parentUid: PARENT_UID,
      childUid: KID_UID,
      memberUids: [PARENT_UID, KID_UID],
      actorUid: KID_UID,
      actorRole: "child",
      action: "SPEAK_MESSAGE",
      details: "Child device spoke parent message aloud",
      status: "ok",
      createdAt: created,
    })
  );
  await assertFails(
    parentDb.collection("devices").doc("dev_1").collection("audit_logs").doc("log_1").update({
      status: "declined",
    })
  );

  // Screen time rules
  await assertSucceeds(
    parentDb.collection("devices").doc("dev_1").collection("screen_time_rules").doc("rule_1").set({
      parentUid: PARENT_UID,
      childUid: KID_UID,
      memberUids: [PARENT_UID, KID_UID],
      packageName: "com.google.android.youtube",
      appName: "YouTube",
      dailyLimitMinutes: 60,
      paused: false,
      scheduleLabel: "Bedtime 9 PM - 7 AM",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Location shares: child creates & updates while active
  await assertSucceeds(
    kidDb.collection("devices").doc("dev_1").collection("location_shares").doc("loc_1").set({
      parentUid: PARENT_UID,
      childUid: KID_UID,
      memberUids: [PARENT_UID, KID_UID],
      latitude: 23.8103,
      longitude: 90.4125,
      accuracyMeters: 12.5,
      provider: "FusedLocation",
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});
