package com.frauddemo.fraudmcp.engine;

import java.util.List;
import java.util.Map;

/**
 * Either a combinator (allOf/anyOf over child nodes) or a leaf comparison.
 * Never eval()'d: evaluation only ever walks this fixed shape. {@code value}
 * is a literal (Number/String/Boolean) or, after JSON deserialization, a
 * {@code Map} of the form {"ref": "dotted.field.path"} pointing at another
 * field on the same transaction.
 */
public class ConditionNode {

    private List<ConditionNode> allOf;
    private List<ConditionNode> anyOf;
    private String field;
    private String operator;
    private Object value;

    public ConditionNode() {
    }

    public static ConditionNode leaf(String field, String operator, Object value) {
        ConditionNode node = new ConditionNode();
        node.field = field;
        node.operator = operator;
        node.value = value;
        return node;
    }

    public static ConditionNode allOf(List<ConditionNode> children) {
        ConditionNode node = new ConditionNode();
        node.allOf = children;
        return node;
    }

    public static ConditionNode anyOf(List<ConditionNode> children) {
        ConditionNode node = new ConditionNode();
        node.anyOf = children;
        return node;
    }

    /** Builds the {"ref": dottedPath} value shape used to compare two transaction fields. */
    public static Map<String, Object> fieldRef(String dottedPath) {
        return Map.of("ref", dottedPath);
    }

    public List<ConditionNode> getAllOf() {
        return allOf;
    }

    public void setAllOf(List<ConditionNode> allOf) {
        this.allOf = allOf;
    }

    public List<ConditionNode> getAnyOf() {
        return anyOf;
    }

    public void setAnyOf(List<ConditionNode> anyOf) {
        this.anyOf = anyOf;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
