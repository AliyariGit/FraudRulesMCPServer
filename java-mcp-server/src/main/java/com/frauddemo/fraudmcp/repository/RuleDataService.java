package com.frauddemo.fraudmcp.repository;

import com.frauddemo.fraudmcp.model.EvaluationEntity;
import com.frauddemo.fraudmcp.model.EvaluationResult;
import com.frauddemo.fraudmcp.model.Rule;
import com.frauddemo.fraudmcp.model.RuleEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared DB access used by the MCP tools -- keeps Spring Data JPA out of the
 * tool methods and gives both the MCP and REST entry points one place to
 * read/write rules and evaluations from. Mirrors database/repo.py.
 */
@Service
@Transactional
public class RuleDataService {

    private final RuleJpaRepository ruleRepository;
    private final EvaluationJpaRepository evaluationRepository;

    public RuleDataService(RuleJpaRepository ruleRepository, EvaluationJpaRepository evaluationRepository) {
        this.ruleRepository = ruleRepository;
        this.evaluationRepository = evaluationRepository;
    }

    private static Rule toRule(RuleEntity entity) {
        Rule rule = new Rule();
        rule.setId(entity.getId());
        rule.setName(entity.getName());
        rule.setConditionDsl(entity.getConditionDsl());
        rule.setConditionJson(entity.getConditionJson());
        rule.setAction(entity.getAction());
        rule.setSeverityWeight(entity.getSeverityWeight());
        rule.setPriority(entity.getPriority());
        rule.setStatus(entity.getStatus());
        rule.setSource(entity.getSource());
        rule.setCreatedAt(entity.getCreatedAt());
        rule.setUpdatedAt(entity.getUpdatedAt());
        return rule;
    }

    @Transactional(readOnly = true)
    public List<Rule> listActiveRules() {
        return ruleRepository.findByStatus("ACTIVE").stream().map(RuleDataService::toRule).toList();
    }

    @Transactional(readOnly = true)
    public List<Rule> listAllRules() {
        return ruleRepository.findAll().stream().map(RuleDataService::toRule).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Rule> getRule(long ruleId) {
        return ruleRepository.findById(ruleId).map(RuleDataService::toRule);
    }

    public Rule saveRule(Rule rule) {
        RuleEntity entity = new RuleEntity();
        entity.setName(rule.getName());
        entity.setConditionDsl(rule.getConditionDsl());
        entity.setConditionJson(rule.getConditionJson());
        entity.setAction(rule.getAction());
        entity.setSeverityWeight(rule.getSeverityWeight());
        entity.setPriority(rule.getPriority());
        entity.setStatus(rule.getStatus());
        entity.setSource(rule.getSource());
        return toRule(ruleRepository.save(entity));
    }

    public Optional<Rule> setRuleStatus(long ruleId, String status) {
        return ruleRepository.findById(ruleId).map(entity -> {
            entity.setStatus(status);
            return toRule(ruleRepository.save(entity));
        });
    }

    public void saveEvaluation(EvaluationResult result, Map<String, Object> payload) {
        EvaluationEntity entity = new EvaluationEntity();
        entity.setTransactionId(result.getTransactionId());
        entity.setPayload(payload);
        entity.setDecision(result.getDecision());
        entity.setRiskScore(result.getRiskScore());
        entity.setMatchedRules(result.getMatchedRules());
        entity.setReason(result.getReason());
        entity.setRuleTrace(result.getRuleTrace());
        evaluationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<EvaluationEntity> getLatestEvaluation(String transactionId) {
        return evaluationRepository
                .findByTransactionIdOrderByEvaluatedAtDesc(transactionId, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    @Transactional(readOnly = true)
    public List<EvaluationEntity> listRecentEvaluations(int limit) {
        return evaluationRepository.findAllByOrderByEvaluatedAtDesc(PageRequest.of(0, limit, Sort.unsorted()));
    }
}
