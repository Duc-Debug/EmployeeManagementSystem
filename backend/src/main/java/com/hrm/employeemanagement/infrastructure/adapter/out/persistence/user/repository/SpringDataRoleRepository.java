package com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository;

import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, Long> {
    Optional<RoleJpaEntity> findByCode(String code);
}
