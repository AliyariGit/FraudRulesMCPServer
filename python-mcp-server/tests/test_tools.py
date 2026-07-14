"""Integration tests for tools/* against a live Postgres instance (docker compose
up first, see docker-compose.yml). These exercise the same DB path the MCP
server and FastAPI wrapper use.
"""
import pytest

from tools.create_rule import create_rule
from tools.evaluate import evaluate_transaction
from tools.explain import explain_decision
from tools.migrate import migrate_legacy_rule
from tools.simulate import simulate_rule

SAMPLE_TXN = {
    "transactionId": "TX-TEST-0001",
    "amount": 8500,
    "country": "CA",
    "merchant": "CryptoExchange",
    "customerAge": 22,
    "deviceRisk": "HIGH",
    "customer": {"country": "US"},
}


def test_create_and_evaluate_and_explain_round_trip():
    created = create_rule(
        name="Test Rule TX-TEST",
        condition="deviceRisk == HIGH AND merchant == CryptoExchange",
        action="DECLINE",
    )
    assert created["status"] == "ACTIVE"
    assert created["id"] is not None

    result = evaluate_transaction(SAMPLE_TXN)
    assert result["decision"] == "DECLINE"
    assert result["riskScore"] > 0

    explanation = explain_decision("TX-TEST-0001")
    assert explanation["decision"] == "DECLINE"
    assert "declined" in explanation["explanation"]


def test_simulate_rule_reports_impact():
    report = simulate_rule(condition="amount > 5000", action="REVIEW", name="High Amount Sim")
    assert report["transactionsTested"] == 300
    assert report["blocked"] >= 0
    assert report["ruleName"] == "High Amount Sim"


def test_migrate_legacy_rule_persists_pending():
    saved = migrate_legacy_rule("IF TX_AMT > 9000 SET FRAUD_FLAG=Y")
    assert saved["status"] == "PENDING_APPROVAL"
    assert saved["source"] == "MIGRATED"


def test_explain_missing_transaction_raises():
    with pytest.raises(ValueError):
        explain_decision("TX-DOES-NOT-EXIST")
