"""Evaluates a ConditionNode tree against a transaction dict, producing both the
boolean result and a per-leaf trace so explain_decision can say exactly which
sub-conditions fired.
"""
from __future__ import annotations

import operator as _op
from typing import Any

from models.schemas import ConditionNode, ConditionTrace, FieldRef

_OPS = {
    ">": _op.gt,
    "<": _op.lt,
    ">=": _op.ge,
    "<=": _op.le,
    "==": _op.eq,
    "!=": _op.ne,
}


def resolve_field(txn: dict, dotted_path: str) -> Any:
    value: Any = txn
    for part in dotted_path.split("."):
        if isinstance(value, dict):
            value = value.get(part)
        else:
            value = getattr(value, part, None)
    return value


def _resolve_value(value: Any, txn: dict) -> Any:
    if isinstance(value, FieldRef):
        return resolve_field(txn, value.ref)
    if isinstance(value, dict) and "ref" in value:
        return resolve_field(txn, value["ref"])
    return value


def evaluate_condition(node: ConditionNode, txn: dict) -> tuple[bool, ConditionTrace]:
    if node.all_of is not None:
        child_results = [evaluate_condition(child, txn) for child in node.all_of]
        matched = all(result for result, _ in child_results)
        trace = ConditionTrace(
            matched=matched, combinator="all_of", children=[t for _, t in child_results]
        )
        return matched, trace

    if node.any_of is not None:
        child_results = [evaluate_condition(child, txn) for child in node.any_of]
        matched = any(result for result, _ in child_results)
        trace = ConditionTrace(
            matched=matched, combinator="any_of", children=[t for _, t in child_results]
        )
        return matched, trace

    actual = resolve_field(txn, node.field)
    expected = _resolve_value(node.value, txn)
    try:
        matched = _OPS[node.operator](actual, expected)
    except TypeError:
        matched = False
    trace = ConditionTrace(
        field=node.field,
        operator=node.operator,
        expected=expected,
        actual=actual,
        matched=matched,
    )
    return matched, trace
