from __future__ import annotations

from database.repo import save_rule
from rules.legacy_migration import migrate_legacy_rule as _migrate_legacy_rule


def migrate_legacy_rule(legacy_text: str, name: str | None = None) -> dict:
    """Deterministically converts legacy COBOL-style rule text (e.g.
    "IF TX_AMT > 5000 AND CNTRY <> HOME_CNTRY SET FRAUD_FLAG=Y") into the modern
    structured rule format and stores it as PENDING_APPROVAL for human review.
    """
    rule = _migrate_legacy_rule(legacy_text, name=name)
    saved = save_rule(rule)
    return saved.model_dump(mode="json")
