from __future__ import annotations

from models.schemas import ConditionTrace, EvaluationResult, Rule, RuleMatch
from rules.condition_tree import evaluate_condition

_FIELD_DESCRIPTIONS = {
    "amount": lambda actual, op, expected: f"Amount ({actual}) {op} threshold ({expected})",
    "deviceRisk": lambda actual, op, expected: f"Device risk was {actual}",
    "merchant": lambda actual, op, expected: f"Merchant category was {actual}",
    "country": lambda actual, op, expected: (
        f"Transaction country ({actual}) differs from customer's home country ({expected})"
        if op == "!="
        else f"Transaction country ({actual}) {op} {expected}"
    ),
    "customerAge": lambda actual, op, expected: f"Customer age ({actual}) {op} {expected}",
    "transactionId": lambda actual, op, expected: f"Transaction ID {op} {expected}",
    "customer.country": lambda actual, op, expected: f"Customer home country ({actual}) {op} {expected}",
}


def describe_matched_leaves(trace: ConditionTrace) -> list[str]:
    """Collects one human-readable description per matched leaf condition."""
    if trace.combinator is not None:
        descriptions: list[str] = []
        for child in trace.children or []:
            descriptions.extend(describe_matched_leaves(child))
        return descriptions
    if not trace.matched:
        return []
    describe = _FIELD_DESCRIPTIONS.get(trace.field, lambda a, o, e: f"{trace.field} {o} {e}")
    return [describe(trace.actual, trace.operator, trace.expected)]


def evaluate_rules(txn: dict, active_rules: list[Rule]) -> EvaluationResult:
    matches: list[RuleMatch] = []
    reasons: list[str] = []

    for rule in sorted(active_rules, key=lambda r: r.priority):
        matched, trace = evaluate_condition(rule.condition_json, txn)
        if not matched:
            continue
        matches.append(
            RuleMatch(
                rule_id=rule.id,
                rule_name=rule.name,
                action=rule.action,
                severity_weight=rule.severity_weight,
                trace=trace,
            )
        )
        reasons.extend(describe_matched_leaves(trace))

    risk_score = min(100, sum(m.severity_weight for m in matches))

    actions = {m.action for m in matches}
    if "DECLINE" in actions:
        decision = "DECLINE"
    elif actions & {"REVIEW", "ALERT"}:
        decision = "REVIEW"
    else:
        decision = "APPROVE"

    reason = "; ".join(dict.fromkeys(reasons)) if reasons else "No fraud rules matched."

    return EvaluationResult(
        transactionId=txn["transactionId"],
        decision=decision,
        riskScore=risk_score,
        matchedRules=[f"RULE-{m.rule_id:03d}" if m.rule_id else m.rule_name for m in matches],
        reason=reason,
        ruleTrace=matches,
    )
