package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillGroupJpaEntity;
import java.util.List;
import java.util.Optional;

public interface SpringDataSkillGroupRepository extends JpaRepository<SkillGroupJpaEntity, Long> {
    Optional<SkillGroupJpaEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<SkillGroupJpaEntity> findByStatus(String status);
}
