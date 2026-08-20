package com.hrm.employeemanagement.domain.repository.user;

import com.hrm.employeemanagement.domain.model.employee.Employee;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findByUserId(Long userId);
    Optional<Employee> findById(Long id);
    List<Employee> findAllByUserIdIn(List<Long> userIds);
}
