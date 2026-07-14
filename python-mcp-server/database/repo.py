"""Shared DB access helpers used by tools/*, keeping SQLAlchemy queries out of the
tool functions and giving both the MCP server and the FastAPI wrapper one place
to read/write rules and evaluations from.
"""
from __future__ import annotations

from sqlalchemy import select

from database.session import get_session
from models.db_models import EvaluationORM, RuleORM
from models.schemas import EvaluationResult, Rule


def _row_to_rule(row: RuleORM) -> Rule:
    return Rule.model_validate(row, from_attributes=True)


def list_active_rules() -> list[Rule]:
    with get_session() as session:
        rows = session.execute(select(RuleORM).where(RuleORM.status == "ACTIVE")).scalars().all()
        return [_row_to_rule(r) for r in rows]


def list_all_rules() -> list[Rule]:
    with get_session() as session:
        rows = session.execute(select(RuleORM)).scalars().all()
        return [_row_to_rule(r) for r in rows]


def get_rule(rule_id: int) -> Rule | None:
    with get_session() as session:
        row = session.get(RuleORM, rule_id)
        return _row_to_rule(row) if row else None


def save_rule(rule: Rule) -> Rule:
    with get_session() as session:
        row = RuleORM(
            name=rule.name,
            condition_dsl=rule.condition_dsl,
            condition_json=rule.condition_json.model_dump(exclude_none=True),
            action=rule.action,
            severity_weight=rule.severity_weight,
            priority=rule.priority,
            status=rule.status,
            source=rule.source,
        )
        session.add(row)
        session.commit()
        session.refresh(row)
        return _row_to_rule(row)


def set_rule_status(rule_id: int, status: str) -> Rule | None:
    with get_session() as session:
        row = session.get(RuleORM, rule_id)
        if row is None:
            return None
        row.status = status
        session.commit()
        session.refresh(row)
        return _row_to_rule(row)


def save_evaluation(result: EvaluationResult, payload: dict) -> None:
    with get_session() as session:
        session.add(
            EvaluationORM(
                transaction_id=result.transactionId,
                payload=payload,
                decision=result.decision,
                risk_score=result.riskScore,
                matched_rules=result.matchedRules,
                reason=result.reason,
                rule_trace=[m.model_dump() for m in result.ruleTrace],
            )
        )
        session.commit()


def list_recent_evaluations(limit: int = 50) -> list[EvaluationORM]:
    with get_session() as session:
        rows = (
            session.execute(
                select(EvaluationORM).order_by(EvaluationORM.evaluated_at.desc()).limit(limit)
            )
            .scalars()
            .all()
        )
        for row in rows:
            session.expunge(row)
        return list(rows)


def get_latest_evaluation(transaction_id: str) -> EvaluationORM | None:
    with get_session() as session:
        row = (
            session.execute(
                select(EvaluationORM)
                .where(EvaluationORM.transaction_id == transaction_id)
                .order_by(EvaluationORM.evaluated_at.desc())
            )
            .scalars()
            .first()
        )
        if row is None:
            return None
        session.expunge(row)
        return row
