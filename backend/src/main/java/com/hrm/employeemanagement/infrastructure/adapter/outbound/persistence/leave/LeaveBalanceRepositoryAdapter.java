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
    public LeaveBalance findOrCreateDefault(Long employeeId, int year) {
        // MySQL upsert is atomic under the unique (employee_id, year_number) constraint.
        repository.insertDefaultIfAbsent(employeeId, year, new java.math.BigDecimal("12.0"), java.math.BigDecimal.ZERO);
        return repository.findByEmployeeIdAndYearNumber(employeeId, year)
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "Leave balance was not found after creating the default balance"
                ));
    }

    @Override
    public LeaveBalance findOrCreateDefaultWithLock(Long employeeId, int year) {
        // Đảm bảo bản ghi tồn tại bằng MySQL atomic upsert trước khi lấy pessimistic lock (SELECT FOR UPDATE).
        repository.insertDefaultIfAbsent(employeeId, year, new java.math.BigDecimal("12.0"), java.math.BigDecimal.ZERO);
        return repository.findByEmployeeIdAndYearNumberWithLock(employeeId, year)
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "Leave balance was not found after creating the default balance"
                ));
    }

    @Override
    public LeaveBalance save(LeaveBalance leaveBalance) {
        EmployeeLeaveBalanceJpaEntity entity = mapper.toJpaEntity(leaveBalance);
        EmployeeLeaveBalanceJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
