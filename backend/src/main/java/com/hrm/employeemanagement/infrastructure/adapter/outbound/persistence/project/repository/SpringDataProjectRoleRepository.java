package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT COUNT(e) FROM EmployeeJpaEntity e WHERE LOWER(e.professionalRole) = LOWER(:roleName) OR LOWER(e.professionalRole) = LOWER(:roleCode)")
    long countEmployeesByProfessionalRole(@Param("roleName") String roleName, @Param("roleCode") String roleCode);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE EmployeeJpaEntity e SET e.professionalRole = :newName WHERE LOWER(e.professionalRole) = LOWER(:oldName)")
    int syncEmployeeProfessionalRole(@Param("oldName") String oldName, @Param("newName") String newName);
}
