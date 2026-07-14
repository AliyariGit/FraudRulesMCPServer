from __future__ import annotations

from database.repo import list_active_rules, save_evaluation
from models.schemas import Transaction
from rules.engine import evaluate_rules


def evaluate_transaction(transaction: dict) -> dict:
    """Evaluates a transaction against all ACTIVE fraud rules and returns the
    decision, risk score, matched rules, and a human-readable reason. Persists
    the result so explain_decision can look it up later.
    """
    txn_model = Transaction.model_validate(transaction)
    txn_dict = txn_model.model_dump()

    active_rules = list_active_rules()
    result = evaluate_rules(txn_dict, active_rules)
    save_evaluation(result, txn_dict)

    return result.model_dump(mode="json")
