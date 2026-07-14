package com.frauddemo.fraudmcp.model;

import com.frauddemo.fraudmcp.engine.ConditionTrace;

public class RuleMatch {

    private Long ruleId;
    private String ruleName;
    private String action;
    private int severityWeight;
    private ConditionTrace trace;

    public RuleMatch() {
    }

    public RuleMatch(Long ruleId, String ruleName, String action, int severityWeight, ConditionTrace trace) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.action = action;
        this.severityWeight = severityWeight;
        this.trace = trace;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getAction() {
        return action;
    }

    public int getSeverityWeight() {
        return severityWeight;
    }

    public ConditionTrace getTrace() {
        return trace;
    }
}
