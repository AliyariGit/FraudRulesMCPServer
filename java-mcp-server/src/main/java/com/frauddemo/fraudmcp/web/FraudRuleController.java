package com.frauddemo.fraudmcp.web;

import com.frauddemo.fraudmcp.mcp.FraudRuleTools;
import com.frauddemo.fraudmcp.model.EvaluationResult;
import com.frauddemo.fraudmcp.model.ExplanationResult;
import com.frauddemo.fraudmcp.model.Rule;
import com.frauddemo.fraudmcp.model.SimulationReport;
import com.frauddemo.fraudmcp.model.Transaction;
import com.frauddemo.fraudmcp.repository.RuleDataService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thin HTTP facade over FraudRuleTools -- mirrors python-mcp-server/api.py.
 * No duplicated business logic: every endpoint just calls into the same
 * tool methods the MCP stdio server exposes.
 */
@RestController
public class FraudRuleController {

    private final FraudRuleTools tools;
    private final RuleDataService ruleDataService;

    public FraudRuleController(FraudRuleTools tools, RuleDataService ruleDataService) {
        this.tools = tools;
        this.ruleDataService = ruleDataService;
    }

    @PostMapping("/evaluate")
    public EvaluationResult evaluate(@RequestBody Transaction transaction) {
        try {
            return tools.evaluateTransaction(transaction);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/rules")
    public List<Rule> rules() {
        return ruleDataService.listAllRules();
    }

    @PostMapping("/rules")
    public Rule createRule(@RequestBody CreateRuleRequest req) {
        try {
            return tools.createRule(req.name(), req.condition(), req.action(), req.priority());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/rules/generate")
    public Rule generateRule(@RequestBody GenerateRuleRequest req) {
        try {
            return tools.generateRuleFromText(req.instruction());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/rules/{ruleId}/approve")
    public Rule approve(@PathVariable long ruleId) {
        try {
            return tools.approveRule(ruleId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @GetMapping("/explain/{transactionId}")
    public ExplanationResult explain(@PathVariable String transactionId) {
        try {
            return tools.explainDecision(transactionId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/simulate")
    public SimulationReport simulate(@RequestBody SimulateRuleRequest req) {
        try {
            return tools.simulateRule(req.condition(), req.action(), req.name() != null ? req.name() : "Candidate Rule");
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/migrate")
    public Rule migrate(@RequestBody MigrateRuleRequest req) {
        try {
            return tools.migrateLegacyRule(req.legacyText(), req.name());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
