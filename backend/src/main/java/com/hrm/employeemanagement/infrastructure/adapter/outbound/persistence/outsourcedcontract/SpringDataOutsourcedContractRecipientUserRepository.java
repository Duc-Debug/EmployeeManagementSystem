package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.outsourcedcontract;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;

@Repository
public interface SpringDataOutsourcedContractRecipientUserRepository extends JpaRepository<UserJpaEntity, Long> {

    @Query("SELECT u.id FROM UserJpaEntity u WHERE u.role.code IN ('VT-03', 'VT-05') AND u.isActive = true")
    List<Long> findResourceManagersAndHrUserIds();
}
