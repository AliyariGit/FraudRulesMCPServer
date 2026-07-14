from __future__ import annotations

from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, Integer, String, Text, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class RuleORM(Base):
    __tablename__ = "rules"
    __table_args__ = (
        CheckConstraint("action IN ('DECLINE','REVIEW','ALERT','HIGH_RISK')", name="ck_rules_action"),
        CheckConstraint(
            "status IN ('ACTIVE','PENDING_APPROVAL','DISABLED')", name="ck_rules_status"
        ),
        CheckConstraint("source IN ('MANUAL','LLM_GENERATED','MIGRATED')", name="ck_rules_source"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    condition_dsl: Mapped[str | None] = mapped_column(Text, nullable=True)
    condition_json: Mapped[dict] = mapped_column(JSONB, nullable=False)
    action: Mapped[str] = mapped_column(String, nullable=False)
    severity_weight: Mapped[int] = mapped_column(Integer, nullable=False)
    priority: Mapped[int] = mapped_column(Integer, nullable=False, default=100)
    status: Mapped[str] = mapped_column(String, nullable=False, default="ACTIVE")
    source: Mapped[str] = mapped_column(String, nullable=False, default="MANUAL")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class EvaluationORM(Base):
    __tablename__ = "evaluations"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    transaction_id: Mapped[str] = mapped_column(String, index=True, nullable=False)
    payload: Mapped[dict] = mapped_column(JSONB, nullable=False)
    decision: Mapped[str] = mapped_column(String, nullable=False)
    risk_score: Mapped[int] = mapped_column(Integer, nullable=False)
    matched_rules: Mapped[list] = mapped_column(JSONB, nullable=False)
    reason: Mapped[str] = mapped_column(Text, nullable=False)
    rule_trace: Mapped[list] = mapped_column(JSONB, nullable=False)
    evaluated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
