from __future__ import annotations

from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
from pydantic import BaseModel

from dashboard import render_dashboard
from database.repo import list_all_rules, list_recent_evaluations
from models.schemas import Action
from tools.approve_rule import approve_rule
from tools.create_rule import create_rule
from tools.evaluate import evaluate_transaction
from tools.explain import explain_decision
from tools.generate_rule import generate_rule_from_text
from tools.migrate import migrate_legacy_rule
from tools.simulate import simulate_rule

app = FastAPI(
    title="Fraud Rules API",
    description="Thin HTTP facade over the same tools/* logic the MCP server exposes.",
)


class CreateRuleRequest(BaseModel):
    name: str
    condition: str
    action: Action
    priority: int = 100


class GenerateRuleRequest(BaseModel):
    instruction: str


class SimulateRuleRequest(BaseModel):
    condition: str
    action: Action
    name: str = "Candidate Rule"


class MigrateRuleRequest(BaseModel):
    legacy_text: str
    name: str | None = None


@app.get("/", response_class=HTMLResponse)
def dashboard():
    rules = list_all_rules()
    evaluations = [
        {
            "transaction_id": e.transaction_id,
            "decision": e.decision,
            "risk_score": e.risk_score,
            "matched_rules": e.matched_rules,
            "reason": e.reason,
            "evaluated_at": e.evaluated_at.isoformat(),
        }
        for e in list_recent_evaluations(limit=50)
    ]
    return render_dashboard(rules, evaluations)


@app.post("/evaluate")
def evaluate(transaction: dict):
    try:
        return evaluate_transaction(transaction)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.get("/rules")
def rules():
    return [r.model_dump(mode="json") for r in list_all_rules()]


@app.post("/rules")
def create(req: CreateRuleRequest):
    try:
        return create_rule(req.name, req.condition, req.action, req.priority)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/rules/generate")
def generate(req: GenerateRuleRequest):
    try:
        return generate_rule_from_text(req.instruction)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/rules/{rule_id}/approve")
def approve(rule_id: int):
    try:
        return approve_rule(rule_id)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.get("/explain/{transaction_id}")
def explain(transaction_id: str):
    try:
        return explain_decision(transaction_id)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.post("/simulate")
def simulate(req: SimulateRuleRequest):
    try:
        return simulate_rule(req.condition, req.action, req.name)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/migrate")
def migrate(req: MigrateRuleRequest):
    try:
        return migrate_legacy_rule(req.legacy_text, req.name)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
