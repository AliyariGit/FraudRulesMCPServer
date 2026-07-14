package com.frauddemo.fraudmcp.model;

import java.util.List;

public class EvaluationResult {

    private String transactionId;
    private String decision; // DECLINE | REVIEW | APPROVE
    private int riskScore;
    private List<String> matchedRules;
    private String reason;
    private List<RuleMatch> ruleTrace;

    public EvaluationResult() {
    }

    public EvaluationResult(String transactionId, String decision, int riskScore,
                             List<String> matchedRules, String reason, List<RuleMatch> ruleTrace) {
        this.transactionId = transactionId;
        this.decision = decision;
        this.riskScore = riskScore;
        this.matchedRules = matchedRules;
        this.reason = reason;
        this.ruleTrace = ruleTrace;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getDecision() {
        return decision;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public List<String> getMatchedRules() {
        return matchedRules;
    }

    public String getReason() {
        return reason;
    }

    public List<RuleMatch> getRuleTrace() {
        return ruleTrace;
    }
}
