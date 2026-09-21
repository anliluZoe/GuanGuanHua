const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const http = require("node:http");
const os = require("os");
const path = require("path");

const dataRoot = fs.mkdtempSync(path.join(os.tmpdir(), "save-money-household-api-"));
process.env.SAVE_MONEY_DATA = dataRoot;
const { app } = require("./server");

test("join is capped at two members and can enter as an existing person", async () => {
  await withServer(async ({ base }) => {
    const alice = await json(base, "POST", "/api/households", { name: "Alice" });
    const bob = await json(base, "POST", "/api/households/join", {
      name: "Bob",
      code: alice.householdCode,
    });
    assert.equal(bob.members.length, 2);

    const full = await fetch(`${base}/api/households/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: "Cara", code: alice.householdCode }),
    });
    const fullBody = await full.json();
    assert.equal(full.status, 409);
    assert.equal(fullBody.code, "household_full");
    assert.match(fullBody.detail, /两个人/);
    assert.equal(fullBody.members.length, 2);
    assert.deepEqual(
      fullBody.members.map((member) => member.name).sort(),
      ["Alice", "Bob"],
    );
    assert.ok(fullBody.members.every((member) => typeof member.id === "number"));

    const asBob = await json(base, "POST", "/api/households/join", {
      code: alice.householdCode,
      memberId: bob.memberId,
    });
    assert.equal(asBob.token, bob.token);
    assert.equal(asBob.memberId, bob.memberId);
    assert.equal(asBob.name, "Bob");

    const stranger = await json(base, "POST", "/api/households", { name: "Zed" });
    const mismatch = await fetch(`${base}/api/households/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code: alice.householdCode, memberId: stranger.memberId }),
    });
    assert.equal(mismatch.status, 400);
    assert.match((await mismatch.json()).detail, /不在这个家庭/);
  });
});

test("leaving frees a slot so the next join creates a new member", async () => {
  await withServer(async ({ base }) => {
    const alice = await json(base, "POST", "/api/households", { name: "Alice" });
    const bob = await json(base, "POST", "/api/households/join", {
      name: "Bob",
      code: alice.householdCode,
    });

    const left = await fetch(`${base}/api/session`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${bob.token}` },
    });
    assert.equal(left.status, 200);
    assert.equal((await left.json()).ok, true);

    const stale = await fetch(`${base}/api/session`, {
      headers: { Authorization: `Bearer ${bob.token}` },
    });
    assert.equal(stale.status, 401);

    const stillAlice = await json(base, "GET", "/api/session", null, alice.token);
    assert.equal(stillAlice.members.length, 1);
    assert.equal(stillAlice.members[0].name, "Alice");

    const cara = await json(base, "POST", "/api/households/join", {
      name: "Cara",
      code: alice.householdCode,
    });
    assert.equal(cara.name, "Cara");
    assert.notEqual(cara.token, bob.token);
    assert.equal(cara.members.length, 2);
    assert.deepEqual(
      cara.members.map((member) => member.name).sort(),
      ["Alice", "Cara"],
    );
  });
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
