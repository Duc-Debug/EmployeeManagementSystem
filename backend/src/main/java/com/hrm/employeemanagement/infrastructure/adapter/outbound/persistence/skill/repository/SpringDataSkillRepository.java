package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import java.util.List;
import java.util.Optional;

public interface SpringDataSkillRepository extends JpaRepository<SkillJpaEntity, Long> {
    Optional<SkillJpaEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    long countByGroupIdAndStatus(Long groupId, String status);

    @Query("""
        SELECT s FROM SkillJpaEntity s
        WHERE (:groupId IS NULL OR s.groupId = :groupId)
          AND (:status IS NULL OR s.status = :status)
          AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY s.id DESC
    """)
    List<SkillJpaEntity> searchSkills(
            @Param("groupId") Long groupId,
            @Param("status") String status,
            @Param("keyword") String keyword
    );
}
