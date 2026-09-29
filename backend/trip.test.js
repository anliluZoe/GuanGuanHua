const { test, after } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const http = require("node:http");
const os = require("os");
const path = require("path");
const { DatabaseSync } = require("node:sqlite");

const dataRoot = fs.mkdtempSync(path.join(os.tmpdir(), "save-money-trip-api-"));
process.env.SAVE_MONEY_DATA = dataRoot;
const { app } = require("./server");
const { Store } = require("./store");

test("trips are shared by the household and only one can be active", async () => {
  await withServer(async ({ base }) => {
    const alice = await json(base, "POST", "/api/households", { name: "Alice" });
    const bob = await json(base, "POST", "/api/households/join", {
      name: "Bob",
      code: alice.householdCode,
    });

    const anon = await fetch(`${base}/api/trips`);
    assert.equal(anon.status, 401);

    const started = await json(base, "POST", "/api/trips", { name: "桂林阳朔", plannedEnd: "2026-10-07" }, alice.token);
    assert.equal(started.name, "桂林阳朔");
    assert.equal(started.endedAt, null);
    assert.equal(started.plannedEnd, "2026-10-07");
    assert.equal(started.stopCount, 0);

    const again = await fetch(`${base}/api/trips`, {
      method: "POST",
      headers: { Authorization: `Bearer ${bob.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ name: "再开一段" }),
    });
    assert.equal(again.status, 409);

    const active = await json(base, "GET", "/api/trips/active", null, bob.token);
    assert.equal(active.id, started.id);
    assert.equal(active.name, "桂林阳朔");

    const stop = await json(
      base,
      "POST",
      `/api/trips/${started.id}/stops`,
      { name: "某某粉店", kind: "美食", rating: 5, visitedOn: "2026-10-02", lat: 25.27, lng: 110.29 },
      bob.token
    );
    assert.equal(stop.kind, "美食");
    assert.equal(stop.rating, 5);
    assert.equal(stop.amountCents, null);

    const listed = await json(base, "GET", `/api/trips/${started.id}`, null, alice.token);
    assert.equal(listed.stops.length, 1);
    assert.equal(listed.stops[0].name, "某某粉店");

    const ended = await json(base, "POST", `/api/trips/${started.id}/end`, {}, alice.token);
    assert.ok(ended.endedAt);

    const late = await fetch(`${base}/api/trips/${started.id}/stops`, {
      method: "POST",
      headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ name: "补记", kind: "风景", visitedOn: "2026-10-03" }),
    });
    assert.equal(late.status, 409);

    const next = await json(base, "POST", "/api/trips", { name: "云南" }, bob.token);
    assert.notEqual(next.id, started.id);
    const all = await json(base, "GET", "/api/trips", null, alice.token);
    assert.equal(all.length, 2);
    assert.equal(all[0].name, "云南");
    assert.equal(all[1].name, "桂林阳朔");
  });
});

test("stop kinds ratings reorder and isolation", async () => {
  await withServer(async ({ base }) => {
    const ada = await json(base, "POST", "/api/households", { name: "Ada" });
    const other = await json(base, "POST", "/api/households", { name: "Other" });
    const trip = await json(base, "POST", "/api/trips", { name: "广西" }, ada.token);

    const stolen = await fetch(`${base}/api/trips/${trip.id}/stops`, {
      method: "POST",
      headers: { Authorization: `Bearer ${other.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ name: "偷记", kind: "美食", visitedOn: "2026-10-01" }),
    });
    assert.equal(stolen.status, 404);

    const badKind = await fetch(`${base}/api/trips/${trip.id}/stops`, {
      method: "POST",
      headers: { Authorization: `Bearer ${ada.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ name: "店", kind: "景点", visitedOn: "2026-10-01" }),
    });
    assert.equal(badKind.status, 400);

    const first = await json(
      base,
      "POST",
      `/api/trips/${trip.id}/stops`,
      { name: "客栈", kind: "住宿", visitedOn: "2026-10-01", amountCents: 28000 },
      ada.token
    );
    assert.equal(first.amountCents, 28000);
    const second = await json(
      base,
      "POST",
      `/api/trips/${trip.id}/stops`,
      { name: "米粉", kind: "美食", rating: 4, visitedOn: "2026-10-01", amountCents: 1800 },
      ada.token
    );
    await json(
      base,
      "PATCH",
      `/api/trips/${trip.id}/stops/${first.id}`,
      { rating: 3, kind: "住宿", amountCents: 26000 },
      ada.token
    );
    const listed = await json(base, "GET", `/api/trips/${trip.id}`, null, ada.token);
    assert.equal(listed.spentCents, 27800);
    const cleared = await json(
      base,
      "PATCH",
      `/api/trips/${trip.id}/stops/${second.id}`,
      { amountCents: null },
      ada.token
    );
    assert.equal(cleared.amountCents, null);
    const afterClear = await json(base, "GET", `/api/trips/${trip.id}`, null, ada.token);
    assert.equal(afterClear.spentCents, 26000);
    const badAmount = await fetch(`${base}/api/trips/${trip.id}/stops/${first.id}`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${ada.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ amountCents: 0 }),
    });
    assert.equal(badAmount.status, 400);
    const reordered = await json(
      base,
      "PATCH",
      `/api/trips/${trip.id}/stops/reorder`,
      { orderedIds: [second.id, first.id] },
      ada.token
    );
    assert.equal(reordered[0].id, second.id);
    assert.equal(reordered[1].id, first.id);

    const removed = await fetch(`${base}/api/trips/${trip.id}/stops/${second.id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${ada.token}` },
    });
    assert.equal(removed.status, 200);
    const left = await json(base, "GET", `/api/trips/${trip.id}`, null, ada.token);
    assert.equal(left.stops.length, 1);
    assert.equal(left.stops[0].rating, 3);
  });
});

test("saving a stop without coords keeps Chengdu instead of wiping it", async () => {
  await withServer(async ({ base }) => {
    const ada = await json(base, "POST", "/api/households", { name: "Ada" });
    const trip = await json(base, "POST", "/api/trips", { name: "成都" }, ada.token);
    const stop = await json(
      base,
      "POST",
      `/api/trips/${trip.id}/stops`,
      { name: "宽窄巷子", kind: "风景", visitedOn: "2026-10-02", lat: 30.67, lng: 104.06 },
      ada.token
    );
    assert.equal(stop.lat, 30.67);
    assert.equal(stop.lng, 104.06);

    const patched = await json(
      base,
      "PATCH",
      `/api/trips/${trip.id}/stops/${stop.id}`,
      { name: "宽窄巷子", kind: "风景", rating: 4, lat: null, lng: null, amountCents: null },
      ada.token
    );
    assert.equal(patched.lat, 30.67);
    assert.equal(patched.lng, 104.06);
    assert.equal(patched.rating, 4);

    const uploaded = await uploadStopPhoto(base, ada.token, trip.id, stop.id, "chengdu-stop");
    assert.equal(uploaded.lat, 30.67);
    assert.equal(uploaded.lng, 104.06);
    assert.equal(uploaded.photos.length, 1);

    const listed = await json(base, "GET", `/api/trips/${trip.id}`, null, ada.token);
    assert.equal(listed.stops[0].lat, 30.67);
    assert.equal(listed.stops[0].lng, 104.06);
  });
});

test("stop photos are household scoped and capped at six", async () => {
  await withServer(async ({ base }) => {
    const ada = await json(base, "POST", "/api/households", { name: "Ada" });
    const other = await json(base, "POST", "/api/households", { name: "Other" });
    const trip = await json(base, "POST", "/api/trips", { name: "广西" }, ada.token);
    const stop = await json(
      base,
      "POST",
      `/api/trips/${trip.id}/stops`,
      { name: "漓江", kind: "风景", visitedOn: "2026-10-02" },
      ada.token
    );
    assert.deepEqual(stop.photos, []);

    const stolen = await fetch(`${base}/api/trips/${trip.id}/stops/${stop.id}/photos`, {
      method: "POST",
      headers: { Authorization: `Bearer ${other.token}` },
      body: (() => {
        const form = new FormData();
        form.append("image", new Blob(["nope"]), "x.jpg");
        return form;
      })(),
    });
    assert.equal(stolen.status, 404);

    const uploaded = await uploadStopPhoto(base, ada.token, trip.id, stop.id, "photo-one");
    assert.equal(uploaded.photos.length, 1);
    assert.match(uploaded.photos[0].url, /\/api\/files\/.+\.jpg$/);
    const file = await fetch(uploaded.photos[0].url);
    assert.equal(file.status, 200);
    assert.equal(Buffer.from(await file.arrayBuffer()).toString(), "photo-one");

    for (let i = 0; i < 5; i += 1) {
      await uploadStopPhoto(base, ada.token, trip.id, stop.id, `photo-${i + 2}`);
    }
    const seventh = await fetch(`${base}/api/trips/${trip.id}/stops/${stop.id}/photos`, {
      method: "POST",
      headers: { Authorization: `Bearer ${ada.token}` },
      body: (() => {
        const form = new FormData();
        form.append("image", new Blob(["overflow"]), "x.jpg");
        return form;
      })(),
    });
    assert.equal(seventh.status, 400);

    const detail = await json(base, "GET", `/api/trips/${trip.id}`, null, ada.token);
    assert.equal(detail.stops[0].photos.length, 6);
    const photoId = detail.stops[0].photos[0].id;
    const afterDelete = await json(
      base,
      "DELETE",
      `/api/trips/${trip.id}/stops/${stop.id}/photos/${photoId}`,
      null,
      ada.token
    );
    assert.equal(afterDelete.photos.length, 5);

    await json(base, "POST", `/api/trips/${trip.id}/end`, {}, ada.token);
    const late = await fetch(`${base}/api/trips/${trip.id}/stops/${stop.id}/photos`, {
      method: "POST",
      headers: { Authorization: `Bearer ${ada.token}` },
      body: (() => {
        const form = new FormData();
        form.append("image", new Blob(["late"]), "x.jpg");
        return form;
      })(),
    });
    assert.equal(late.status, 409);
  });
});

test("opening an older database adds trip tables", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "trip-migrate-"));
  const dbPath = path.join(root, "save_money.db");
  const old = new DatabaseSync(dbPath);
  old.exec(`
    CREATE TABLE households (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE NOT NULL);
    CREATE TABLE members (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      household_id INTEGER NOT NULL,
      token TEXT UNIQUE NOT NULL,
      name TEXT NOT NULL
    );
    CREATE TABLE trips (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      household_id INTEGER NOT NULL,
      name TEXT NOT NULL,
      started_at INTEGER NOT NULL,
      ended_at INTEGER,
      planned_end TEXT,
      created_by INTEGER NOT NULL,
      created_at INTEGER NOT NULL
    );
    CREATE TABLE trip_stops (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      trip_id INTEGER NOT NULL,
      name TEXT NOT NULL,
      kind TEXT NOT NULL,
      rating INTEGER,
      lat REAL,
      lng REAL,
      visited_on TEXT NOT NULL,
      sort_order INTEGER NOT NULL,
      created_by INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );
  `);
  old.prepare("INSERT INTO households(code) VALUES ('111111')").run();
  old.prepare("INSERT INTO members(household_id, token, name) VALUES (1, 'tok', 'Ada')").run();
  old.close();
  const store = new Store(root);
  try {
    const tables = store.db.prepare("SELECT name FROM sqlite_master WHERE type = 'table'").all().map((row) => row.name);
    assert.ok(tables.includes("trips"));
    assert.ok(tables.includes("trip_stops"));
    assert.ok(tables.includes("trip_stop_photos"));
    const columns = store.db.prepare("PRAGMA table_info(trip_stops)").all().map((row) => row.name);
    assert.ok(columns.includes("amount_cents"));
    const started = store.startTrip(1, 1, "桂林", "2026-10-07");
    assert.equal(started.name, "桂林");
    assert.equal(store.activeTrip(1).id, started.id);
  } finally {
    store.db.close();
    fs.rmSync(root, { recursive: true, force: true });
  }
});

after(() => {
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

async function uploadStopPhoto(base, token, tripId, stopId, bytes) {
  const form = new FormData();
  form.append("image", new Blob([bytes]), "stop.jpg");
  const res = await fetch(`${base}/api/trips/${tripId}/stops/${stopId}/photos`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
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
