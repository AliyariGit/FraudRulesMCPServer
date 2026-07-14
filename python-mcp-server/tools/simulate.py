from __future__ import annotations

import json
import pathlib

from models.schemas import Action, SimulationReport
from rules.condition_dsl import parse_condition
from rules.condition_tree import evaluate_condition

DATA_FILE = pathlib.Path(__file__).resolve().parent.parent / "data" / "synthetic_transactions.json"


def simulate_rule(condition: str, action: Action, name: str = "Candidate Rule") -> dict:
    """Dry-runs a candidate rule (not yet stored/active) against the bundled
    synthetic historical transaction dataset and reports its real-world impact
    before it's ever deployed.
    """
    condition_json = parse_condition(condition)
    transactions = json.loads(DATA_FILE.read_text())

    blocked = 0
    false_positives = 0
    fraud_prevented = 0.0

    for txn in transactions:
        matched, _ = evaluate_condition(condition_json, txn)
        if matched:
            blocked += 1
            if txn["is_fraud"]:
                fraud_prevented += txn["amount"]
            else:
                false_positives += 1

    report = SimulationReport(
        transactionsTested=len(transactions),
        blocked=blocked,
        falsePositives=false_positives,
        estimatedFraudPrevented=round(fraud_prevented, 2),
    )
    result = report.model_dump(mode="json")
    result["ruleName"] = name
    result["condition"] = condition
    result["action"] = action
    return result
