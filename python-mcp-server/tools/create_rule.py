from __future__ import annotations

from database.repo import save_rule
from models.schemas import DEFAULT_SEVERITY_WEIGHT, Action, Rule
from rules.condition_dsl import parse_condition


def create_rule(name: str, condition: str, action: Action, priority: int = 100) -> dict:
    """Stores a manually-authored rule directly as ACTIVE. `condition` is the
    human-friendly DSL string, e.g. "amount > 10000 AND country != CA".
    """
    condition_json = parse_condition(condition)
    rule = Rule(
        name=name,
        condition_dsl=condition,
        condition_json=condition_json,
        action=action,
        severity_weight=DEFAULT_SEVERITY_WEIGHT[action],
        priority=priority,
        status="ACTIVE",
        source="MANUAL",
    )
    saved = save_rule(rule)
    return saved.model_dump(mode="json")
