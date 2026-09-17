const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const http = require("node:http");
const os = require("os");
const path = require("path");
const express = require("express");
const { UpdateStore } = require("./updates");

test("missing latest.json returns 404 with a clear body", async () => {
  await withServer(async ({ base }) => {
    const res = await fetch(`${base}/api/update/latest`);
    assert.equal(res.status, 404);
    assert.equal((await res.json()).detail, "还没有发布新版本");
  });
});

test("latest.json is parsed into versionCode, versionName, relative apkUrl, notes", async () => {
  await withServer(async ({ base, dir }) => {
    publish(dir, {
      versionCode: 3,
      versionName: "1.2.0",
      filename: "saveMoney.apk",
      notes: "修了点小毛病",
    });
    const res = await fetch(`${base}/api/update/latest`);
    assert.equal(res.status, 200);
    assert.deepEqual(await res.json(), {
      versionCode: 3,
      versionName: "1.2.0",
      apkUrl: "/api/update/download/saveMoney.apk",
      notes: "修了点小毛病",
    });
  });
});

test("latest.json without notes omits the field and accepts apkUrl as filename source", async () => {
  await withServer(async ({ base, dir }) => {
    const apk = "household-debug.apk";
    fs.writeFileSync(path.join(dir, apk), Buffer.from("apk-bytes"));
    fs.writeFileSync(
      path.join(dir, "latest.json"),
      JSON.stringify({
        versionCode: 4,
        versionName: "1.2.1",
        apkUrl: `/updates/${apk}`,
      })
    );
    const body = await (await fetch(`${base}/api/update/latest`)).json();
    assert.equal(body.apkUrl, "/api/update/download/household-debug.apk");
    assert.equal(body.notes, undefined);
  });
});

test("invalid latest.json returns 500", async () => {
  await withServer(async ({ base, dir }) => {
    fs.writeFileSync(path.join(dir, "latest.json"), "{not json");
    const res = await fetch(`${base}/api/update/latest`);
    assert.equal(res.status, 500);
    assert.match((await res.json()).detail, /合法 JSON/);
  });
});

test("latest.json with path-traversal filename is rejected", async () => {
  await withServer(async ({ base, dir }) => {
    fs.writeFileSync(
      path.join(dir, "latest.json"),
      JSON.stringify({ versionCode: 2, versionName: "1.1.0", filename: "../secret.apk" })
    );
    const res = await fetch(`${base}/api/update/latest`);
    assert.equal(res.status, 500);
    assert.match((await res.json()).detail, /文件名/);
  });
});

test("download serves only files inside updates/ and rejects traversal", async () => {
  await withServer(async ({ base, dir, root }) => {
    publish(dir, { versionCode: 2, versionName: "1.1.0", filename: "saveMoney.apk" }, Buffer.from("apk-bytes"));
    fs.writeFileSync(path.join(root, "secret.apk"), "nope");

    const ok = await fetch(`${base}/api/update/download/saveMoney.apk`);
    assert.equal(ok.status, 200);
    assert.equal(ok.headers.get("content-type"), "application/vnd.android.package-archive");
    assert.equal(Buffer.from(await ok.arrayBuffer()).toString(), "apk-bytes");

    const traversal = await fetch(`${base}/api/update/download/${encodeURIComponent("../secret.apk")}`);
    assert.equal(traversal.status, 404);
    assert.equal((await traversal.json()).detail, "没有这个安装包");

    const missing = await fetch(`${base}/api/update/download/other.apk`);
    assert.equal(missing.status, 404);
  });
});

test("latest 404s when the apk file is missing", async () => {
  await withServer(async ({ base, dir }) => {
    fs.writeFileSync(
      path.join(dir, "latest.json"),
      JSON.stringify({ versionCode: 5, versionName: "1.3.0", filename: "saveMoney.apk" })
    );
    const res = await fetch(`${base}/api/update/latest`);
    assert.equal(res.status, 404);
    assert.equal((await res.json()).detail, "还没有放安装包");
  });
});

test("publish-update.sh copies apk and writes latest.json", () => {
  const { spawnSync } = require("node:child_process");
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "save-money-publish-"));
  try {
    const apk = path.join(root, "saveMoney.apk");
    fs.writeFileSync(apk, "apk-bytes");
    const result = spawnSync(
      "bash",
      [path.join(__dirname, "scripts/publish-update.sh"), apk, "1042", "1.2.42", "ci-notes"],
      { env: { ...process.env, SAVE_MONEY_DATA: root }, encoding: "utf8" }
    );
    assert.equal(result.status, 0, result.stderr || result.stdout);
    const latest = JSON.parse(fs.readFileSync(path.join(root, "updates", "latest.json"), "utf8"));
    assert.equal(latest.versionCode, 1042);
    assert.equal(latest.versionName, "1.2.42");
    assert.equal(latest.filename, "saveMoney.apk");
    assert.equal(latest.notes, "ci-notes");
    assert.equal(fs.readFileSync(path.join(root, "updates", "saveMoney.apk"), "utf8"), "apk-bytes");
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

function publish(dir, payload, bytes = Buffer.from("apk-bytes")) {
  fs.writeFileSync(path.join(dir, payload.filename), bytes);
  fs.writeFileSync(path.join(dir, "latest.json"), JSON.stringify(payload));
}

function withServer(work) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "save-money-updates-"));
  const store = new UpdateStore(root);
  const app = express();
  store.attach(app);
  const server = http.createServer(app);
  return new Promise((resolve, reject) => {
    server.listen(0, "127.0.0.1", async () => {
      const { port } = server.address();
      try {
        await work({ root, dir: store.dir, base: `http://127.0.0.1:${port}` });
        resolve();
      } catch (error) {
        reject(error);
      } finally {
        server.close();
        fs.rmSync(root, { recursive: true, force: true });
      }
    });
  });
}
