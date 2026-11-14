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
const CHARLIE_UID = "charlie_789";
const ADMIN_UID = "admin_001";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTs(context) {
  // @firebase/rules-unit-testing exposes firebase.firestore.FieldValue.serverTimestamp() via assertSucceeds with admin or RulesTestContext
  return new Date();
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("scans").get());
  await assertFails(unauthDb.collection("scamAlerts").where("moderationStatus", "==", "approved").get());
  await assertFails(unauthDb.collection("familyGroups").get());
});

test("User profile isolation and privilege escalation protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  const validProfile = {
    uid: ALICE_UID,
    displayName: "Alice Khan",
    email: "alice@example.com",
    country: "US",
    language: "en",
    theme: "system",
    subscriptionType: "free",
    accountStatus: "active",
    saveHistory: true,
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update with isAdmin ghost field must fail
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ isAdmin: true, updatedAt: new Date() }));

  // Self-upgrade subscriptionType must fail
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ subscriptionType: "premium", updatedAt: new Date() }));

  // Valid update succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({ theme: "dark", updatedAt: new Date() }));
});

test("Scan history is strictly isolated per user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const scanPayload = {
    userId: ALICE_UID,
    scanType: "message",
    inputPreview: "Urgent bank notice...",
    riskLevel: "high",
    riskScore: 88,
    confidence: "high",
    category: "bank_impersonation",
    summary: "Contains urgent credential request.",
    warningSigns: ["Urgency", "Suspicious link"],
    evidenceObservations: ["Domain mismatch"],
    recommendation: "Do not click the link.",
    verificationSteps: ["Open official banking app"],
    verificationStatus: "suspicious",
    country: "US",
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("scans").doc("scan_1").set(scanPayload));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("scans").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("scans").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("scans").doc("scan_1").delete());
});

test("Family group, invitations, and shared alerts enforce membership and verified email", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, { email: "charlie@example.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("familyGroups").doc("fam_1").set({
    ownerId: ALICE_UID,
    name: "Khan Family",
    memberUids: [ALICE_UID],
    memberLabels: ["Alice"],
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  // Non-member cannot read
  await assertFails(charlieDb.collection("familyGroups").doc("fam_1").get());

  // Alice invites Bob
  await assertSucceeds(aliceDb.collection("familyInvitations").doc("inv_1").set({
    groupId: "fam_1",
    groupName: "Khan Family",
    inviterUid: ALICE_UID,
    inviterName: "Alice",
    inviteeEmail: "bob@example.com",
    status: "pending",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  // Bob accepts invitation and joins group
  await assertSucceeds(bobDb.collection("familyInvitations").doc("inv_1").update({
    status: "accepted",
    updatedAt: new Date(),
  }));
  await assertSucceeds(bobDb.collection("familyGroups").doc("fam_1").update({
    memberUids: [ALICE_UID, BOB_UID],
    memberLabels: ["Alice", "Bob"],
    updatedAt: new Date(),
  }));

  // Member Bob can share privacy-safe alert; Charlie cannot
  await assertSucceeds(bobDb.collection("familyGroups").doc("fam_1").collection("sharedAlerts").doc("alert_1").set({
    groupId: "fam_1",
    senderUid: BOB_UID,
    senderName: "Bob",
    memberUids: [ALICE_UID, BOB_UID],
    scanType: "message",
    riskLevel: "high",
    riskScore: 90,
    category: "bank_impersonation",
    privacySafeSummary: "High-risk fake bank alert detected.",
    warningSigns: ["Urgent OTP request"],
    recommendation: "Never share OTP.",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  await assertFails(charlieDb.collection("familyGroups").doc("fam_1").collection("sharedAlerts").get());
});

test("Community scam reports & admin moderation rules prevent spoofing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_1", { email: "docmahamkhalid@gmail.com", email_verified: false }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "docmahamkhalid@gmail.com", email_verified: true }).firestore();

  // Client cannot self-approve report
  await assertFails(aliceDb.collection("scamReports").doc("rep_bad").set({
    userId: ALICE_UID,
    reportType: "phone",
    category: "Bank impersonation",
    normalizedTarget: "+14155550199",
    country: "US",
    description: "Claimed to be bank fraud dept.",
    moderationStatus: "approved",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  await assertSucceeds(aliceDb.collection("scamReports").doc("rep_1").set({
    userId: ALICE_UID,
    reportType: "phone",
    category: "Bank impersonation",
    normalizedTarget: "+14155550199",
    country: "US",
    description: "Claimed to be bank fraud dept.",
    moderationStatus: "pending",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  // Spoofed unverified admin email cannot moderate or publish scamAlerts
  await assertFails(spoofedAdminDb.collection("scamReports").doc("rep_1").update({
    moderationStatus: "approved",
    updatedAt: new Date(),
  }));
  await assertFails(spoofedAdminDb.collection("scamAlerts").doc("al_1").set({
    title: "Spoof Alert",
    explanation: "Should fail",
    country: "US",
    category: "Banking Scams",
    sourceType: "community_moderated",
    reportCount: 5,
    warningSigns: ["OTP request"],
    recommendedAction: "Ignore",
    moderationStatus: "approved",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  // Real verified admin can approve report and publish scamAlert
  await assertSucceeds(realAdminDb.collection("scamReports").doc("rep_1").update({
    moderationStatus: "approved",
    updatedAt: new Date(),
  }));
  await assertSucceeds(realAdminDb.collection("scamAlerts").doc("al_1").set({
    title: "Fake Bank Fraud Desk Call",
    explanation: "Callers claim unauthorized card charges and ask for OTP codes.",
    country: "US",
    category: "Banking Scams",
    sourceType: "community_moderated",
    reportCount: 12,
    warningSigns: ["Requests OTP code", "Urgency"],
    recommendedAction: "Hang up and call the number on the back of your card.",
    moderationStatus: "approved",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  // Authenticated user must filter by moderationStatus == 'approved'
  await assertFails(aliceDb.collection("scamAlerts").get());
  await assertSucceeds(aliceDb.collection("scamAlerts").where("moderationStatus", "==", "approved").get());
});
