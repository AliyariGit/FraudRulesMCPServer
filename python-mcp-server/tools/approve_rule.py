from __future__ import annotations

from database.repo import get_rule, set_rule_status


def approve_rule(rule_id: int) -> dict:
    """Flips a PENDING_APPROVAL rule (from generate_rule_from_text or
    migrate_legacy_rule) to ACTIVE. This is the human-in-the-loop governance
    gate before an AI-authored or migrated rule can affect real decisions.
    """
    existing = get_rule(rule_id)
    if existing is None:
        raise ValueError(f"No rule found with id {rule_id}")
    if existing.status != "PENDING_APPROVAL":
        raise ValueError(
            f"Rule {rule_id} is {existing.status}, not PENDING_APPROVAL; nothing to approve"
        )

    updated = set_rule_status(rule_id, "ACTIVE")
    return updated.model_dump(mode="json")
