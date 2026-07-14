from __future__ import annotations

from datetime import datetime
from typing import Any, Literal, Union

from pydantic import BaseModel, model_validator

Action = Literal["DECLINE", "REVIEW", "ALERT", "HIGH_RISK"]
RuleStatus = Literal["ACTIVE", "PENDING_APPROVAL", "DISABLED"]
RuleSource = Literal["MANUAL", "LLM_GENERATED", "MIGRATED"]
Operator = Literal[">", "<", ">=", "<=", "==", "!="]

DEFAULT_SEVERITY_WEIGHT: dict[Action, int] = {
    "DECLINE": 60,
    "REVIEW": 30,
    "ALERT": 30,
    "HIGH_RISK": 20,
}


class FieldRef(BaseModel):
    ref: str


class ConditionNode(BaseModel):
    """Either a combinator (all_of/any_of over child nodes) or a leaf comparison."""

    all_of: list["ConditionNode"] | None = None
    any_of: list["ConditionNode"] | None = None
    field: str | None = None
    operator: Operator | None = None
    value: Union[int, float, str, bool, FieldRef, None] = None

    @model_validator(mode="after")
    def _check_shape(self) -> "ConditionNode":
        is_combinator = self.all_of is not None or self.any_of is not None
        is_leaf = self.field is not None and self.operator is not None
        if is_combinator == is_leaf:
            raise ValueError(
                "ConditionNode must be exactly one of: a combinator (all_of/any_of) "
                "or a leaf (field+operator+value)"
            )
        if is_combinator and self.all_of is not None and self.any_of is not None:
            raise ValueError("ConditionNode cannot set both all_of and any_of")
        return self


class ConditionTrace(BaseModel):
    field: str | None = None
    operator: str | None = None
    expected: Any = None
    actual: Any = None
    matched: bool
    combinator: Literal["all_of", "any_of"] | None = None
    children: list["ConditionTrace"] | None = None


class CustomerInfo(BaseModel):
    country: str | None = None


class Transaction(BaseModel):
    transactionId: str
    amount: float
    country: str
    merchant: str
    customerAge: int
    deviceRisk: str
    customer: CustomerInfo | None = None


class Rule(BaseModel):
    id: int | None = None
    name: str
    condition_dsl: str | None = None
    condition_json: ConditionNode
    action: Action
    severity_weight: int
    priority: int = 100
    status: RuleStatus = "ACTIVE"
    source: RuleSource = "MANUAL"
    created_at: datetime | None = None
    updated_at: datetime | None = None


class RuleMatch(BaseModel):
    rule_id: int
    rule_name: str
    action: Action
    severity_weight: int
    trace: ConditionTrace


class EvaluationResult(BaseModel):
    transactionId: str
    decision: Literal["DECLINE", "REVIEW", "APPROVE"]
    riskScore: int
    matchedRules: list[str]
    reason: str
    ruleTrace: list[RuleMatch]


class SimulationReport(BaseModel):
    transactionsTested: int
    blocked: int
    falsePositives: int
    estimatedFraudPrevented: float


class RuleDraft(BaseModel):
    """Structured output schema the LLM must fill in for generate_rule_from_text."""

    name: str
    condition_json: ConditionNode
    action: Action
    reasoning: str
