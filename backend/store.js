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
        code TEXT UNIQUE NOT NULL,
        requester_name TEXT NOT NULL DEFAULT '申请人',
        approver_name TEXT NOT NULL DEFAULT '审核人'
      );
      CREATE TABLE IF NOT EXISTS members (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        household_id INTEGER NOT NULL REFERENCES households(id),
        token TEXT UNIQUE NOT NULL,
        role TEXT NOT NULL,
        name TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS purchase_requests (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        household_id INTEGER NOT NULL REFERENCES households(id),
        item_name TEXT NOT NULL,
        category TEXT NOT NULL,
        unit_price_cents INTEGER NOT NULL,
        quantity INTEGER NOT NULL,
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

  createHousehold(name, role) {
    const token = crypto.randomBytes(16).toString("hex");
    const requester = role === "REQUESTER" ? name : "申请人";
    const approver = role === "APPROVER" ? name : "审核人";
    this.withTransaction(() => {
      let householdId;
      for (let attempt = 0; attempt < 8; attempt++) {
        const code = String(crypto.randomInt(0, 1_000_000)).padStart(6, "0");
        try {
          householdId = this.db
            .prepare("INSERT INTO households(code, requester_name, approver_name) VALUES (?,?,?)")
            .run(code, requester, approver).lastInsertRowid;
          break;
        } catch (error) {
          if (String(error.message).includes("UNIQUE") && attempt < 7) continue;
          throw error;
        }
      }
      this.db
        .prepare("INSERT INTO members(household_id, token, role, name) VALUES (?,?,?,?)")
        .run(householdId, token, role, name);
    });
    return this.sessionPayload(token);
  }

  joinHousehold(code, name, role) {
    const house = this.db.prepare("SELECT * FROM households WHERE code = ?").get(String(code).trim());
    if (!house) return null;
    const token = crypto.randomBytes(16).toString("hex");
    this.withTransaction(() => {
      if (role === "REQUESTER") {
        this.db.prepare("UPDATE households SET requester_name = ? WHERE id = ?").run(name, house.id);
      } else {
        this.db.prepare("UPDATE households SET approver_name = ? WHERE id = ?").run(name, house.id);
      }
      this.db
        .prepare("INSERT INTO members(household_id, token, role, name) VALUES (?,?,?,?)")
        .run(house.id, token, role, name);
    });
    return this.sessionPayload(token);
  }

  memberByToken(token) {
    return this.db
      .prepare(
        `SELECT m.*, h.code, h.requester_name, h.approver_name
         FROM members m JOIN households h ON h.id = m.household_id
         WHERE m.token = ?`
      )
      .get(token);
  }

  sessionPayload(token) {
    const row = this.memberByToken(token);
    return {
      token,
      householdCode: row.code,
      role: row.role,
      requesterName: row.requester_name,
      approverName: row.approver_name,
    };
  }

  updateNames(householdId, requester, approver) {
    this.db
      .prepare("UPDATE households SET requester_name = ?, approver_name = ? WHERE id = ?")
      .run(requester, approver, householdId);
  }

  updateRole(memberId, role) {
    this.db.prepare("UPDATE members SET role = ? WHERE id = ?").run(role, memberId);
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
             household_id, item_name, category, unit_price_cents, quantity, reason,
             requester_name, status, created_at, image_file
           ) VALUES (?,?,?,?,?,?,?,?,?,?)`
        )
        .run(
          householdId,
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

  review(householdId, requestId, approve, reviewer, comment, now) {
    const row = this.getRequest(householdId, requestId);
    if (!row || row.status !== "PENDING") return false;
    this.withTransaction(() => {
      this.db
        .prepare(
          `UPDATE purchase_requests
           SET status = ?, reviewed_at = ?, reviewer_name = ?, review_comment = ?
           WHERE id = ?`
        )
        .run(approve ? "APPROVED" : "REJECTED", now, reviewer, comment || null, requestId);
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
            row.unit_price_cents * row.quantity,
            now,
            row.requester_name,
            reviewer
          );
      }
    });
    return true;
  }

  withdraw(householdId, requestId) {
    const row = this.getRequest(householdId, requestId);
    if (!row || row.status !== "PENDING") return false;
    if (row.image_file) {
      const filePath = this.photoPath(row.image_file);
      if (filePath) fs.unlinkSync(filePath);
    }
    this.db.prepare("DELETE FROM purchase_requests WHERE household_id = ? AND id = ?").run(householdId, requestId);
    return true;
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
