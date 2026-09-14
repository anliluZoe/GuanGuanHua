"""省钱助手 API：两台手机用同一个家庭码共享申请、审核、消费和照片。"""

from __future__ import annotations

import os
import time
from pathlib import Path

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field

from store import Store

DATA_ROOT = Path(os.environ.get("SAVE_MONEY_DATA", Path(__file__).resolve().parent / "data"))
store = Store(DATA_ROOT)
app = FastAPI(title="省钱助手")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class JoinBody(BaseModel):
    name: str = Field(min_length=1)
    role: str
    code: str | None = None


class ReviewBody(BaseModel):
    approve: bool
    comment: str = ""


class ProfileBody(BaseModel):
    role: str
    requesterName: str
    approverName: str


class BudgetBody(BaseModel):
    amountCents: int = Field(gt=0)


def _file_url(request: Request, filename: str | None) -> str | None:
    if not filename:
        return None
    return str(request.base_url).rstrip("/") + "/api/files/" + filename


def _request_json(row, request: Request) -> dict:
    return {
        "id": row["id"],
        "itemName": row["item_name"],
        "category": row["category"],
        "unitPriceCents": row["unit_price_cents"],
        "quantity": row["quantity"],
        "reason": row["reason"],
        "requesterName": row["requester_name"],
        "status": row["status"],
        "createdAt": row["created_at"],
        "reviewedAt": row["reviewed_at"],
        "reviewerName": row["reviewer_name"],
        "reviewComment": row["review_comment"],
        "imagePath": _file_url(request, row["image_file"]),
    }


def _expense_json(row) -> dict:
    return {
        "id": row["id"],
        "requestId": row["request_id"],
        "itemName": row["item_name"],
        "category": row["category"],
        "amountCents": row["amount_cents"],
        "spentAt": row["spent_at"],
        "requesterName": row["requester_name"],
        "reviewerName": row["reviewer_name"],
    }


def current_member(authorization: str | None = Header(default=None)):
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(401, "需要登录")
    member = store.member_by_token(authorization.split(" ", 1)[1].strip())
    if member is None:
        raise HTTPException(401, "令牌无效")
    return member


@app.get("/api/health")
def health():
    return {"ok": True}


@app.post("/api/households")
def create_household(body: JoinBody):
    if body.role not in ("REQUESTER", "APPROVER"):
        raise HTTPException(400, "角色无效")
    return store.create_household(body.name.strip(), body.role)


@app.post("/api/households/join")
def join_household(body: JoinBody):
    if body.role not in ("REQUESTER", "APPROVER") or not body.code:
        raise HTTPException(400, "家庭码或角色无效")
    session = store.join_household(body.code, body.name.strip(), body.role)
    if session is None:
        raise HTTPException(404, "找不到这个家庭码")
    return session


@app.get("/api/session")
def read_session(member=Depends(current_member)):
    return store.session_payload(member["token"])


@app.put("/api/session")
def update_session(body: ProfileBody, member=Depends(current_member)):
    if body.role not in ("REQUESTER", "APPROVER"):
        raise HTTPException(400, "角色无效")
    store.update_names(member["household_id"], body.requesterName.strip() or "申请人", body.approverName.strip() or "审核人")
    store.update_role(member["id"], body.role)
    return store.session_payload(member["token"])


@app.get("/api/requests")
def list_requests(request: Request, member=Depends(current_member)):
    return [_request_json(row, request) for row in store.list_requests(member["household_id"])]


@app.get("/api/requests/{request_id}")
def get_request(request_id: int, request: Request, member=Depends(current_member)):
    row = store.get_request(member["household_id"], request_id)
    if row is None:
        raise HTTPException(404, "申请不存在")
    return _request_json(row, request)


@app.post("/api/requests")
async def create_request(
    request: Request,
    itemName: str = Form(),
    category: str = Form(),
    unitPriceCents: int = Form(),
    quantity: int = Form(),
    reason: str = Form(""),
    image: UploadFile | None = File(default=None),
    member=Depends(current_member),
):
    image_file = None
    if image is not None and image.filename:
        data = await image.read()
        if data:
            suffix = Path(image.filename).suffix or ".jpg"
            image_file = store.save_photo(data, suffix)
    requester = member["requester_name"] if member["role"] == "REQUESTER" else member["name"]
    new_id = store.insert_request(
        member["household_id"],
        {
            "item_name": itemName.strip(),
            "category": category,
            "unit_price_cents": unitPriceCents,
            "quantity": quantity,
            "reason": reason.strip(),
            "requester_name": requester,
            "created_at": int(time.time() * 1000),
            "image_file": image_file,
        },
    )
    row = store.get_request(member["household_id"], new_id)
    return _request_json(row, request)


@app.post("/api/requests/{request_id}/review")
def review_request(request_id: int, body: ReviewBody, request: Request, member=Depends(current_member)):
    reviewer = member["approver_name"] if member["role"] == "APPROVER" else member["name"]
    ok = store.review(
        member["household_id"],
        request_id,
        body.approve,
        reviewer,
        body.comment.strip(),
        int(time.time() * 1000),
    )
    if not ok:
        raise HTTPException(409, "这条申请不能审核")
    row = store.get_request(member["household_id"], request_id)
    return _request_json(row, request)


@app.delete("/api/requests/{request_id}")
def withdraw_request(request_id: int, member=Depends(current_member)):
    if not store.withdraw(member["household_id"], request_id):
        raise HTTPException(409, "只能撤回待审核的申请")
    return {"ok": True}


@app.get("/api/expenses")
def list_expenses(yearMonth: str, member=Depends(current_member)):
    year, month = [int(part) for part in yearMonth.split("-")]
    start = int(time.mktime(time.struct_time((year, month, 1, 0, 0, 0, 0, 0, -1)))) * 1000
    if month == 12:
        end_t = time.struct_time((year + 1, 1, 1, 0, 0, 0, 0, 0, -1))
    else:
        end_t = time.struct_time((year, month + 1, 1, 0, 0, 0, 0, 0, -1))
    end = int(time.mktime(end_t)) * 1000
    return [_expense_json(row) for row in store.list_expenses(member["household_id"], start, end)]


@app.get("/api/budget")
def read_budget(yearMonth: str, member=Depends(current_member)):
    row = store.get_budget(member["household_id"], yearMonth)
    if row is None:
        return None
    return {"yearMonth": row["year_month"], "amountCents": row["amount_cents"]}


@app.put("/api/budget")
def write_budget(yearMonth: str, body: BudgetBody, member=Depends(current_member)):
    store.set_budget(member["household_id"], yearMonth, body.amountCents)
    return {"yearMonth": yearMonth, "amountCents": body.amountCents}


@app.get("/api/files/{filename}")
def read_file(filename: str):
    path = store.photo_path(filename)
    if path is None:
        raise HTTPException(404, "没有这张图")
    return FileResponse(path)
