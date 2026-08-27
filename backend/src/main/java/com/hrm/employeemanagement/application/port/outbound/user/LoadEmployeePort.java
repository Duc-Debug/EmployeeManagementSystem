package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.List;
import java.util.Optional;

public interface LoadEmployeePort {
    Optional<Employee> findByUserId(UserId userId);
    Optional<Employee> findById(EmployeeId id);
    boolean existsByEmployeeCode(String employeeCode);
    List<Employee> findAllByUserIdIn(List<UserId> userIds);
}
