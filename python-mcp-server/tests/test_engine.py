from models.schemas import DEFAULT_SEVERITY_WEIGHT, Rule
from rules.condition_dsl import parse_condition
from rules.engine import evaluate_rules


def make_rule(id, name, dsl, action, priority=100):
    return Rule(
        id=id,
        name=name,
        condition_dsl=dsl,
        condition_json=parse_condition(dsl),
        action=action,
        severity_weight=DEFAULT_SEVERITY_WEIGHT[action],
        priority=priority,
    )


RULES = [
    make_rule(1, "High International Transfer", "amount > 5000 AND country != customer.country", "HIGH_RISK"),
    make_rule(2, "Crypto High Device Risk", "deviceRisk == HIGH AND merchant == CryptoExchange", "DECLINE"),
    make_rule(3, "Young Customer Large Amount", "customerAge < 25 AND amount > 10000", "REVIEW"),
]


def test_decline_transaction_matches_multiple_rules():
    txn = {
        "transactionId": "TX12345",
        "amount": 8500,
        "country": "CA",
        "merchant": "CryptoExchange",
        "customerAge": 22,
        "deviceRisk": "HIGH",
        "customer": {"country": "US"},
    }
    result = evaluate_rules(txn, RULES)
    assert result.decision == "DECLINE"
    assert result.riskScore == 80  # 20 (HIGH_RISK) + 60 (DECLINE)
    assert "RULE-001" in result.matchedRules
    assert "RULE-002" in result.matchedRules
    assert "RULE-003" not in result.matchedRules
    assert "Device risk was HIGH" in result.reason


def test_clean_transaction_is_approved():
    txn = {
        "transactionId": "TX99999",
        "amount": 50,
        "country": "CA",
        "merchant": "GroceryStore",
        "customerAge": 40,
        "deviceRisk": "LOW",
        "customer": {"country": "CA"},
    }
    result = evaluate_rules(txn, RULES)
    assert result.decision == "APPROVE"
    assert result.riskScore == 0
    assert result.matchedRules == []


def test_review_only_transaction():
    txn = {
        "transactionId": "TX55555",
        "amount": 15000,
        "country": "CA",
        "merchant": "Electronics",
        "customerAge": 20,
        "deviceRisk": "LOW",
        "customer": {"country": "CA"},
    }
    result = evaluate_rules(txn, RULES)
    assert result.decision == "REVIEW"
    assert result.riskScore == 30
    assert result.matchedRules == ["RULE-003"]


def test_risk_score_capped_at_100():
    rules = RULES + [
        make_rule(4, "Extra Decline A", "amount > 1", "DECLINE"),
        make_rule(5, "Extra Decline B", "amount > 1", "DECLINE"),
    ]
    txn = {
        "transactionId": "TX1",
        "amount": 8500,
        "country": "CA",
        "merchant": "CryptoExchange",
        "customerAge": 22,
        "deviceRisk": "HIGH",
        "customer": {"country": "US"},
    }
    result = evaluate_rules(txn, rules)
    assert result.riskScore == 100
