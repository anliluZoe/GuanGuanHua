const path = require("path");
const express = require("express");
const cors = require("cors");
const multer = require("multer");
const { Store, normalizeWidgetCaptionColor } = require("./store");
const { UpdateStore } = require("./updates");

const DATA_ROOT = process.env.SAVE_MONEY_DATA || path.join(__dirname, "data");
const store = new Store(DATA_ROOT);
const updates = new UpdateStore(DATA_ROOT);
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 8 * 1024 * 1024 } });
const app = express();
app.use(cors());
app.use(express.json());

function requestJson(row, req) {
  return {
    id: row.id,
    requesterId: row.requester_id,
    mine: row.requester_id === req.member.id,
    itemName: row.item_name,
    category: row.category,
    unitPriceCents: row.unit_price_cents,
    quantity: row.quantity,
    approvedUnitPriceCents: row.approved_unit_price_cents == null ? null : Number(row.approved_unit_price_cents),
    approvedQuantity: row.approved_quantity == null ? null : Number(row.approved_quantity),
    partial:
      row.status === "APPROVED" &&
      row.approved_quantity != null &&
      (Number(row.approved_quantity) !== Number(row.quantity) ||
        Number(row.approved_unit_price_cents) !== Number(row.unit_price_cents)),
    reason: row.reason,
    requesterName: row.requester_name,
    status: row.status,
    createdAt: row.created_at,
    reviewedAt: row.reviewed_at,
    reviewerName: row.reviewer_name,
    reviewComment: row.review_comment,
    imagePath: row.image_file ? `${req.protocol}://${req.get("host")}/api/files/${row.image_file}` : null,
  };
}

function requireMember(req, res, next) {
  const header = req.get("authorization") || "";
  if (!header.toLowerCase().startsWith("bearer ")) {
    return res.status(401).json({ detail: "需要登录" });
  }
  const member = store.memberByToken(header.slice(7).trim());
  if (!member) return res.status(401).json({ detail: "令牌无效" });
  req.member = member;
  next();
}

app.get("/api/health", (_req, res) => res.json({ ok: true }));
updates.attach(app);

function fileUrl(req, filename) {
  if (!filename) return null;
  return `${req.protocol}://${req.get("host")}/api/files/${encodeURIComponent(filename)}`;
}

function memberJson(req, member) {
  return {
    id: member.id,
    name: member.name,
    avatarPreset: member.avatarPreset,
    avatarUrl: fileUrl(req, member.avatarFile),
  };
}

function sessionJson(req, token) {
  const payload = store.sessionPayload(token);
  return {
    ...payload,
    members: payload.members.map((member) => memberJson(req, member)),
  };
}

app.post("/api/households", (req, res) => {
  const name = String(req.body?.name || "").trim();
  if (!name) return res.status(400).json({ detail: "先填一下名字" });
  const created = store.createHousehold(name);
  res.json(sessionJson(req, created.token));
});

app.post("/api/households/join", (req, res) => {
  const name = String(req.body?.name || "").trim();
  const code = String(req.body?.code || "").trim();
  const rawMemberId = req.body?.memberId;
  let memberId = null;
  if (rawMemberId != null && rawMemberId !== "") {
    memberId = Number(rawMemberId);
    if (!Number.isSafeInteger(memberId) || memberId < 1) {
      return res.status(400).json({ detail: "选的人不在这个家庭里" });
    }
  }
  if (!code) return res.status(400).json({ detail: "名字和家庭码都要填" });
  if (memberId == null && !name) return res.status(400).json({ detail: "名字和家庭码都要填" });
  const session = store.joinHousehold(code, name, memberId);
  if (!session) return res.status(404).json({ detail: "找不到这个家庭码" });
  if (session.error === "bad_member") {
    return res.status(400).json({ detail: "选的人不在这个家庭里" });
  }
  if (session.householdFull) {
    return res.status(409).json({
      detail: "这个家庭已经有两个人了，请选择其中一个身份进入",
      code: "household_full",
      members: session.members.map((member) => memberJson(req, member)),
    });
  }
  res.json(sessionJson(req, session.token));
});

app.get("/api/session", requireMember, (req, res) => {
  res.json(sessionJson(req, req.member.token));
});

app.delete("/api/session", requireMember, (req, res) => {
  store.leaveHousehold(req.member.id);
  res.json({ ok: true });
});

app.put("/api/session", requireMember, (req, res) => {
  const name = String(req.body?.name || "").trim();
  const hasPreset = typeof req.body?.avatarPreset === "string";
  if (!name && !hasPreset) return res.status(400).json({ detail: "名字不能为空" });
  if (name) store.updateName(req.member.id, name);
  if (hasPreset) {
    const preset = String(req.body.avatarPreset).trim();
    if (!store.knownAvatarPreset(preset)) return res.status(400).json({ detail: "头像预设不对" });
    store.setAvatarPreset(req.member.id, preset);
  }
  res.json(sessionJson(req, req.member.token));
});

app.post("/api/session/avatar", requireMember, upload.single("avatar"), (req, res) => {
  if (!req.file || !req.file.buffer.length) return res.status(400).json({ detail: "先选一张照片" });
  const suffix = path.extname(req.file.originalname || "") || ".jpg";
  store.setAvatarFile(req.member.id, store.savePhoto(req.file.buffer, suffix));
  res.json(sessionJson(req, req.member.token));
});

function widgetJson(req, householdId) {
  const row = store.getWidget(householdId);
  return {
    widgetImageUrl: fileUrl(req, row.imageFile),
    widgetCaption: row.caption,
    widgetCaptionColor: row.captionColor,
    widgetUpdatedBy: row.updatedBy,
    widgetUpdatedAt: row.updatedAt,
  };
}

app.get("/api/widget", requireMember, (req, res) => {
  res.json(widgetJson(req, req.member.household_id));
});

app.patch("/api/widget", requireMember, (req, res) => {
  const body = req.body || {};
  const hasCaption = Object.prototype.hasOwnProperty.call(body, "widgetCaption");
  const hasColor = Object.prototype.hasOwnProperty.call(body, "widgetCaptionColor");
  if (!hasCaption && !hasColor) {
    return res.status(400).json({ detail: "请填写说明或颜色" });
  }
  const fields = {};
  if (hasCaption) fields.caption = body.widgetCaption;
  if (hasColor) {
    const captionColor = normalizeWidgetCaptionColor(body.widgetCaptionColor);
    if (!captionColor) return res.status(400).json({ detail: "颜色格式不对，请用 #RRGGBB" });
    fields.captionColor = captionColor;
  }
  store.setWidget(req.member.household_id, req.member.id, fields);
  res.json(widgetJson(req, req.member.household_id));
});

app.post("/api/widget/image", requireMember, upload.single("image"), (req, res) => {
  if (!req.file || !req.file.buffer.length) return res.status(400).json({ detail: "先选一张照片" });
  const suffix = path.extname(req.file.originalname || "") || ".jpg";
  store.setWidget(req.member.household_id, req.member.id, {
    imageFile: store.savePhoto(req.file.buffer, suffix),
  });
  res.json(widgetJson(req, req.member.household_id));
});

app.get("/api/requests", requireMember, (req, res) => {
  res.json(store.listRequests(req.member.household_id).map((row) => requestJson(row, req)));
});

app.get("/api/requests/:id", requireMember, (req, res) => {
  const row = store.getRequest(req.member.household_id, Number(req.params.id));
  if (!row) return res.status(404).json({ detail: "申请不存在" });
  res.json(requestJson(row, req));
});

app.post("/api/requests", requireMember, upload.single("image"), (req, res) => {
  const { itemName, category, unitPriceCents, quantity, reason } = req.body;
  if (!String(itemName || "").trim() || !category || !(Number(unitPriceCents) > 0) || !(Number(quantity) > 0)) {
    return res.status(400).json({ detail: "申请内容不完整" });
  }
  let imageFile = null;
  if (req.file && req.file.buffer.length > 0) {
    const suffix = path.extname(req.file.originalname || "") || ".jpg";
    imageFile = store.savePhoto(req.file.buffer, suffix);
  }
  const id = store.insertRequest(req.member.household_id, {
    requester_id: req.member.id,
    item_name: String(itemName).trim(),
    category,
    unit_price_cents: Number(unitPriceCents),
    quantity: Number(quantity),
    reason: String(reason || "").trim(),
    requester_name: req.member.name,
    created_at: Date.now(),
    image_file: imageFile,
  });
  res.json(requestJson(store.getRequest(req.member.household_id, id), req));
});

app.post("/api/requests/:id/review", requireMember, (req, res) => {
  const approve = Boolean(req.body?.approve);
  const outcome = store.review(
    req.member.household_id,
    Number(req.params.id),
    req.member,
    approve,
    String(req.body?.comment || "").trim(),
    Date.now(),
    approve && req.body?.quantity != null ? Number(req.body.quantity) : null,
    approve && req.body?.unitPriceCents != null ? Number(req.body.unitPriceCents) : null
  );
  if (outcome === "own") return res.status(403).json({ detail: "自己的申请要留给对方审哦" });
  if (outcome === "bad_amount") return res.status(400).json({ detail: "批准总额不能超过申请总额" });
  if (outcome !== "ok") return res.status(409).json({ detail: "这条申请不能审核" });
  res.json(requestJson(store.getRequest(req.member.household_id, Number(req.params.id)), req));
});

app.delete("/api/requests/:id", requireMember, (req, res) => {
  const outcome = store.withdraw(req.member.household_id, Number(req.params.id), req.member.id);
  if (outcome === "not_owner") return res.status(403).json({ detail: "只能撤回自己的申请" });
  if (outcome !== "ok") return res.status(409).json({ detail: "只能撤回待审核的申请" });
  res.json({ ok: true });
});

app.get("/api/expenses", requireMember, (req, res) => {
  const [year, month] = String(req.query.yearMonth || "").split("-").map(Number);
  const start = new Date(year, month - 1, 1).getTime();
  const end = new Date(year, month, 1).getTime();
  res.json(
    store.listExpenses(req.member.household_id, start, end).map((row) => ({
      id: row.id,
      requestId: row.request_id,
      itemName: row.item_name,
      category: row.category,
      amountCents: row.amount_cents,
      spentAt: row.spent_at,
      requesterName: row.requester_name,
      reviewerName: row.reviewer_name,
    }))
  );
});

app.get("/api/budget", requireMember, (req, res) => {
  const row = store.getBudget(req.member.household_id, String(req.query.yearMonth || ""));
  if (!row) return res.json(null);
  res.json({ yearMonth: row.year_month, amountCents: row.amount_cents });
});

app.put("/api/budget", requireMember, (req, res) => {
  const yearMonth = String(req.query.yearMonth || "");
  const amountCents = Number(req.body?.amountCents);
  if (!yearMonth || !(amountCents > 0)) {
    return res.status(400).json({ detail: "预算无效" });
  }
  store.setBudget(req.member.household_id, yearMonth, amountCents);
  res.json({ yearMonth, amountCents });
});

app.get("/api/files/:filename", (req, res) => {
  const filePath = store.photoPath(req.params.filename);
  if (!filePath) return res.status(404).json({ detail: "没有这张图" });
  res.sendFile(filePath);
});

const port = Number(process.env.PORT || 8080);
if (require.main === module) {
  app.listen(port, "0.0.0.0", () => {
    console.log(`管管花 API  http://0.0.0.0:${port}`);
  });
}

module.exports = { app };
