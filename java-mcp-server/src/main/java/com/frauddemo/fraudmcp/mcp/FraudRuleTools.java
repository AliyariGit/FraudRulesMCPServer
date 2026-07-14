package com.frauddemo.fraudmcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddemo.fraudmcp.engine.ConditionDslParser;
import com.frauddemo.fraudmcp.engine.RuleEngine;
import com.frauddemo.fraudmcp.engine.SimulationService;
import com.frauddemo.fraudmcp.llm.AnthropicRuleGenerator;
import com.frauddemo.fraudmcp.llm.RuleDraft;
import com.frauddemo.fraudmcp.migration.LegacyRuleMigrator;
import com.frauddemo.fraudmcp.model.EvaluationEntity;
import com.frauddemo.fraudmcp.model.EvaluationResult;
import com.frauddemo.fraudmcp.model.ExplanationResult;
import com.frauddemo.fraudmcp.model.Rule;
import com.frauddemo.fraudmcp.model.RuleActions;
import com.frauddemo.fraudmcp.model.SimulationReport;
import com.frauddemo.fraudmcp.model.Transaction;
import com.frauddemo.fraudmcp.repository.RuleDataService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * The 7 fraud-rule MCP tools, mirroring python-mcp-server/tools/*.py one file
 * per method. Both the MCP stdio server (stdio profile) and the REST
 * controller (web profile) call straight into these methods -- no duplicated
 * business logic between the two entry points.
 */
@Component
public class FraudRuleTools {

    private final RuleDataService ruleDataService;
    private final SimulationService simulationService;
    private final AnthropicRuleGenerator ruleGenerator;
    private final ObjectMapper objectMapper;

    public FraudRuleTools(RuleDataService ruleDataService, SimulationService simulationService,
                           AnthropicRuleGenerator ruleGenerator, ObjectMapper objectMapper) {
        this.ruleDataService = ruleDataService;
        this.simulationService = simulationService;
        this.ruleGenerator = ruleGenerator;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "evaluate_transaction", description = "Evaluate a transaction against all "
            + "ACTIVE fraud rules and return a decision (DECLINE/REVIEW/APPROVE), risk score, "
            + "matched rules, and reason.")
    public EvaluationResult evaluateTransaction(
            @McpToolParam(description = "The transaction to evaluate", required = true) Transaction transaction) {
        Map<String, Object> txn = transaction.toMap(objectMapper);
        List<Rule> activeRules = ruleDataService.listActiveRules();
        EvaluationResult result = RuleEngine.evaluateRules(txn, activeRules);
        ruleDataService.saveEvaluation(result, txn);
        return result;
    }

    @McpTool(name = "create_rule", description = "Create and activate a fraud rule from a DSL "
            + "condition string, e.g. 'amount > 10000 AND country != CA'. action must be one of "
            + "DECLINE, REVIEW, ALERT, HIGH_RISK.")
    public Rule createRule(
            @McpToolParam(description = "Human-readable rule name", required = true) String name,
            @McpToolParam(description = "DSL condition string", required = true) String condition,
            @McpToolParam(description = "One of DECLINE, REVIEW, ALERT, HIGH_RISK", required = true) String action,
            @McpToolParam(description = "Evaluation order; lower runs first (default 100)", required = false) Integer priority) {
        if (!RuleActions.ALL.contains(action)) {
            throw new IllegalArgumentException("Unknown action '" + action + "'; expected one of " + RuleActions.ALL);
        }
        Rule rule = new Rule();
        rule.setName(name);
        rule.setConditionDsl(condition);
        rule.setConditionJson(ConditionDslParser.parse(condition));
        rule.setAction(action);
        rule.setSeverityWeight(RuleActions.defaultSeverityWeight(action));
        rule.setPriority(priority != null ? priority : 100);
        rule.setStatus("ACTIVE");
        rule.setSource("MANUAL");
        return ruleDataService.saveRule(rule);
    }

    @McpTool(name = "generate_rule_from_text", description = "Use an LLM to draft a fraud rule "
            + "from a natural-language instruction (e.g. 'Block suspicious payments from new "
            + "devices over $5000'). The rule is stored PENDING_APPROVAL until approve_rule is "
            + "called on it.")
    public Rule generateRuleFromText(
            @McpToolParam(description = "Natural-language rule instruction", required = true) String instruction) {
        RuleDraft draft = ruleGenerator.generate(instruction);

        Rule rule = new Rule();
        rule.setName(draft.getName());
        rule.setConditionJson(draft.getConditionJson().toConditionNode());
        rule.setAction(draft.getAction());
        rule.setSeverityWeight(RuleActions.defaultSeverityWeight(draft.getAction()));
        rule.setStatus("PENDING_APPROVAL");
        rule.setSource("LLM_GENERATED");
        return ruleDataService.saveRule(rule);
    }

    @McpTool(name = "approve_rule", description = "Approve a PENDING_APPROVAL rule (from "
            + "generate_rule_from_text or migrate_legacy_rule), activating it so it affects "
            + "future evaluations.")
    public Rule approveRule(
            @McpToolParam(description = "ID of the rule to approve", required = true) long ruleId) {
        Rule existing = ruleDataService.getRule(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("No rule found with id " + ruleId));
        if (!"PENDING_APPROVAL".equals(existing.getStatus())) {
            throw new IllegalArgumentException(
                    "Rule " + ruleId + " is " + existing.getStatus() + ", not PENDING_APPROVAL; nothing to approve");
        }
        return ruleDataService.setRuleStatus(ruleId, "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("Rule " + ruleId + " disappeared during approval"));
    }

    @McpTool(name = "explain_decision", description = "Explain, for compliance/audit purposes, "
            + "why a previously evaluated transaction received its decision.")
    public ExplanationResult explainDecision(
            @McpToolParam(description = "The transactionId to look up", required = true) String transactionId) {
        EvaluationEntity evaluation = ruleDataService.getLatestEvaluation(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("No evaluation found for transactionId '" + transactionId + "'"));

        List<String> reasons = Arrays.stream(evaluation.getReason().split("; "))
                .filter(r -> !r.isBlank())
                .toList();
        String verb = switch (evaluation.getDecision()) {
            case "DECLINE" -> "declined";
            case "REVIEW" -> "flagged for review";
            default -> "approved";
        };

        StringBuilder explanation = new StringBuilder("Transaction ").append(verb).append(" because:");
        for (int i = 0; i < reasons.size(); i++) {
            explanation.append('\n').append(i + 1).append(". ").append(reasons.get(i));
        }

        return new ExplanationResult(
                evaluation.getTransactionId(), evaluation.getDecision(), evaluation.getRiskScore(),
                evaluation.getMatchedRules(), explanation.toString(), evaluation.getRuleTrace(), evaluation.getEvaluatedAt()
        );
    }

    @McpTool(name = "simulate_rule", description = "Dry-run a candidate rule (not stored/active) "
            + "against historical transactions and report how many it would block, false "
            + "positives, and estimated fraud prevented -- before deploying it.")
    public SimulationReport simulateRule(
            @McpToolParam(description = "DSL condition string to test", required = true) String condition,
            @McpToolParam(description = "One of DECLINE, REVIEW, ALERT, HIGH_RISK", required = true) String action,
            @McpToolParam(description = "Display name for the simulation report", required = false) String name) {
        return simulationService.simulate(condition, action, name != null ? name : "Candidate Rule");
    }

    @McpTool(name = "migrate_legacy_rule", description = "Deterministically convert legacy "
            + "COBOL-style rule text (e.g. 'IF TX_AMT > 5000 AND CNTRY <> HOME_CNTRY SET "
            + "FRAUD_FLAG=Y') into the modern rule format. Stored PENDING_APPROVAL pending "
            + "human review.")
    public Rule migrateLegacyRule(
            @McpToolParam(description = "Legacy rule text", required = true) String legacyText,
            @McpToolParam(description = "Optional name for the migrated rule", required = false) String name) {
        Rule rule = LegacyRuleMigrator.migrate(legacyText, name);
        return ruleDataService.saveRule(rule);
    }
}
