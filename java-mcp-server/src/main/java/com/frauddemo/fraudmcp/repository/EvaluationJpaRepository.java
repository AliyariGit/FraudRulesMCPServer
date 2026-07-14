package com.frauddemo.fraudmcp.repository;

import com.frauddemo.fraudmcp.model.EvaluationEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationJpaRepository extends JpaRepository<EvaluationEntity, Long> {
    List<EvaluationEntity> findByTransactionIdOrderByEvaluatedAtDesc(String transactionId, Pageable pageable);

    List<EvaluationEntity> findAllByOrderByEvaluatedAtDesc(Pageable pageable);
}
