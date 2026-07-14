package com.frauddemo.fraudmcp.model;

import com.frauddemo.fraudmcp.engine.ConditionNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** JPA entity for the {@code rules} table -- mirrors models/db_models.py's RuleORM. */
@Entity
@Table(name = "rules")
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "condition_dsl")
    private String conditionDsl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_json", nullable = false, columnDefinition = "jsonb")
    private ConditionNode conditionJson;

    @Column(nullable = false)
    private String action;

    @Column(name = "severity_weight", nullable = false)
    private int severityWeight;

    @Column(nullable = false)
    private int priority = 100;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private String source = "MANUAL";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
