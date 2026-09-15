const path = require("path");
const express = require("express");
const cors = require("cors");
const multer = require("multer");
const { Store } = require("./store");

const DATA_ROOT = process.env.SAVE_MONEY_DATA || path.join(__dirname, "data");
const store = new Store(DATA_ROOT);
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

app.post("/api/households", (req, res) => {
  const name = String(req.body?.name || "").trim();
  if (!name) return res.status(400).json({ detail: "先填一下名字" });
  res.json(store.createHousehold(name));
});

app.post("/api/households/join", (req, res) => {
  const name = String(req.body?.name || "").trim();
  const code = String(req.body?.code || "").trim();
  if (!name || !code) return res.status(400).json({ detail: "名字和家庭码都要填" });
  const session = store.joinHousehold(code, name);
  if (!session) return res.status(404).json({ detail: "找不到这个家庭码" });
  res.json(session);
});

app.get("/api/session", requireMember, (req, res) => {
  res.json(store.sessionPayload(req.member.token));
});

app.put("/api/session", requireMember, (req, res) => {
  const name = String(req.body?.name || "").trim();
  if (!name) return res.status(400).json({ detail: "名字不能为空" });
  store.updateName(req.member.id, name);
  res.json(store.sessionPayload(req.member.token));
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
  const outcome = store.review(
    req.member.household_id,
    Number(req.params.id),
    req.member,
    Boolean(req.body?.approve),
    String(req.body?.comment || "").trim(),
    Date.now()
  );
  if (outcome === "own") return res.status(403).json({ detail: "自己的申请要留给对方审哦" });
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
app.listen(port, "0.0.0.0", () => {
  console.log(`省钱助手 API  http://0.0.0.0:${port}`);
});
