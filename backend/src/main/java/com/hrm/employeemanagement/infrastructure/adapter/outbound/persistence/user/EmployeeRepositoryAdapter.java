package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EmployeeRepositoryAdapter implements LoadEmployeePort, SaveEmployeePort {

    private final SpringDataEmployeeRepository springDataEmployeeRepository;
    private final UserPersistenceMapper mapper;

    public EmployeeRepositoryAdapter(SpringDataEmployeeRepository springDataEmployeeRepository, UserPersistenceMapper mapper) {
        this.springDataEmployeeRepository = springDataEmployeeRepository;
        this.mapper = mapper;
    }

    @Override
    public Employee save(Employee employee) {
        EmployeeJpaEntity saved;
        if (employee.getIdValue() != null) {
            EmployeeJpaEntity existing = springDataEmployeeRepository.findById(employee.getIdValue())
                    .orElseGet(() -> mapper.toJpaEntity(employee));
            mapper.updateJpaEntity(existing, employee);
            saved = springDataEmployeeRepository.save(existing);
        } else {
            EmployeeJpaEntity entity = mapper.toJpaEntity(employee);
            saved = springDataEmployeeRepository.save(entity);
        }
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Employee> findByUserId(UserId userId) {
        if (userId == null || userId.value() == null) return Optional.empty();
        return springDataEmployeeRepository.findByUserId(userId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Employee> findById(EmployeeId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return springDataEmployeeRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Employee> findAllByUserIdIn(List<UserId> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = userIds.stream().map(UserId::value).filter(java.util.Objects::nonNull).toList();
        return springDataEmployeeRepository.findByUserIdIn(ids).stream().map(mapper::toDomain).toList();
    }
}
