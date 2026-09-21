const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const http = require("node:http");
const os = require("os");
const path = require("path");

const dataRoot = fs.mkdtempSync(path.join(os.tmpdir(), "save-money-widget-api-"));
process.env.SAVE_MONEY_DATA = dataRoot;
const { app } = require("./server");

test("widget API is household-scoped and round-trips image plus caption", async () => {
  await withServer(async ({ base }) => {
    const alice = await json(base, "POST", "/api/households", { name: "Alice" });
    const bob = await json(base, "POST", "/api/households/join", {
      name: "Bob",
      code: alice.householdCode,
    });
    const empty = await json(base, "GET", "/api/widget", null, alice.token);
    assert.equal(empty.widgetImageUrl, null);
    assert.equal(empty.widgetCaption, "");
    assert.equal(empty.widgetUpdatedBy, null);
    assert.equal(empty.widgetUpdatedAt, null);

    const uploaded = await uploadImage(base, alice.token, "cover-one");
    assert.match(uploaded.widgetImageUrl, /\/api\/files\/.+\.jpg$/);
    assert.equal(uploaded.widgetCaption, "");
    assert.equal(uploaded.widgetUpdatedBy, "Alice");
    assert.equal(typeof uploaded.widgetUpdatedAt, "number");

    const patched = await json(base, "PATCH", "/api/widget", { widgetCaption: "  周末去看海  " }, bob.token);
    assert.equal(patched.widgetCaption, "周末去看海");
    assert.equal(patched.widgetUpdatedBy, "Bob");
    assert.equal(patched.widgetImageUrl, uploaded.widgetImageUrl);

    const fromAlice = await json(base, "GET", "/api/widget", null, alice.token);
    assert.deepEqual(fromAlice, patched);

    const photo = await fetch(patched.widgetImageUrl);
    assert.equal(photo.status, 200);
    assert.equal(Buffer.from(await photo.arrayBuffer()).toString(), "cover-one");

    const missingCaption = await fetch(`${base}/api/widget`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({}),
    });
    assert.equal(missingCaption.status, 400);

    const other = await json(base, "POST", "/api/households", { name: "Cara" });
    const otherWidget = await json(base, "GET", "/api/widget", null, other.token);
    assert.equal(otherWidget.widgetCaption, "");
    assert.equal(otherWidget.widgetImageUrl, null);
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

async function uploadImage(base, token, bytes) {
  const form = new FormData();
  form.append("image", new Blob([bytes]), "cover.jpg");
  const res = await fetch(`${base}/api/widget/image`, {
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
