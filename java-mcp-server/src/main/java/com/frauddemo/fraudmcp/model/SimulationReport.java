package com.frauddemo.fraudmcp.model;

public class SimulationReport {

    private int transactionsTested;
    private int blocked;
    private int falsePositives;
    private double estimatedFraudPrevented;
    private String ruleName;
    private String condition;
    private String action;

    public SimulationReport() {
    }

    public SimulationReport(int transactionsTested, int blocked, int falsePositives,
                             double estimatedFraudPrevented, String ruleName, String condition, String action) {
        this.transactionsTested = transactionsTested;
        this.blocked = blocked;
        this.falsePositives = falsePositives;
        this.estimatedFraudPrevented = estimatedFraudPrevented;
        this.ruleName = ruleName;
        this.condition = condition;
        this.action = action;
    }

    public int getTransactionsTested() {
        return transactionsTested;
    }

    public int getBlocked() {
        return blocked;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public double getEstimatedFraudPrevented() {
        return estimatedFraudPrevented;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getCondition() {
        return condition;
    }

    public String getAction() {
        return action;
    }
}
