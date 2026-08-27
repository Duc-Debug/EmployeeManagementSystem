package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataEmployeeRepository extends JpaRepository<EmployeeJpaEntity, Long> {
    Optional<EmployeeJpaEntity> findByUserId(Long userId);
    boolean existsByEmployeeCode(String employeeCode);
    List<EmployeeJpaEntity> findByUserIdIn(List<Long> userIds);
}
