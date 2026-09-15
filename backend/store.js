const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { DatabaseSync } = require("node:sqlite");

class Store {
  constructor(root) {
    this.root = root;
    this.photos = path.join(root, "photos");
    fs.mkdirSync(this.photos, { recursive: true });
    this.db = new DatabaseSync(path.join(root, "save_money.db"));
    this.db.exec("PRAGMA foreign_keys = ON");
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS households (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        code TEXT UNIQUE NOT NULL
      );
      CREATE TABLE IF NOT EXISTS members (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        household_id INTEGER NOT NULL REFERENCES households(id),
        token TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS purchase_requests (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        household_id INTEGER NOT NULL REFERENCES households(id),
        requester_id INTEGER,
        item_name TEXT NOT NULL,
        category TEXT NOT NULL,
        unit_price_cents INTEGER NOT NULL,
        quantity INTEGER NOT NULL,
        approved_quantity INTEGER,
        approved_unit_price_cents INTEGER,
        reason TEXT NOT NULL,
        requester_name TEXT NOT NULL,
        status TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        reviewed_at INTEGER,
        reviewer_name TEXT,
        review_comment TEXT,
        image_file TEXT
      );
      CREATE TABLE IF NOT EXISTS expense_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        household_id INTEGER NOT NULL REFERENCES households(id),
        request_id INTEGER NOT NULL,
        item_name TEXT NOT NULL,
        category TEXT NOT NULL,
        amount_cents INTEGER NOT NULL,
        spent_at INTEGER NOT NULL,
        requester_name TEXT NOT NULL,
        reviewer_name TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS monthly_budgets (
        household_id INTEGER NOT NULL REFERENCES households(id),
        year_month TEXT NOT NULL,
        amount_cents INTEGER NOT NULL,
        PRIMARY KEY (household_id, year_month)
      );
    `);
    this.migrateFromFixedRoles();
  }

  /** 早期版本每个成员有固定的申请人/审核人角色；现在谁都能申请，由对方审核。 */
  migrateFromFixedRoles() {
    const columns = (table) => this.db.prepare(`PRAGMA table_info(${table})`).all().map((c) => c.name);
    if (columns("members").includes("role")) this.db.exec("ALTER TABLE members DROP COLUMN role");
    for (const column of ["requester_name", "approver_name"]) {
      if (columns("households").includes(column)) this.db.exec(`ALTER TABLE households DROP COLUMN ${column}`);
    }
    if (!columns("purchase_requests").includes("requester_id")) {
      this.db.exec("ALTER TABLE purchase_requests ADD COLUMN requester_id INTEGER");
    }
    if (!columns("purchase_requests").includes("approved_quantity")) {
      this.db.exec("ALTER TABLE purchase_requests ADD COLUMN approved_quantity INTEGER");
      this.db.exec("ALTER TABLE purchase_requests ADD COLUMN approved_unit_price_cents INTEGER");
    }
  }

  withTransaction(work) {
    this.db.exec("BEGIN");
    try {
      const result = work();
      this.db.exec("COMMIT");
      return result;
    } catch (error) {
      this.db.exec("ROLLBACK");
      throw error;
    }
  }

  createHousehold(name) {
    const token = crypto.randomBytes(16).toString("hex");
    this.withTransaction(() => {
      let householdId;
      for (let attempt = 0; attempt < 8; attempt++) {
        const code = String(crypto.randomInt(0, 1_000_000)).padStart(6, "0");
        try {
          householdId = this.db.prepare("INSERT INTO households(code) VALUES (?)").run(code).lastInsertRowid;
          break;
        } catch (error) {
          if (String(error.message).includes("UNIQUE") && attempt < 7) continue;
          throw error;
        }
      }
      this.db
        .prepare("INSERT INTO members(household_id, token, name) VALUES (?,?,?)")
        .run(householdId, token, name);
    });
    return this.sessionPayload(token);
  }

  joinHousehold(code, name) {
    const house = this.db.prepare("SELECT * FROM households WHERE code = ?").get(String(code).trim());
    if (!house) return null;
    const token = crypto.randomBytes(16).toString("hex");
    this.db
      .prepare("INSERT INTO members(household_id, token, name) VALUES (?,?,?)")
      .run(house.id, token, name);
    return this.sessionPayload(token);
  }

  memberByToken(token) {
    return this.db
      .prepare(
        `SELECT m.*, h.code
         FROM members m JOIN households h ON h.id = m.household_id
         WHERE m.token = ?`
      )
      .get(token);
  }

  sessionPayload(token) {
    const row = this.memberByToken(token);
    const members = this.db
      .prepare("SELECT id, name FROM members WHERE household_id = ? ORDER BY id")
      .all(row.household_id);
    return {
      token,
      memberId: row.id,
      householdCode: row.code,
      name: row.name,
      members,
    };
  }

  updateName(memberId, name) {
    this.db.prepare("UPDATE members SET name = ? WHERE id = ?").run(name, memberId);
  }

  savePhoto(buffer, suffix = ".jpg") {
    const filename = crypto.randomBytes(16).toString("hex") + suffix;
    fs.writeFileSync(path.join(this.photos, filename), buffer);
    return filename;
  }

  photoPath(filename) {
    const photosRoot = path.resolve(this.photos);
    const resolved = path.resolve(this.photos, path.basename(filename));
    if (resolved !== photosRoot && !resolved.startsWith(photosRoot + path.sep)) return null;
    return fs.existsSync(resolved) ? resolved : null;
  }

  listRequests(householdId) {
    return this.db
      .prepare("SELECT * FROM purchase_requests WHERE household_id = ? ORDER BY created_at DESC")
      .all(householdId);
  }

  getRequest(householdId, requestId) {
    return this.db
      .prepare("SELECT * FROM purchase_requests WHERE household_id = ? AND id = ?")
      .get(householdId, requestId);
  }

  insertRequest(householdId, fields) {
    return Number(
      this.db
        .prepare(
          `INSERT INTO purchase_requests(
             household_id, requester_id, item_name, category, unit_price_cents, quantity, reason,
             requester_name, status, created_at, image_file
           ) VALUES (?,?,?,?,?,?,?,?,?,?,?)`
        )
        .run(
          householdId,
          fields.requester_id,
          fields.item_name,
          fields.category,
          fields.unit_price_cents,
          fields.quantity,
          fields.reason,
          fields.requester_name,
          "PENDING",
          fields.created_at,
          fields.image_file ?? null
        ).lastInsertRowid
    );
  }

  /**
   * 返回 "ok" | "not_pending" | "own" | "bad_amount"。
   * 通过时可以少买或砍价；数量/单价只能比原申请低，不能加码。入账按改过的金额。
   */
  review(householdId, requestId, reviewer, approve, comment, now, approvedQuantity, approvedUnitPriceCents) {
    const row = this.getRequest(householdId, requestId);
    if (!row || row.status !== "PENDING") return "not_pending";
    if (row.requester_id === reviewer.id) return "own";
    let quantity = row.quantity;
    let unitPrice = row.unit_price_cents;
    if (approve) {
      if (approvedQuantity != null) quantity = Number(approvedQuantity);
      if (approvedUnitPriceCents != null) unitPrice = Number(approvedUnitPriceCents);
      if (!Number.isInteger(quantity) || quantity < 1 || quantity > row.quantity) return "bad_amount";
      if (!Number.isInteger(unitPrice) || unitPrice < 1 || unitPrice > row.unit_price_cents) return "bad_amount";
    }
    this.withTransaction(() => {
      this.db
        .prepare(
          `UPDATE purchase_requests
           SET status = ?, reviewed_at = ?, reviewer_name = ?, review_comment = ?,
               approved_quantity = ?, approved_unit_price_cents = ?
           WHERE id = ?`
        )
        .run(
          approve ? "APPROVED" : "REJECTED",
          now,
          reviewer.name,
          comment || null,
          approve ? quantity : null,
          approve ? unitPrice : null,
          requestId
        );
      if (approve) {
        this.db
          .prepare(
            `INSERT INTO expense_records(
               household_id, request_id, item_name, category, amount_cents,
               spent_at, requester_name, reviewer_name
             ) VALUES (?,?,?,?,?,?,?,?)`
          )
          .run(
            householdId,
            requestId,
            row.item_name,
            row.category,
            unitPrice * quantity,
            now,
            row.requester_name,
            reviewer.name
          );
      }
    });
    return "ok";
  }

  /** 返回 "ok" | "not_pending" | "not_owner"：只能撤回自己还没被审的申请。 */
  withdraw(householdId, requestId, memberId) {
    const row = this.getRequest(householdId, requestId);
    if (!row || row.status !== "PENDING") return "not_pending";
    if (row.requester_id !== memberId) return "not_owner";
    if (row.image_file) {
      const filePath = this.photoPath(row.image_file);
      if (filePath) fs.unlinkSync(filePath);
    }
    this.db.prepare("DELETE FROM purchase_requests WHERE household_id = ? AND id = ?").run(householdId, requestId);
    return "ok";
  }

  listExpenses(householdId, startMs, endMs) {
    return this.db
      .prepare(
        `SELECT * FROM expense_records
         WHERE household_id = ? AND spent_at >= ? AND spent_at < ?
         ORDER BY spent_at DESC`
      )
      .all(householdId, startMs, endMs);
  }

  getBudget(householdId, yearMonth) {
    return this.db
      .prepare("SELECT * FROM monthly_budgets WHERE household_id = ? AND year_month = ?")
      .get(householdId, yearMonth);
  }

  setBudget(householdId, yearMonth, amountCents) {
    this.db
      .prepare(
        `INSERT INTO monthly_budgets(household_id, year_month, amount_cents) VALUES (?,?,?)
         ON CONFLICT(household_id, year_month) DO UPDATE SET amount_cents = excluded.amount_cents`
      )
      .run(householdId, yearMonth, amountCents);
  }
}

module.exports = { Store };
