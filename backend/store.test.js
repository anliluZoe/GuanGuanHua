const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { DatabaseSync } = require("node:sqlite");
const {
  Store,
  approvedAmountsOk,
  MAX_WIDGET_CAPTION,
  DEFAULT_WIDGET_CAPTION_COLOR,
  normalizeWidgetCaptionColor,
} = require("./store");

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

test("widget starts empty and is shared for the household", () => {
  withStore((store) => {
    const { alice, bob } = twoMembers(store);
    const empty = store.getWidget(alice.household_id);
    assert.equal(empty.imageFile, null);
    assert.equal(empty.caption, "");
    assert.equal(empty.captionColor, "#FFFFFF");
    assert.equal(empty.updatedBy, null);
    assert.equal(empty.updatedAt, null);

    const first = store.savePhoto(Buffer.from("cover-one"), ".jpg");
    store.setWidget(alice.household_id, alice.id, { imageFile: first, caption: "周末去看海" });
    const aliceView = store.getWidget(alice.household_id);
    const bobView = store.getWidget(bob.household_id);
    assert.equal(aliceView.imageFile, first);
    assert.equal(aliceView.caption, "周末去看海");
    assert.equal(aliceView.captionColor, "#FFFFFF");
    assert.equal(aliceView.updatedBy, "Alice");
    assert.equal(typeof aliceView.updatedAt, "number");
    assert.deepEqual(bobView, aliceView);
    assert.ok(store.photoPath(first));
  });
});

test("widget caption is trimmed and capped, image replace deletes the old file", () => {
  withStore((store) => {
    const { alice, bob } = twoMembers(store);
    const first = store.savePhoto(Buffer.from("cover-one"), ".png");
    store.setWidget(alice.household_id, alice.id, { imageFile: first, caption: "  先写一句  " });
    assert.equal(store.getWidget(alice.household_id).caption, "先写一句");

    const long = "啊".repeat(MAX_WIDGET_CAPTION + 8);
    store.setWidget(alice.household_id, bob.id, { caption: long });
    const afterCaption = store.getWidget(alice.household_id);
    assert.equal(afterCaption.caption, "啊".repeat(MAX_WIDGET_CAPTION));
    assert.equal(afterCaption.captionColor, "#FFFFFF");
    assert.equal(afterCaption.imageFile, first);
    assert.equal(afterCaption.updatedBy, "Bob");

    const second = store.savePhoto(Buffer.from("cover-two"), ".jpg");
    store.setWidget(alice.household_id, bob.id, { imageFile: second });
    const afterImage = store.getWidget(alice.household_id);
    assert.equal(afterImage.imageFile, second);
    assert.equal(afterImage.caption, "啊".repeat(MAX_WIDGET_CAPTION));
    assert.equal(store.photoPath(first), null);
    assert.ok(store.photoPath(second));
  });
});

test("widget state is per household", () => {
  withStore((store) => {
    const a = store.createHousehold("Alice");
    const alice = store.memberByToken(a.token);
    const other = store.createHousehold("Cara");
    const cara = store.memberByToken(other.token);
    store.setWidget(alice.household_id, alice.id, {
      imageFile: store.savePhoto(Buffer.from("a"), ".jpg"),
      caption: "我们的",
    });
    store.setWidget(cara.household_id, cara.id, { caption: "另一家" });
    assert.equal(store.getWidget(alice.household_id).caption, "我们的");
    assert.equal(store.getWidget(cara.household_id).caption, "另一家");
    assert.equal(store.getWidget(cara.household_id).captionColor, "#FFFFFF");
    assert.equal(store.getWidget(cara.household_id).imageFile, null);
  });
});

test("widget caption color persists, normalizes hex, and survives image replace", () => {
  withStore((store) => {
    const { alice, bob } = twoMembers(store);
    store.setWidget(alice.household_id, alice.id, { caption: "想吃火锅", captionColor: "#f07" });
    const afterPreset = store.getWidget(alice.household_id);
    assert.equal(afterPreset.caption, "想吃火锅");
    assert.equal(afterPreset.captionColor, "#FF0077");

    store.setWidget(alice.household_id, bob.id, { captionColor: "7eb8d8" });
    const afterColor = store.getWidget(alice.household_id);
    assert.equal(afterColor.caption, "想吃火锅");
    assert.equal(afterColor.captionColor, "#7EB8D8");
    assert.equal(afterColor.updatedBy, "Bob");

    const cover = store.savePhoto(Buffer.from("cover"), ".jpg");
    store.setWidget(alice.household_id, alice.id, { imageFile: cover });
    assert.equal(store.getWidget(alice.household_id).captionColor, "#7EB8D8");
    assert.equal(store.getWidget(alice.household_id).caption, "想吃火锅");
  });
});

test("widget caption color migrates onto older households tables", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "save-money-widget-color-"));
  try {
    const db = new DatabaseSync(path.join(root, "save_money.db"));
    db.exec(`
      CREATE TABLE households (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        code TEXT UNIQUE NOT NULL,
        widget_image_file TEXT,
        widget_caption TEXT,
        widget_updated_by INTEGER,
        widget_updated_at INTEGER
      );
      CREATE TABLE members (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        household_id INTEGER NOT NULL,
        token TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL
      );
    `);
    db.prepare("INSERT INTO households(code) VALUES (?)").run("OLD123");
    db.close();
    const store = new Store(root);
    const columns = store.db.prepare("PRAGMA table_info(households)").all().map((c) => c.name);
    assert.ok(columns.includes("widget_caption_color"));
    assert.equal(store.getWidget(1).captionColor, DEFAULT_WIDGET_CAPTION_COLOR);
    store.db.close();
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test("normalizeWidgetCaptionColor expands short hex and rejects junk", () => {
  assert.equal(normalizeWidgetCaptionColor(null), "#FFFFFF");
  assert.equal(normalizeWidgetCaptionColor("  "), "#FFFFFF");
  assert.equal(normalizeWidgetCaptionColor("#fff"), "#FFFFFF");
  assert.equal(normalizeWidgetCaptionColor("f07a5c"), "#F07A5C");
  assert.equal(normalizeWidgetCaptionColor("#GG0000"), null);
  assert.equal(normalizeWidgetCaptionColor("red"), null);
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
