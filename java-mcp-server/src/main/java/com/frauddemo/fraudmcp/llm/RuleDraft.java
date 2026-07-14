package com.frauddemo.fraudmcp.llm;

/** Structured output schema the LLM must fill in for generate_rule_from_text. */
public class RuleDraft {

    private String name;
    private ConditionNodeDraft conditionJson;
    private String action;
    private String reasoning;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConditionNodeDraft getConditionJson() {
        return conditionJson;
    }

    public void setConditionJson(ConditionNodeDraft conditionJson) {
        this.conditionJson = conditionJson;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}
