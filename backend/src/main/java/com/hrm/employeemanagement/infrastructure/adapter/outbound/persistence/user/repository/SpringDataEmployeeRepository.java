package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface SpringDataEmployeeRepository extends JpaRepository<EmployeeJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmployeeJpaEntity e WHERE e.id = :id")
    Optional<EmployeeJpaEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<EmployeeJpaEntity> findByUserId(Long userId);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);
    List<EmployeeJpaEntity> findByUserIdIn(List<Long> userIds);
    List<EmployeeJpaEntity> findByOrgUnitId(Long orgUnitId);
    List<EmployeeJpaEntity> findByOrgUnitIdAndStatus(Long orgUnitId, String status);
}
