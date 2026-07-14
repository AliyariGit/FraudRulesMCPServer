from __future__ import annotations

from typing import Optional

from mcp.server.fastmcp import FastMCP

from tools.approve_rule import approve_rule as _approve_rule
from tools.create_rule import create_rule as _create_rule
from tools.evaluate import evaluate_transaction as _evaluate_transaction
from tools.explain import explain_decision as _explain_decision
from tools.generate_rule import generate_rule_from_text as _generate_rule_from_text
from tools.migrate import migrate_legacy_rule as _migrate_legacy_rule
from tools.simulate import simulate_rule as _simulate_rule

mcp = FastMCP("fraud-rules-mcp-server")


@mcp.tool()
def evaluate_transaction(transaction: dict) -> dict:
    """Evaluate a transaction against all ACTIVE fraud rules and return a
    decision (DECLINE/REVIEW/APPROVE), risk score, matched rules, and reason."""
    return _evaluate_transaction(transaction)


@mcp.tool()
def create_rule(name: str, condition: str, action: str, priority: int = 100) -> dict:
    """Create and activate a fraud rule from a DSL condition string, e.g.
    'amount > 10000 AND country != CA'. action must be one of DECLINE, REVIEW,
    ALERT, HIGH_RISK."""
    return _create_rule(name, condition, action, priority)


@mcp.tool()
def generate_rule_from_text(instruction: str) -> dict:
    """Use an LLM to draft a fraud rule from a natural-language instruction
    (e.g. 'Block suspicious payments from new devices over $5000'). The rule is
    stored PENDING_APPROVAL until approve_rule is called on it."""
    return _generate_rule_from_text(instruction)


@mcp.tool()
def approve_rule(rule_id: int) -> dict:
    """Approve a PENDING_APPROVAL rule (from generate_rule_from_text or
    migrate_legacy_rule), activating it so it affects future evaluations."""
    return _approve_rule(rule_id)


@mcp.tool()
def explain_decision(transaction_id: str) -> dict:
    """Explain, for compliance/audit purposes, why a previously evaluated
    transaction received its decision."""
    return _explain_decision(transaction_id)


@mcp.tool()
def simulate_rule(condition: str, action: str, name: str = "Candidate Rule") -> dict:
    """Dry-run a candidate rule (not stored/active) against historical
    transactions and report how many it would block, false positives, and
    estimated fraud prevented -- before deploying it."""
    return _simulate_rule(condition, action, name)


@mcp.tool()
def migrate_legacy_rule(legacy_text: str, name: Optional[str] = None) -> dict:
    """Deterministically convert legacy COBOL-style rule text (e.g.
    'IF TX_AMT > 5000 AND CNTRY <> HOME_CNTRY SET FRAUD_FLAG=Y') into the
    modern rule format. Stored PENDING_APPROVAL pending human review."""
    return _migrate_legacy_rule(legacy_text, name)


if __name__ == "__main__":
    mcp.run()
