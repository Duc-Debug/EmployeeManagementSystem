package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;

@Repository
public interface SpringDataProjectRoleRepository extends JpaRepository<ProjectRoleJpaEntity, Long> {

    Optional<ProjectRoleJpaEntity> findByCode(String code);

    List<ProjectRoleJpaEntity> findByStatus(String status);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT COUNT(d) FROM ProjectResourceDemandJpaEntity d WHERE d.roleId = :roleId")
    long countDemandsByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT COUNT(e) FROM EmployeeJpaEntity e WHERE e.professionalRole = :roleName")
    long countEmployeesByProfessionalRole(@Param("roleName") String roleName);
}
