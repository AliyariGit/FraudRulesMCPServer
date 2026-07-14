package com.frauddemo.fraudmcp.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Evaluates a ConditionNode tree against a transaction map, producing both the
 * boolean result and a per-leaf trace -- a direct port of
 * rules/condition_tree.py.
 */
public final class ConditionTreeEvaluator {

    private ConditionTreeEvaluator() {
    }

    public record Outcome(boolean matched, ConditionTrace trace) {
    }

    public static Object resolveField(Map<String, Object> txn, String dottedPath) {
        Object value = txn;
        for (String part : dottedPath.split("\\.")) {
            if (value instanceof Map<?, ?> map) {
                value = map.get(part);
            } else {
                value = null;
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Object resolveValue(Object value, Map<String, Object> txn) {
        if (value instanceof Map<?, ?> map && map.containsKey("ref")) {
            return resolveField(txn, (String) map.get("ref"));
        }
        return value;
    }

    public static Outcome evaluate(ConditionNode node, Map<String, Object> txn) {
        if (node.getAllOf() != null) {
            List<ConditionTrace> childTraces = new ArrayList<>();
            boolean matched = true;
            for (ConditionNode child : node.getAllOf()) {
                Outcome outcome = evaluate(child, txn);
                childTraces.add(outcome.trace());
                matched &= outcome.matched();
            }
            return new Outcome(matched, ConditionTrace.combinator("allOf", matched, childTraces));
        }

        if (node.getAnyOf() != null) {
            List<ConditionTrace> childTraces = new ArrayList<>();
            boolean matched = false;
            for (ConditionNode child : node.getAnyOf()) {
                Outcome outcome = evaluate(child, txn);
                childTraces.add(outcome.trace());
                matched |= outcome.matched();
            }
            return new Outcome(matched, ConditionTrace.combinator("anyOf", matched, childTraces));
        }

        Object actual = resolveField(txn, node.getField());
        Object expected = resolveValue(node.getValue(), txn);
        boolean matched = compare(node.getOperator(), actual, expected);
        return new Outcome(matched, ConditionTrace.leaf(node.getField(), node.getOperator(), expected, actual, matched));
    }

    private static boolean compare(String operator, Object actual, Object expected) {
        try {
            if (actual instanceof Number a && expected instanceof Number e) {
                return applyOrdering(operator, Double.compare(a.doubleValue(), e.doubleValue()));
            }
            if (actual instanceof String a && expected instanceof String e) {
                return applyOrdering(operator, a.compareTo(e));
            }
            if (actual instanceof Boolean a && expected instanceof Boolean e) {
                return applyOrdering(operator, Boolean.compare(a, e));
            }
            boolean equal = java.util.Objects.equals(actual, expected);
            return switch (operator) {
                case "==" -> equal;
                case "!=" -> !equal;
                default -> false; // ordering undefined for mismatched/null types
            };
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean applyOrdering(String operator, int cmp) {
        return switch (operator) {
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case "==" -> cmp == 0;
            case "!=" -> cmp != 0;
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }
}
