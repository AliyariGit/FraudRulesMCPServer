package com.frauddemo.fraudmcp.llm;

import com.frauddemo.fraudmcp.engine.ConditionNode;

/**
 * Strict, explicitly-typed leaf value used only for the LLM structured-output
 * schema (generate_rule_from_text): every field must have a concrete JSON
 * type for the Anthropic API's schema validation to accept it -- a plain
 * {@code Object} field (as the runtime ConditionNode uses) generates a
 * type-less schema node the API rejects.
 */
public class ConditionValueDraft {

    private Double numberValue;
    private String stringValue;
    private Boolean boolValue;
    private String refValue;

    public Object toRuntimeValue() {
        if (refValue != null) {
            return ConditionNode.fieldRef(refValue);
        }
        if (numberValue != null) {
            return numberValue;
        }
        if (boolValue != null) {
            return boolValue;
        }
        return stringValue;
    }

    public Double getNumberValue() {
        return numberValue;
    }

    public void setNumberValue(Double numberValue) {
        this.numberValue = numberValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Boolean getBoolValue() {
        return boolValue;
    }

    public void setBoolValue(Boolean boolValue) {
        this.boolValue = boolValue;
    }

    public String getRefValue() {
        return refValue;
    }

    public void setRefValue(String refValue) {
        this.refValue = refValue;
    }
}
