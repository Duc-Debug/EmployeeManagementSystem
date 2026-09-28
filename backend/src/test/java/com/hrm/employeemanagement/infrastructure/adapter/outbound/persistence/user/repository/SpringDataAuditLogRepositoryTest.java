package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpringDataAuditLogRepositoryTest {

    @Autowired
    private SpringDataAuditLogRepository auditLogRepository;

    @Test
    @DisplayName("audit_logs lưu và đọc được old_value, new_value")
    void testSaveAndLoad_PreservesOldAndNewValues() {
        AuditLogJpaEntity saved = auditLogRepository.saveAndFlush(
                new AuditLogJpaEntity(
                        null,
                        null,
                        "UPDATE_AUTHORIZATION",
                        "users",
                        25L,
                        LocalDateTime.now(),
                        "role=VT-04;dataScope=SELF;scopeOrgUnitId=null",
                        "role=VT-02;dataScope=ORGANIZATION_BRANCH;scopeOrgUnitId=5"
                )
        );

        AuditLogJpaEntity loaded = auditLogRepository
                .findById(saved.getId())
                .orElseThrow();

        assertEquals(
                saved.getOldValue(),
                loaded.getOldValue()
        );

        assertEquals(
                saved.getNewValue(),
                loaded.getNewValue()
        );
    }
}
