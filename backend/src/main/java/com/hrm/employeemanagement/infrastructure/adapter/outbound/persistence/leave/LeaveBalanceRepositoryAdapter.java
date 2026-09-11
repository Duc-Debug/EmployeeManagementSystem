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
        Optional<EmployeeLeaveBalanceJpaEntity> existing = repository.findByEmployeeIdAndYearNumber(employeeId, year);
        if (existing.isPresent()) {
            return mapper.toDomain(existing.get());
        }

        EmployeeLeaveBalanceJpaEntity entity = mapper.toJpaEntity(LeaveBalance.createDefault(employeeId, year));
        try {
            EmployeeLeaveBalanceJpaEntity saved = repository.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Trường hợp race condition: transaction song song đã insert trước
            return repository.findByEmployeeIdAndYearNumber(employeeId, year)
                    .map(mapper::toDomain)
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    public LeaveBalance save(LeaveBalance leaveBalance) {
        EmployeeLeaveBalanceJpaEntity entity = mapper.toJpaEntity(leaveBalance);
        EmployeeLeaveBalanceJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
