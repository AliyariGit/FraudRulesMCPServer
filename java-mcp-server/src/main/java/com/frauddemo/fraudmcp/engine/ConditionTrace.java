package com.frauddemo.fraudmcp.engine;

import java.util.List;

/** Per-node evaluation trace so explain_decision can say exactly which
 * sub-conditions fired. */
public class ConditionTrace {

    private String field;
    private String operator;
    private Object expected;
    private Object actual;
    private boolean matched;
    private String combinator; // "allOf" | "anyOf" | null for a leaf
    private List<ConditionTrace> children;

    public ConditionTrace() {
    }

    public static ConditionTrace leaf(String field, String operator, Object expected, Object actual, boolean matched) {
        ConditionTrace trace = new ConditionTrace();
        trace.field = field;
        trace.operator = operator;
        trace.expected = expected;
        trace.actual = actual;
        trace.matched = matched;
        return trace;
    }

    public static ConditionTrace combinator(String combinator, boolean matched, List<ConditionTrace> children) {
        ConditionTrace trace = new ConditionTrace();
        trace.combinator = combinator;
        trace.matched = matched;
        trace.children = children;
        return trace;
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator;
    }

    public Object getExpected() {
        return expected;
    }

    public Object getActual() {
        return actual;
    }

    public boolean isMatched() {
        return matched;
    }

    public String getCombinator() {
        return combinator;
    }

    public List<ConditionTrace> getChildren() {
        return children;
    }
}
