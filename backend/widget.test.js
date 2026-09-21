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
    assert.equal(empty.widgetCaptionColor, "#FFFFFF");
    assert.equal(empty.widgetUpdatedBy, null);
    assert.equal(empty.widgetUpdatedAt, null);

    const uploaded = await uploadImage(base, alice.token, "cover-one");
    assert.match(uploaded.widgetImageUrl, /\/api\/files\/.+\.jpg$/);
    assert.equal(uploaded.widgetCaption, "");
    assert.equal(uploaded.widgetCaptionColor, "#FFFFFF");
    assert.equal(uploaded.widgetUpdatedBy, "Alice");
    assert.equal(typeof uploaded.widgetUpdatedAt, "number");

    const patched = await json(base, "PATCH", "/api/widget", { widgetCaption: "  周末去看海  " }, bob.token);
    assert.equal(patched.widgetCaption, "周末去看海");
    assert.equal(patched.widgetCaptionColor, "#FFFFFF");
    assert.equal(patched.widgetUpdatedBy, "Bob");
    assert.equal(patched.widgetImageUrl, uploaded.widgetImageUrl);

    const recolored = await json(base, "PATCH", "/api/widget", { widgetCaptionColor: "#f07a5c" }, alice.token);
    assert.equal(recolored.widgetCaption, "周末去看海");
    assert.equal(recolored.widgetCaptionColor, "#F07A5C");
    assert.equal(recolored.widgetUpdatedBy, "Alice");

    const both = await json(
      base,
      "PATCH",
      "/api/widget",
      { widgetCaption: "想吃火锅", widgetCaptionColor: "#7eb8d8" },
      bob.token,
    );
    assert.equal(both.widgetCaption, "想吃火锅");
    assert.equal(both.widgetCaptionColor, "#7EB8D8");

    const afterPhoto = await uploadImage(base, alice.token, "cover-two");
    assert.equal(afterPhoto.widgetCaption, "想吃火锅");
    assert.equal(afterPhoto.widgetCaptionColor, "#7EB8D8");

    const fromAlice = await json(base, "GET", "/api/widget", null, alice.token);
    assert.deepEqual(fromAlice, afterPhoto);

    const photo = await fetch(afterPhoto.widgetImageUrl);
    assert.equal(photo.status, 200);
    assert.equal(Buffer.from(await photo.arrayBuffer()).toString(), "cover-two");

    const cleared = await fetch(`${base}/api/widget/image`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${alice.token}` },
    });
    const clearedBody = await cleared.json();
    assert.equal(cleared.status, 200, JSON.stringify(clearedBody));
    assert.equal(clearedBody.widgetImageUrl, null);
    assert.equal(clearedBody.widgetCaption, "想吃火锅");
    assert.equal(clearedBody.widgetCaptionColor, "#7EB8D8");
    assert.equal((await fetch(afterPhoto.widgetImageUrl)).status, 404);

    const missingCaption = await fetch(`${base}/api/widget`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({}),
    });
    assert.equal(missingCaption.status, 400);

    const other = await json(base, "POST", "/api/households", { name: "Cara" });
    const badColor = await fetch(`${base}/api/widget`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ widgetCaptionColor: "not-a-color" }),
    });
    assert.equal(badColor.status, 400);

    const otherWidget = await json(base, "GET", "/api/widget", null, other.token);
    assert.equal(otherWidget.widgetCaption, "");
    assert.equal(otherWidget.widgetCaptionColor, "#FFFFFF");
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
