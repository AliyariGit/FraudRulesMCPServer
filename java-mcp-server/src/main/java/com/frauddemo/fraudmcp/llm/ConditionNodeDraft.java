package com.frauddemo.fraudmcp.llm;

import com.frauddemo.fraudmcp.engine.ConditionNode;
import java.util.List;

public class ConditionNodeDraft {

    private List<ConditionNodeDraft> allOf;
    private List<ConditionNodeDraft> anyOf;
    private String field;
    private String operator;
    private ConditionValueDraft value;

    public ConditionNode toConditionNode() {
        if (allOf != null) {
            return ConditionNode.allOf(allOf.stream().map(ConditionNodeDraft::toConditionNode).toList());
        }
        if (anyOf != null) {
            return ConditionNode.anyOf(anyOf.stream().map(ConditionNodeDraft::toConditionNode).toList());
        }
        return ConditionNode.leaf(field, operator, value.toRuntimeValue());
    }

    public List<ConditionNodeDraft> getAllOf() {
        return allOf;
    }

    public void setAllOf(List<ConditionNodeDraft> allOf) {
        this.allOf = allOf;
    }

    public List<ConditionNodeDraft> getAnyOf() {
        return anyOf;
    }

    public void setAnyOf(List<ConditionNodeDraft> anyOf) {
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

    public ConditionValueDraft getValue() {
        return value;
    }

    public void setValue(ConditionValueDraft value) {
        this.value = value;
    }
}
