package com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user;

import com.hrm.employeemanagement.domain.model.employee.Employee;
import com.hrm.employeemanagement.domain.repository.user.EmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataEmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final SpringDataEmployeeRepository springDataEmployeeRepository;

    public EmployeeRepositoryAdapter(SpringDataEmployeeRepository springDataEmployeeRepository) {
        this.springDataEmployeeRepository = springDataEmployeeRepository;
    }

    @Override
    public Employee save(Employee employee) {
        EmployeeJpaEntity entity = new EmployeeJpaEntity(
                employee.getId(),
                employee.getUserId(),
                employee.getDepartmentId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getIsOutsourced(),
                employee.getStandardHoursPerWeek(),
                employee.getStatus()
        );
        EmployeeJpaEntity saved = springDataEmployeeRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<Employee> findByUserId(Long userId) {
        return springDataEmployeeRepository.findByUserId(userId).map(this::mapToDomain);
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return springDataEmployeeRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<Employee> findAllByUserIdIn(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return springDataEmployeeRepository.findByUserIdIn(userIds).stream().map(this::mapToDomain).toList();
    }

    private Employee mapToDomain(EmployeeJpaEntity entity) {
        return new Employee(
                entity.getId(),
                entity.getUserId(),
                entity.getDepartmentId(),
                entity.getEmployeeCode(),
                entity.getFullName(),
                entity.getIsOutsourced(),
                entity.getStandardHoursPerWeek(),
                entity.getStatus()
        );
    }
}
