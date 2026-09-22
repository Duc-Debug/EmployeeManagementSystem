package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.unavailability;

import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.unavailability.entity.UnavailabilityDeclarationJpaEntity;

public class UnavailabilityDeclarationPersistenceMapper {

    public static UnavailabilityDeclaration toDomain(UnavailabilityDeclarationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UnavailabilityDeclaration(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getReasonType(),
                entity.getReasonDetail(),
                entity.getTotalHoursDeducted(),
                entity.getStatus(),
                entity.getApproverId(),
                entity.getApproverComment(),
                entity.getApprovedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public static UnavailabilityDeclarationJpaEntity toJpaEntity(UnavailabilityDeclaration domain) {
        if (domain == null) {
            return null;
        }
        UnavailabilityDeclarationJpaEntity entity = new UnavailabilityDeclarationJpaEntity();
        entity.setId(domain.getId());
        entity.setEmployeeId(domain.getEmployeeId());
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setReasonType(domain.getReasonType());
        entity.setReasonDetail(domain.getReasonDetail());
        entity.setTotalHoursDeducted(domain.getTotalHoursDeducted());
        entity.setStatus(domain.getStatus());
        entity.setApproverId(domain.getApproverId());
        entity.setApproverComment(domain.getApproverComment());
        entity.setApprovedAt(domain.getApprovedAt());
        entity.setVersion(domain.getVersion());
        return entity;
    }
}
