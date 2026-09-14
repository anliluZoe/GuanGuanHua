"""SQLite 家庭账本存储：成员令牌、申请、消费、预算、照片。"""

from __future__ import annotations

import os
import sqlite3
import uuid
from pathlib import Path


def _connect(path: Path) -> sqlite3.Connection:
    conn = sqlite3.connect(path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


class Store:
    def __init__(self, root: Path):
        self.root = root
        self.root.mkdir(parents=True, exist_ok=True)
        self.photos = self.root / "photos"
        self.photos.mkdir(exist_ok=True)
        self.conn = _connect(self.root / "save_money.db")
        self._init_schema()

    def _init_schema(self) -> None:
        self.conn.executescript(
            """
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
            """
        )
        self.conn.commit()

    def create_household(self, name: str, role: str) -> dict:
        code = f"{uuid.uuid4().int % 1_000_000:06d}"
        token = uuid.uuid4().hex
        requester = name if role == "REQUESTER" else "申请人"
        approver = name if role == "APPROVER" else "审核人"
        cur = self.conn.cursor()
        cur.execute(
            "INSERT INTO households(code, requester_name, approver_name) VALUES (?,?,?)",
            (code, requester, approver),
        )
        hid = cur.lastrowid
        cur.execute(
            "INSERT INTO members(household_id, token, role, name) VALUES (?,?,?,?)",
            (hid, token, role, name),
        )
        self.conn.commit()
        return self.session_payload(token)

    def join_household(self, code: str, name: str, role: str) -> dict | None:
        row = self.conn.execute("SELECT * FROM households WHERE code = ?", (code.strip(),)).fetchone()
        if row is None:
            return None
        token = uuid.uuid4().hex
        if role == "REQUESTER":
            self.conn.execute("UPDATE households SET requester_name = ? WHERE id = ?", (name, row["id"]))
        else:
            self.conn.execute("UPDATE households SET approver_name = ? WHERE id = ?", (name, row["id"]))
        self.conn.execute(
            "INSERT INTO members(household_id, token, role, name) VALUES (?,?,?,?)",
            (row["id"], token, role, name),
        )
        self.conn.commit()
        return self.session_payload(token)

    def member_by_token(self, token: str) -> sqlite3.Row | None:
        return self.conn.execute(
            """
            SELECT m.*, h.code, h.requester_name, h.approver_name
            FROM members m JOIN households h ON h.id = m.household_id
            WHERE m.token = ?
            """,
            (token,),
        ).fetchone()

    def session_payload(self, token: str) -> dict:
        row = self.member_by_token(token)
        assert row is not None
        return {
            "token": token,
            "householdCode": row["code"],
            "role": row["role"],
            "requesterName": row["requester_name"],
            "approverName": row["approver_name"],
        }

    def update_names(self, household_id: int, requester: str, approver: str) -> None:
        self.conn.execute(
            "UPDATE households SET requester_name = ?, approver_name = ? WHERE id = ?",
            (requester, approver, household_id),
        )
        self.conn.commit()

    def update_role(self, member_id: int, role: str) -> None:
        self.conn.execute("UPDATE members SET role = ? WHERE id = ?", (role, member_id))
        self.conn.commit()

    def save_photo(self, data: bytes, suffix: str = ".jpg") -> str:
        filename = uuid.uuid4().hex + suffix
        (self.photos / filename).write_bytes(data)
        return filename

    def photo_path(self, filename: str) -> Path | None:
        path = (self.photos / Path(filename).name).resolve()
        if not str(path).startswith(str(self.photos.resolve())) or not path.is_file():
            return None
        return path

    def list_requests(self, household_id: int) -> list[sqlite3.Row]:
        return self.conn.execute(
            "SELECT * FROM purchase_requests WHERE household_id = ? ORDER BY created_at DESC",
            (household_id,),
        ).fetchall()

    def get_request(self, household_id: int, request_id: int) -> sqlite3.Row | None:
        return self.conn.execute(
            "SELECT * FROM purchase_requests WHERE household_id = ? AND id = ?",
            (household_id, request_id),
        ).fetchone()

    def insert_request(self, household_id: int, fields: dict) -> int:
        cur = self.conn.cursor()
        cur.execute(
            """
            INSERT INTO purchase_requests(
                household_id, item_name, category, unit_price_cents, quantity, reason,
                requester_name, status, created_at, image_file
            ) VALUES (?,?,?,?,?,?,?,?,?,?)
            """,
            (
                household_id,
                fields["item_name"],
                fields["category"],
                fields["unit_price_cents"],
                fields["quantity"],
                fields["reason"],
                fields["requester_name"],
                "PENDING",
                fields["created_at"],
                fields.get("image_file"),
            ),
        )
        self.conn.commit()
        return int(cur.lastrowid)

    def review(self, household_id: int, request_id: int, approve: bool, reviewer: str, comment: str, now: int) -> bool:
        row = self.get_request(household_id, request_id)
        if row is None or row["status"] != "PENDING":
            return False
        status = "APPROVED" if approve else "REJECTED"
        self.conn.execute(
            """
            UPDATE purchase_requests
            SET status = ?, reviewed_at = ?, reviewer_name = ?, review_comment = ?
            WHERE id = ?
            """,
            (status, now, reviewer, comment or None, request_id),
        )
        if approve:
            self.conn.execute(
                """
                INSERT INTO expense_records(
                    household_id, request_id, item_name, category, amount_cents,
                    spent_at, requester_name, reviewer_name
                ) VALUES (?,?,?,?,?,?,?,?)
                """,
                (
                    household_id,
                    request_id,
                    row["item_name"],
                    row["category"],
                    row["unit_price_cents"] * row["quantity"],
                    now,
                    row["requester_name"],
                    reviewer,
                ),
            )
        self.conn.commit()
        return True

    def withdraw(self, household_id: int, request_id: int) -> bool:
        row = self.get_request(household_id, request_id)
        if row is None or row["status"] != "PENDING":
            return False
        if row["image_file"]:
            path = self.photo_path(row["image_file"])
            if path:
                path.unlink(missing_ok=True)
        self.conn.execute("DELETE FROM purchase_requests WHERE id = ?", (request_id,))
        self.conn.commit()
        return True

    def list_expenses(self, household_id: int, start_ms: int, end_ms: int) -> list[sqlite3.Row]:
        return self.conn.execute(
            """
            SELECT * FROM expense_records
            WHERE household_id = ? AND spent_at >= ? AND spent_at < ?
            ORDER BY spent_at DESC
            """,
            (household_id, start_ms, end_ms),
        ).fetchall()

    def get_budget(self, household_id: int, year_month: str) -> sqlite3.Row | None:
        return self.conn.execute(
            "SELECT * FROM monthly_budgets WHERE household_id = ? AND year_month = ?",
            (household_id, year_month),
        ).fetchone()

    def set_budget(self, household_id: int, year_month: str, amount_cents: int) -> None:
        self.conn.execute(
            """
            INSERT INTO monthly_budgets(household_id, year_month, amount_cents) VALUES (?,?,?)
            ON CONFLICT(household_id, year_month) DO UPDATE SET amount_cents = excluded.amount_cents
            """,
            (household_id, year_month, amount_cents),
        )
        self.conn.commit()
