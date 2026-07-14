package com.frauddemo.fraudmcp.repository;

import com.frauddemo.fraudmcp.model.RuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleJpaRepository extends JpaRepository<RuleEntity, Long> {
    List<RuleEntity> findByStatus(String status);
}
