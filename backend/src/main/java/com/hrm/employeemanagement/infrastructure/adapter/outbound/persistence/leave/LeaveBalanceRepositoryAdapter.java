package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave;

import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveBalancePort;
import com.hrm.employeemanagement.domain.leave.LeaveBalance;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.entity.EmployeeLeaveBalanceJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.repository.SpringDataLeaveBalanceRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class LeaveBalanceRepositoryAdapter implements LoadLeaveBalancePort, SaveLeaveBalancePort {

    private final SpringDataLeaveBalanceRepository repository;
    private final LeaveBalancePersistenceMapper mapper;

    public LeaveBalanceRepositoryAdapter(
            SpringDataLeaveBalanceRepository repository,
            LeaveBalancePersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year) {
        return repository.findByEmployeeIdAndYearNumber(employeeId, year).map(mapper::toDomain);
    }

    @Override
    public LeaveBalance save(LeaveBalance leaveBalance) {
        EmployeeLeaveBalanceJpaEntity entity = mapper.toJpaEntity(leaveBalance);
        EmployeeLeaveBalanceJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
