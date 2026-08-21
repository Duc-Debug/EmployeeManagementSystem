package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.application.port.outbound.user.LoadDepartmentPort;
import com.hrm.employeemanagement.domain.department.Department;
import com.hrm.employeemanagement.domain.department.DepartmentId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.DepartmentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataDepartmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Persistence Adapter implementing LoadDepartmentPort.
 */
@Component
public class DepartmentRepositoryAdapter implements LoadDepartmentPort {

    private final SpringDataDepartmentRepository springDataDepartmentRepository;

    public DepartmentRepositoryAdapter(SpringDataDepartmentRepository springDataDepartmentRepository) {
        this.springDataDepartmentRepository = springDataDepartmentRepository;
    }

    @Override
    public Optional<Department> findById(DepartmentId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return springDataDepartmentRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Department> findByCode(String code) {
        return springDataDepartmentRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public List<Department> findAllByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return springDataDepartmentRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    private Department toDomain(DepartmentJpaEntity entity) {
        return new Department(
                entity.getId() != null ? new DepartmentId(entity.getId()) : null,
                entity.getCode(),
                entity.getName(),
                entity.getParentId()
        );
    }
}
