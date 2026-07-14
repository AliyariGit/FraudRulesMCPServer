package com.frauddemo.fraudmcp.model;

import java.time.OffsetDateTime;
import java.util.List;

public class ExplanationResult {

    private String transactionId;
    private String decision;
    private int riskScore;
    private List<String> matchedRules;
    private String explanation;
    private List<RuleMatch> ruleTrace;
    private OffsetDateTime evaluatedAt;

    public ExplanationResult(String transactionId, String decision, int riskScore, List<String> matchedRules,
                              String explanation, List<RuleMatch> ruleTrace, OffsetDateTime evaluatedAt) {
        this.transactionId = transactionId;
        this.decision = decision;
        this.riskScore = riskScore;
        this.matchedRules = matchedRules;
        this.explanation = explanation;
        this.ruleTrace = ruleTrace;
        this.evaluatedAt = evaluatedAt;
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

    public String getExplanation() {
        return explanation;
    }

    public List<RuleMatch> getRuleTrace() {
        return ruleTrace;
    }

    public OffsetDateTime getEvaluatedAt() {
        return evaluatedAt;
    }
}
