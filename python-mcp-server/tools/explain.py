from __future__ import annotations

from database.repo import get_latest_evaluation


def explain_decision(transaction_id: str) -> dict:
    """Looks up the most recent persisted evaluation for a transaction and
    renders a human-readable, numbered explanation for compliance/audit review.
    """
    evaluation = get_latest_evaluation(transaction_id)
    if evaluation is None:
        raise ValueError(f"No evaluation found for transactionId {transaction_id!r}")

    reasons = [r for r in evaluation.reason.split("; ") if r] if evaluation.reason else []
    verb = {"DECLINE": "declined", "REVIEW": "flagged for review", "APPROVE": "approved"}[
        evaluation.decision
    ]

    explanation_lines = [f"Transaction {verb} because:"]
    explanation_lines += [f"{i}. {r}" for i, r in enumerate(reasons, start=1)]
    if not reasons:
        explanation_lines.append("No fraud rules matched.")

    return {
        "transactionId": evaluation.transaction_id,
        "decision": evaluation.decision,
        "riskScore": evaluation.risk_score,
        "matchedRules": evaluation.matched_rules,
        "explanation": "\n".join(explanation_lines),
        "ruleTrace": evaluation.rule_trace,
        "evaluatedAt": evaluation.evaluated_at.isoformat(),
    }
