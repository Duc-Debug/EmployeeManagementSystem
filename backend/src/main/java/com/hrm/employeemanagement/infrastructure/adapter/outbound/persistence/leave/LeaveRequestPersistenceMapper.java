package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave;

import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveRequestPolicy;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.LeaveRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestPersistenceMapper {

    public LeaveRequest toDomain(LeaveRequestJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        int daysCount = LeaveRequestPolicy.calculateWorkingDays(entity.getStartDate(), entity.getEndDate());
        LeaveType type = entity.getLeaveType() != null ? LeaveType.valueOf(entity.getLeaveType()) : LeaveType.ANNUAL;
        LeaveStatus status = entity.getStatus() != null ? LeaveStatus.valueOf(entity.getStatus()) : LeaveStatus.PENDING;

        return new LeaveRequest(
                entity.getId(),
                entity.getEmployeeId(),
                type,
                entity.getStartDate(),
                entity.getEndDate(),
                daysCount,
                entity.getHoursDeducted(),
                entity.getReason(),
                status,
                entity.getApproverId(),
                entity.getApproverComment(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public LeaveRequestJpaEntity toJpaEntity(LeaveRequest domain) {
        if (domain == null) {
            return null;
        }

        return new LeaveRequestJpaEntity(
                domain.getId(),
                domain.getEmployeeId(),
                domain.getLeaveType() != null ? domain.getLeaveType().name() : LeaveType.ANNUAL.name(),
                domain.getStartDate(),
                domain.getEndDate(),
                domain.getStatus() != null ? domain.getStatus().name() : LeaveStatus.PENDING.name(),
                domain.getHoursDeducted(),
                domain.getReason(),
                domain.getApproverId(),
                domain.getApproverComment(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
