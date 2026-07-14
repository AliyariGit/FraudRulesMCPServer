"""Deterministic migration of legacy COBOL-style fraud rule text into the modern
structured rule format. Regex + lookup tables only (no LLM call): migration
correctness for a fraud/compliance engine must be auditable and reproducible,
not probabilistic.

Example legacy input:
    IF TX_AMT > 5000 AND CNTRY <> HOME_CNTRY SET FRAUD_FLAG=Y
becomes the modern DSL:
    amount > 5000 AND country != customer.country
with action HIGH_RISK.
"""
from __future__ import annotations

import re

from models.schemas import DEFAULT_SEVERITY_WEIGHT, Action, ConditionNode, Rule
from rules.condition_dsl import ConditionParseError, parse_condition

FIELD_ALIASES = {
    "TX_AMT": "amount",
    "CNTRY": "country",
    "HOME_CNTRY": "customer.country",
    "DEVICE_RISK": "deviceRisk",
    "MERCH": "merchant",
    "CUST_AGE": "customerAge",
    "TXN_ID": "transactionId",
}

FLAG_ACTION_MAP: dict[str, Action] = {
    "FRAUD_FLAG": "HIGH_RISK",
    "DECLINE_FLAG": "DECLINE",
    "REVIEW_FLAG": "REVIEW",
    "ALERT_FLAG": "ALERT",
}

_LEGACY_RE = re.compile(
    r"^\s*IF\s+(?P<cond>.+?)\s+SET\s+(?P<flag>\w+)\s*=\s*(?P<value>\w+)\s*$",
    re.IGNORECASE,
)

_FIELD_TOKEN_RE = re.compile(
    r"\b(" + "|".join(re.escape(alias) for alias in FIELD_ALIASES) + r")\b"
)


class LegacyMigrationError(ValueError):
    pass


def _infer_action(flag_name: str) -> Action:
    if flag_name.upper() in FLAG_ACTION_MAP:
        return FLAG_ACTION_MAP[flag_name.upper()]
    upper = flag_name.upper()
    if "DECLINE" in upper:
        return "DECLINE"
    if "REVIEW" in upper:
        return "REVIEW"
    if "ALERT" in upper:
        return "ALERT"
    return "HIGH_RISK"


def translate_legacy_condition(legacy_cond: str) -> str:
    """Replaces legacy field tokens with their modern equivalents; legacy operators
    (`<>`, `=`) pass through unchanged since parse_condition already accepts them
    as aliases.
    """
    return _FIELD_TOKEN_RE.sub(lambda m: FIELD_ALIASES[m.group(1).upper()], legacy_cond)


def migrate_legacy_rule(legacy_text: str, name: str | None = None) -> Rule:
    match = _LEGACY_RE.match(legacy_text.strip())
    if not match:
        raise LegacyMigrationError(
            f"Legacy rule text did not match the expected "
            f"'IF <conditions> SET <FLAG>=<value>' shape: {legacy_text!r}"
        )

    modern_dsl = translate_legacy_condition(match.group("cond"))
    try:
        condition_json: ConditionNode = parse_condition(modern_dsl)
    except ConditionParseError as exc:
        raise LegacyMigrationError(f"Could not migrate condition: {exc}") from exc

    action = _infer_action(match.group("flag"))
    rule_name = name or f"Migrated Legacy Rule ({match.group('flag')})"

    return Rule(
        name=rule_name,
        condition_dsl=modern_dsl,
        condition_json=condition_json,
        action=action,
        severity_weight=DEFAULT_SEVERITY_WEIGHT[action],
        status="PENDING_APPROVAL",
        source="MIGRATED",
    )
