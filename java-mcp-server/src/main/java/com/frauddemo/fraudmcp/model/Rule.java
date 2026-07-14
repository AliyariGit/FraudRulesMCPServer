package com.frauddemo.fraudmcp.model;

import com.frauddemo.fraudmcp.engine.ConditionNode;
import java.time.OffsetDateTime;

/** Domain representation of a fraud rule -- mirrors models/schemas.py's Rule. */
public class Rule {

    private Long id;
    private String name;
    private String conditionDsl;
    private ConditionNode conditionJson;
    private String action;
    private int severityWeight;
    private int priority = 100;
    private String status = "ACTIVE";
    private String source = "MANUAL";
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConditionDsl() {
        return conditionDsl;
    }

    public void setConditionDsl(String conditionDsl) {
        this.conditionDsl = conditionDsl;
    }

    public ConditionNode getConditionJson() {
        return conditionJson;
    }

    public void setConditionJson(ConditionNode conditionJson) {
        this.conditionJson = conditionJson;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getSeverityWeight() {
        return severityWeight;
    }

    public void setSeverityWeight(int severityWeight) {
        this.severityWeight = severityWeight;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
