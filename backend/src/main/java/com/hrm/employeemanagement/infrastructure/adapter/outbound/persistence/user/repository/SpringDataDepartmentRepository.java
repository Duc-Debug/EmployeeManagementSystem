package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.DepartmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataDepartmentRepository extends JpaRepository<DepartmentJpaEntity, Long> {
    Optional<DepartmentJpaEntity> findByCode(String code);
}
