package com.hrm.employeemanagement.application.dto.unavailability;

import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UnavailabilityDeclarationResult(
        Long id,
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        UnavailabilityReasonType reasonType,
        String reasonDetail,
        BigDecimal totalHoursDeducted,
        UnavailabilityStatus status,
        Long approverId,
        String approverComment,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UnavailabilityDeclarationResult fromDomain(UnavailabilityDeclaration domain) {
        if (domain == null) {
            return null;
        }
        return new UnavailabilityDeclarationResult(
                domain.getId(),
                domain.getEmployeeId(),
                domain.getStartDate(),
                domain.getEndDate(),
                domain.getReasonType(),
                domain.getReasonDetail(),
                domain.getTotalHoursDeducted(),
                domain.getStatus(),
                domain.getApproverId(),
                domain.getApproverComment(),
                domain.getApprovedAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
