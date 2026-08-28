package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;

public interface SpringDataSkillRepository extends JpaRepository<SkillJpaEntity, Long> {

    Optional<SkillJpaEntity> findByCode(String code);

    boolean existsByCode(String code);
}
