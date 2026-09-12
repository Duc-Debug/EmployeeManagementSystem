package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;

@Repository
public interface SpringDataProjectResourceDemandRepository
        extends JpaRepository<ProjectResourceDemandJpaEntity, Long> {

    List<ProjectResourceDemandJpaEntity> findByProjectId(Long projectId);

    List<ProjectResourceDemandJpaEntity> findByProjectIdAndRoleId(Long projectId, Long roleId);

    Optional<ProjectResourceDemandJpaEntity> findByProjectIdAndRoleIdAndYearAndWeekNumber(
            Long projectId, Long roleId, Integer year, Integer weekNumber);

    @Query("""
        SELECT d FROM ProjectResourceDemandJpaEntity d
        JOIN ProjectJpaEntity p ON p.id = d.projectId
        WHERE (:orgUnitId IS NULL OR p.orgUnitId = :orgUnitId)
          AND (:fromYear IS NULL OR d.year > :fromYear OR (d.year = :fromYear AND d.weekNumber >= :fromWeek))
          AND (:toYear IS NULL OR d.year < :toYear OR (d.year = :toYear AND d.weekNumber <= :toWeek))
    """)
    List<ProjectResourceDemandJpaEntity> findDemandsFiltered(
            @Param("orgUnitId") Long orgUnitId,
            @Param("fromYear") Integer fromYear,
            @Param("fromWeek") Integer fromWeek,
            @Param("toYear") Integer toYear,
            @Param("toWeek") Integer toWeek
    );

    @Query("""
        SELECT d FROM ProjectResourceDemandJpaEntity d
        JOIN ProjectJpaEntity p ON p.id = d.projectId
        WHERE (COALESCE(:orgUnitIds, NULL) IS NULL OR p.orgUnitId IN :orgUnitIds)
          AND (:fromYear IS NULL OR d.year > :fromYear OR (d.year = :fromYear AND d.weekNumber >= :fromWeek))
          AND (:toYear IS NULL OR d.year < :toYear OR (d.year = :toYear AND d.weekNumber <= :toWeek))
    """)
    List<ProjectResourceDemandJpaEntity> findDemandsFilteredByOrgUnitIds(
            @Param("orgUnitIds") List<Long> orgUnitIds,
            @Param("fromYear") Integer fromYear,
            @Param("fromWeek") Integer fromWeek,
            @Param("toYear") Integer toYear,
            @Param("toWeek") Integer toWeek
    );
}