package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

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
            saved = springDataEmployeeRepository.saveAndFlush(existing);
        } else {
            EmployeeJpaEntity entity = mapper.toJpaEntity(employee);
            saved = springDataEmployeeRepository.saveAndFlush(entity);
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
    public Optional<Employee> findByIdForUpdate(EmployeeId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return springDataEmployeeRepository.findByIdForUpdate(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmployeeCode(String employeeCode) {
        return springDataEmployeeRepository.existsByEmployeeCode(employeeCode);
    }

    @Override
    public boolean existsByEmployeeCodeAndIdNot(String employeeCode, EmployeeId excludeId) {
        if (employeeCode == null || employeeCode.isBlank() || excludeId == null || excludeId.value() == null) {
            return false;
        }
        return springDataEmployeeRepository.existsByEmployeeCodeAndIdNot(employeeCode.trim(), excludeId.value());
    }

    @Override
    public List<Employee> findAllByUserIdIn(List<UserId> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = userIds.stream().map(UserId::value).filter(java.util.Objects::nonNull).toList();
        return springDataEmployeeRepository.findByUserIdIn(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Employee> findAllByIdIn(List<EmployeeId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> rawIds = ids.stream().map(EmployeeId::value).filter(java.util.Objects::nonNull).toList();
        return springDataEmployeeRepository.findAllById(rawIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Employee> findByOrgUnitId(Long orgUnitId) {
        if (orgUnitId == null) {
            return List.of();
        }
        return springDataEmployeeRepository.findByOrgUnitId(orgUnitId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Employee> findActiveByOrgUnitId(Long orgUnitId) {
        if (orgUnitId == null) {
            return List.of();
        }
        return springDataEmployeeRepository.findByOrgUnitIdAndStatus(orgUnitId, EmployeeStatus.ACTIVE.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Employee> findAllPaged(int size, int offset) {
        return springDataEmployeeRepository.findAllPaged(size, offset).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return springDataEmployeeRepository.count();
    }

    @Override
    public List<Employee> findByOrgUnitBranch(Long scopeOrgUnitId, int size, int offset) {
        if (scopeOrgUnitId == null) {
            return List.of();
        }
        return springDataEmployeeRepository.findByOrgUnitBranch(scopeOrgUnitId, size, offset).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByOrgUnitBranch(Long scopeOrgUnitId) {
        if (scopeOrgUnitId == null) {
            return 0L;
        }
        return springDataEmployeeRepository.countByOrgUnitBranch(scopeOrgUnitId);
    }

    @Override
    public List<Employee> findByProjectManager(Long pmEmployeeId, int size, int offset) {
        if (pmEmployeeId == null) {
            return List.of();
        }
        return springDataEmployeeRepository.findByProjectManager(pmEmployeeId, size, offset).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByProjectManager(Long pmEmployeeId) {
        if (pmEmployeeId == null) {
            return 0L;
        }
        return springDataEmployeeRepository.countByProjectManager(pmEmployeeId);
    }

    @Override
    public List<Employee> findAllActive() {
        return springDataEmployeeRepository.findByStatus(com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
