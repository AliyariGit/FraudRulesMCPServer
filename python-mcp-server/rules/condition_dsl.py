"""Compiles the human-friendly rule DSL (e.g. "amount > 10000 AND country != CA")
into a structured ConditionNode tree. No eval() involved: a fixed field whitelist,
a fixed operator set, and simple string splitting/regex are the entire parser.
"""
from __future__ import annotations

import re

from models.schemas import ConditionNode

ALLOWED_FIELDS = {
    "amount",
    "country",
    "merchant",
    "customerAge",
    "deviceRisk",
    "transactionId",
    "customer.country",
}

_OPERATOR_ALIASES = {
    "=": "==",
    "<>": "!=",
}
_OPERATORS = [">=", "<=", "!=", "==", "<>", ">", "<", "="]

_LEAF_RE = re.compile(
    r"^\s*([\w.]+)\s*(" + "|".join(re.escape(op) for op in _OPERATORS) + r")\s*(.+?)\s*$"
)


class ConditionParseError(ValueError):
    pass


def _split_top_level(text: str, keyword: str) -> list[str]:
    return re.split(rf"\s+{keyword}\s+", text, flags=re.IGNORECASE)


def _coerce_value(raw: str):
    raw = raw.strip()
    if len(raw) >= 2 and raw[0] == raw[-1] and raw[0] in ("'", '"'):
        return raw[1:-1]
    if raw.upper() in ("TRUE", "FALSE"):
        return raw.upper() == "TRUE"
    try:
        return int(raw)
    except ValueError:
        pass
    try:
        return float(raw)
    except ValueError:
        pass
    return raw


def _parse_leaf(text: str) -> ConditionNode:
    match = _LEAF_RE.match(text)
    if not match:
        raise ConditionParseError(f"Could not parse condition clause: {text!r}")
    field, operator, raw_value = match.groups()
    if field not in ALLOWED_FIELDS:
        raise ConditionParseError(
            f"Unknown field {field!r}; allowed fields: {sorted(ALLOWED_FIELDS)}"
        )
    operator = _OPERATOR_ALIASES.get(operator, operator)

    value_token = raw_value.strip()
    if value_token in ALLOWED_FIELDS:
        value: object = {"ref": value_token}
    else:
        value = _coerce_value(value_token)

    return ConditionNode(field=field, operator=operator, value=value)


def parse_condition(dsl: str) -> ConditionNode:
    """Parses "A > 1 AND B != 2 OR C == 3" as (A>1 AND B!=2) OR (C==3):
    OR has lower precedence than AND, matching standard boolean logic. No parens in v1.
    """
    if not dsl or not dsl.strip():
        raise ConditionParseError("Condition string is empty")

    or_groups = _split_top_level(dsl, "OR")
    parsed_groups: list[ConditionNode] = []
    for group in or_groups:
        and_clauses = _split_top_level(group, "AND")
        leaves = [_parse_leaf(clause) for clause in and_clauses]
        if len(leaves) == 1:
            parsed_groups.append(leaves[0])
        else:
            parsed_groups.append(ConditionNode(all_of=leaves))

    if len(parsed_groups) == 1:
        return parsed_groups[0]
    return ConditionNode(any_of=parsed_groups)
