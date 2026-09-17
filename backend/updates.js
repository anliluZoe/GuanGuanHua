const fs = require("fs");
const path = require("path");

const APK_NAME = /^[A-Za-z0-9][A-Za-z0-9._-]*\.apk$/i;

class UpdateStore {
  constructor(root) {
    this.dir = path.join(root, "updates");
    fs.mkdirSync(this.dir, { recursive: true });
  }

  attach(app) {
    app.get("/api/update/latest", (_req, res) => {
      const latest = this.readLatest();
      if (!latest.ok) {
        const status = latest.missing ? 404 : 500;
        return res.status(status).json({
          detail: latest.missing ? "还没有发布新版本" : latest.error,
        });
      }
      if (!this.apkPath(latest.filename)) {
        return res.status(404).json({ detail: "还没有放安装包" });
      }
      const body = {
        versionCode: latest.versionCode,
        versionName: latest.versionName,
        apkUrl: `/api/update/download/${encodeURIComponent(latest.filename)}`,
      };
      if (latest.notes) body.notes = latest.notes;
      res.json(body);
    });

    app.get("/api/update/download/:filename", (req, res) => {
      const filePath = this.apkPath(req.params.filename);
      if (!filePath) return res.status(404).json({ detail: "没有这个安装包" });
      res.setHeader("Content-Type", "application/vnd.android.package-archive");
      res.setHeader("Content-Disposition", `attachment; filename="${path.basename(filePath)}"`);
      res.sendFile(filePath);
    });
  }

  readLatest() {
    const file = path.join(this.dir, "latest.json");
    if (!fs.existsSync(file)) return { ok: false, missing: true };
    let raw;
    try {
      raw = JSON.parse(fs.readFileSync(file, "utf8"));
    } catch {
      return { ok: false, error: "latest.json 不是合法 JSON" };
    }
    if (raw == null || typeof raw !== "object" || Array.isArray(raw)) {
      return { ok: false, error: "latest.json 格式不对" };
    }
    const versionCode = Number(raw.versionCode);
    const versionName = String(raw.versionName || "").trim();
    let fromUrl = "";
    if (typeof raw.apkUrl === "string") {
      const trimmed = raw.apkUrl.trim().split("?")[0].replace(/\/+$/, "");
      fromUrl = trimmed.slice(trimmed.lastIndexOf("/") + 1);
    }
    const filename = this.safeApkName(raw.filename || raw.apk || fromUrl);
    if (!Number.isInteger(versionCode) || versionCode <= 0 || !versionName || !filename) {
      return { ok: false, error: "latest.json 缺少有效的 versionCode、versionName 或安装包文件名" };
    }
    const notes = raw.notes == null ? undefined : String(raw.notes).trim();
    return {
      ok: true,
      versionCode,
      versionName,
      filename,
      notes: notes || undefined,
    };
  }

  apkPath(filename) {
    const safe = this.safeApkName(filename);
    if (!safe) return null;
    const root = path.resolve(this.dir);
    const resolved = path.resolve(this.dir, safe);
    if (resolved !== root && !resolved.startsWith(root + path.sep)) return null;
    return fs.existsSync(resolved) && fs.statSync(resolved).isFile() ? resolved : null;
  }

  safeApkName(filename) {
    const raw = String(filename || "").trim();
    if (!raw || raw.includes("\0") || raw.includes("/") || raw.includes("\\")) return null;
    if (raw === "." || raw === "..") return null;
    if (!APK_NAME.test(raw)) return null;
    return raw;
  }
}

module.exports = { UpdateStore };
