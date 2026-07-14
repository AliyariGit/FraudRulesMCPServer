import pytest

from models.schemas import FieldRef
from rules.legacy_migration import LegacyMigrationError, migrate_legacy_rule


def test_migrate_fraud_flag_rule():
    rule = migrate_legacy_rule("IF TX_AMT > 5000 AND CNTRY <> HOME_CNTRY SET FRAUD_FLAG=Y")
    assert rule.condition_dsl == "amount > 5000 AND country <> customer.country"
    assert rule.action == "HIGH_RISK"
    assert rule.status == "PENDING_APPROVAL"
    assert rule.source == "MIGRATED"
    assert rule.condition_json.all_of[1].value == FieldRef(ref="customer.country")
    assert rule.condition_json.all_of[1].operator == "!="


def test_migrate_decline_flag_rule():
    rule = migrate_legacy_rule("IF DEVICE_RISK = HIGH SET DECLINE_FLAG=Y")
    assert rule.action == "DECLINE"
    assert rule.condition_dsl == "deviceRisk = HIGH"


def test_migrate_review_flag_by_name_heuristic():
    rule = migrate_legacy_rule("IF CUST_AGE < 25 SET REVIEW_ALERT=Y")
    assert rule.action == "REVIEW"


def test_malformed_legacy_text_raises():
    with pytest.raises(LegacyMigrationError):
        migrate_legacy_rule("this is not a legacy rule")


def test_custom_name_used_when_provided():
    rule = migrate_legacy_rule(
        "IF TX_AMT > 100 SET FRAUD_FLAG=Y", name="Custom Migrated Name"
    )
    assert rule.name == "Custom Migrated Name"
