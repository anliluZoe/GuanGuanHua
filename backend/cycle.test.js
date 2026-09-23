const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const http = require("node:http");
const os = require("os");
const path = require("path");
const { DatabaseSync } = require("node:sqlite");

const dataRoot = fs.mkdtempSync(path.join(os.tmpdir(), "save-money-cycle-api-"));
process.env.SAVE_MONEY_DATA = dataRoot;
const { app } = require("./server");
const { Store } = require("./store");

test("cycles and settings stay on the signed-in member", async () => {
  await withServer(async ({ base }) => {
    const alice = await json(base, "POST", "/api/households", { name: "Alice" });
    const bob = await json(base, "POST", "/api/households/join", {
      name: "Bob",
      code: alice.householdCode,
    });

    const anon = await fetch(`${base}/api/cycles`);
    assert.equal(anon.status, 401);

    const created = await json(base, "POST", "/api/cycles", { start: "2026-09-18", end: "2026-09-22" }, alice.token);
    assert.equal(created.start, "2026-09-18");
    assert.equal(created.end, "2026-09-22");

    const aliceList = await json(base, "GET", "/api/cycles", null, alice.token);
    assert.equal(aliceList.length, 1);
    assert.equal(aliceList[0].id, created.id);

    const bobList = await json(base, "GET", "/api/cycles", null, bob.token);
    assert.deepEqual(bobList, []);

    const stolen = await fetch(`${base}/api/cycles/${created.id}`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${bob.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ start: "2026-09-01", end: "2026-09-05" }),
    });
    assert.equal(stolen.status, 404);

    const still = await json(base, "GET", "/api/cycles", null, alice.token);
    assert.equal(still[0].start, "2026-09-18");

    const bobSameDay = await json(base, "POST", "/api/cycles", { start: "2026-09-18" }, bob.token);
    assert.equal(bobSameDay.end, null);
    assert.notEqual(bobSameDay.id, created.id);
    const aliceAgain = await json(base, "GET", "/api/cycles", null, alice.token);
    assert.equal(aliceAgain.length, 1);

    const removed = await fetch(`${base}/api/cycles/${created.id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${alice.token}` },
    });
    assert.equal(removed.status, 404);
    assert.equal((await json(base, "GET", "/api/cycles", null, alice.token)).length, 1);
  });
});

test("cycle dates, duplicates, and settings validation", async () => {
  await withServer(async ({ base }) => {
    const alice = await json(base, "POST", "/api/households", { name: "Alice" });
    const bob = await json(base, "POST", "/api/households/join", {
      name: "Bob",
      code: alice.householdCode,
    });
    const first = await json(base, "POST", "/api/cycles", { start: "2026-07-28", end: "2026-08-02" }, alice.token);

    const dup = await fetch(`${base}/api/cycles`, {
      method: "POST",
      headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ start: "2026-07-28", end: "2026-08-01" }),
    });
    assert.equal(dup.status, 409);

    const backwards = await fetch(`${base}/api/cycles`, {
      method: "POST",
      headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ start: "2026-08-25", end: "2026-08-20" }),
    });
    assert.equal(backwards.status, 400);

    const badDay = await fetch(`${base}/api/cycles`, {
      method: "POST",
      headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ start: "2026-02-31" }),
    });
    assert.equal(badDay.status, 400);

    const second = await json(base, "POST", "/api/cycles", { start: "2026-08-25" }, alice.token);
    const edited = await json(
      base,
      "PATCH",
      `/api/cycles/${second.id}`,
      { start: "2026-08-25", end: "2026-08-30" },
      alice.token,
    );
    assert.equal(edited.end, "2026-08-30");

    const cleared = await json(
      base,
      "PATCH",
      `/api/cycles/${second.id}`,
      { start: "2026-08-25", end: null },
      alice.token,
    );
    assert.equal(cleared.end, null);

    const clash = await fetch(`${base}/api/cycles/${second.id}`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ start: first.start, end: null }),
    });
    assert.equal(clash.status, 409);

    const defaults = await json(base, "GET", "/api/cycle-settings", null, alice.token);
    assert.deepEqual(defaults, {
      referenceCycleDays: null,
      periodDays: 5,
      remindEnabled: true,
      remindDays: 2,
    });

    const saved = await json(
      base,
      "PATCH",
      "/api/cycle-settings",
      { referenceCycleDays: 30, periodDays: 6, remindEnabled: false },
      alice.token,
    );
    assert.equal(saved.referenceCycleDays, 30);
    assert.equal(saved.periodDays, 6);
    assert.equal(saved.remindEnabled, false);
    assert.equal(saved.remindDays, 2);

    const bobSettings = await json(base, "GET", "/api/cycle-settings", null, bob.token);
    assert.equal(bobSettings.referenceCycleDays, null);
    assert.equal(bobSettings.remindEnabled, true);

    const edges = await json(base, "PATCH", "/api/cycle-settings", { referenceCycleDays: 18 }, alice.token);
    assert.equal(edges.referenceCycleDays, 18);
    assert.equal(edges.periodDays, 6);
    const clearedRef = await json(base, "PATCH", "/api/cycle-settings", { referenceCycleDays: null }, alice.token);
    assert.equal(clearedRef.referenceCycleDays, null);
    assert.equal(clearedRef.periodDays, 6);

    for (const bad of [{ referenceCycleDays: 17 }, { referenceCycleDays: 46 }, { periodDays: 0 }, { periodDays: 15 }]) {
      const res = await fetch(`${base}/api/cycle-settings`, {
        method: "PATCH",
        headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
        body: JSON.stringify(bad),
      });
      assert.equal(res.status, 400, JSON.stringify(bad));
    }
  });
});

test("opening an older database adds cycle tables and leaving removes that member's rows", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "cycle-migrate-"));
  const db = new DatabaseSync(path.join(root, "save_money.db"));
  db.exec(`
    CREATE TABLE households (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      code TEXT UNIQUE NOT NULL
    );
    CREATE TABLE members (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      household_id INTEGER NOT NULL,
      token TEXT UNIQUE NOT NULL,
      name TEXT NOT NULL
    );
    INSERT INTO households(code) VALUES ('654321');
    INSERT INTO members(household_id, token, name) VALUES (1, 'tok-ada', 'Ada');
  `);
  db.close();

  const store = new Store(root);
  try {
    const house = store.db.prepare("SELECT code FROM households WHERE id = 1").get();
    assert.equal(house.code, "654321");
    const tables = store.db
      .prepare("SELECT name FROM sqlite_master WHERE type = 'table'")
      .all()
      .map((row) => row.name);
    assert.ok(tables.includes("cycle_records"));
    assert.ok(tables.includes("cycle_settings"));
    assert.ok(tables.includes("purchase_requests"));

    const ada = store.memberByToken("tok-ada");
    const cycle = store.createCycle(ada.id, "2026-09-18", "2026-09-22");
    store.patchCycleSettings(ada.id, { reference_cycle_days: 30, period_days: 4 });
    store.leaveHousehold(ada.id);
    assert.equal(store.listCycles(ada.id).length, 0);
    assert.equal(store.getCycle(ada.id, cycle.id), undefined);
    const settings = store.getCycleSettings(ada.id);
    assert.equal(settings.reference_cycle_days, null);
    assert.equal(settings.period_days, 5);
  } finally {
    store.db.close();
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test.after(() => {
  fs.rmSync(dataRoot, { recursive: true, force: true });
});

async function json(base, method, pathname, body, token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${base}${pathname}`, {
    method,
    headers,
    body: body == null ? undefined : JSON.stringify(body),
  });
  const payload = await res.json();
  assert.equal(res.status, 200, JSON.stringify(payload));
  return payload;
}

function withServer(work) {
  const server = http.createServer(app);
  return new Promise((resolve, reject) => {
    server.listen(0, "127.0.0.1", async () => {
      const { port } = server.address();
      try {
        await work({ base: `http://127.0.0.1:${port}` });
        resolve();
      } catch (error) {
        reject(error);
      } finally {
        server.close();
      }
    });
  });
}
