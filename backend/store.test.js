const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { Store, approvedAmountsOk } = require("./store");

test("3×10=30 allows 2×15=30 and higher qty at same total", () => {
  assert.equal(approvedAmountsOk(3, 10, 2, 15), true);
  assert.equal(approvedAmountsOk(3, 10, 6, 5), true);
  assert.equal(approvedAmountsOk(3, 10, 1, 30), true);
  assert.equal(approvedAmountsOk(3, 10, 3, 10), true);
});

test("3×10=30 rejects total 31 and other overages", () => {
  assert.equal(approvedAmountsOk(3, 10, 1, 31), false);
  assert.equal(approvedAmountsOk(3, 10, 2, 16), false);
  assert.equal(approvedAmountsOk(3, 10, 4, 8), false);
});

test("allows a lower approved total and rejects non-positive amounts", () => {
  assert.equal(approvedAmountsOk(3, 10, 1, 10), true);
  assert.equal(approvedAmountsOk(3, 10, 3, 5), true);
  assert.equal(approvedAmountsOk(3, 10, 0, 10), false);
  assert.equal(approvedAmountsOk(3, 10, 3, 0), false);
  assert.equal(approvedAmountsOk(3, 10, -1, 10), false);
  assert.equal(approvedAmountsOk(3, 10, 3, 1.5), false);
});

test("review persists swapped qty/price when total matches and books that total", () => {
  withStore((store) => {
    const { alice, bob } = twoMembers(store);
    const requestId = insertPending(store, alice, { quantity: 3, unitPriceCents: 10 });
    assert.equal(store.review(alice.household_id, requestId, bob, true, "", Date.now(), 2, 15), "ok");
    const row = store.getRequest(alice.household_id, requestId);
    assert.equal(row.status, "APPROVED");
    assert.equal(row.approved_quantity, 2);
    assert.equal(row.approved_unit_price_cents, 15);
    const expenses = store.listExpenses(alice.household_id, 0, Date.now() + 1_000);
    assert.equal(expenses.length, 1);
    assert.equal(expenses[0].amount_cents, 30);
  });
});

test("review rejects totals above the asked amount and does not keep a qty-only cap", () => {
  withStore((store) => {
    const { alice, bob } = twoMembers(store);
    const overId = insertPending(store, alice, { quantity: 3, unitPriceCents: 10 });
    assert.equal(store.review(alice.household_id, overId, bob, true, "", Date.now(), 1, 31), "bad_amount");
    assert.equal(store.getRequest(alice.household_id, overId).status, "PENDING");

    const higherQtyId = insertPending(store, alice, { quantity: 3, unitPriceCents: 10 });
    assert.equal(store.review(alice.household_id, higherQtyId, bob, true, "", Date.now(), 6, 5), "ok");
    assert.equal(store.getRequest(alice.household_id, higherQtyId).approved_quantity, 6);
  });
});

function withStore(work) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "save-money-"));
  const store = new Store(root);
  try {
    work(store);
  } finally {
    store.db.close();
    fs.rmSync(root, { recursive: true, force: true });
  }
}

function twoMembers(store) {
  const a = store.createHousehold("Alice");
  const b = store.joinHousehold(a.householdCode, "Bob");
  return { alice: store.memberByToken(a.token), bob: store.memberByToken(b.token) };
}

test("new household defaults to cat and dog presets and can switch preset or photo", () => {
  withStore((store) => {
    const { alice, bob } = twoMembers(store);
    const aliceSession = store.sessionPayload(alice.token);
    const bobSession = store.sessionPayload(bob.token);
    const aliceRow = aliceSession.members.find((m) => m.id === alice.id);
    const bobRow = bobSession.members.find((m) => m.id === bob.id);
    assert.equal(aliceRow.avatarPreset, "mascot_cat");
    assert.equal(bobRow.avatarPreset, "mascot_dog");
    assert.equal(aliceRow.avatarFile, null);

    store.setAvatarPreset(alice.id, "kitten");
    const afterPreset = store.sessionPayload(alice.token).members.find((m) => m.id === alice.id);
    assert.equal(afterPreset.avatarPreset, "kitten");
    assert.equal(afterPreset.avatarFile, null);

    const filename = store.savePhoto(Buffer.from("fake-image"), ".png");
    store.setAvatarFile(alice.id, filename);
    const afterFile = store.sessionPayload(alice.token).members.find((m) => m.id === alice.id);
    assert.equal(afterFile.avatarPreset, null);
    assert.equal(afterFile.avatarFile, filename);
    assert.ok(store.photoPath(filename));

    store.setAvatarPreset(alice.id, "corgi");
    const backToPreset = store.sessionPayload(alice.token).members.find((m) => m.id === alice.id);
    assert.equal(backToPreset.avatarPreset, "corgi");
    assert.equal(backToPreset.avatarFile, null);
    assert.equal(store.photoPath(filename), null);

    assert.equal(store.knownAvatarPreset("penguin"), true);
    assert.equal(store.knownAvatarPreset("dragon"), false);
  });
});

function insertPending(store, alice, { quantity, unitPriceCents }) {
  return store.insertRequest(alice.household_id, {
    requester_id: alice.id,
    item_name: "笔",
    category: "其他",
    unit_price_cents: unitPriceCents,
    quantity,
    reason: "用",
    requester_name: alice.name,
    created_at: Date.now(),
  });
}
