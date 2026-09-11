package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave;

import com.hrm.employeemanagement.domain.leave.LeaveBalance;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.entity.EmployeeLeaveBalanceJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class LeaveBalancePersistenceMapper {

    public LeaveBalance toDomain(EmployeeLeaveBalanceJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LeaveBalance(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getYearNumber(),
                entity.getEntitledDays(),
                entity.getCarriedOverDays()
        );
    }

    public EmployeeLeaveBalanceJpaEntity toJpaEntity(LeaveBalance domain) {
        if (domain == null) {
            return null;
        }
        return new EmployeeLeaveBalanceJpaEntity(
                domain.getId(),
                domain.getEmployeeId(),
                domain.getYearNumber(),
                domain.getEntitledDays(),
                domain.getCarriedOverDays()
        );
    }
}
