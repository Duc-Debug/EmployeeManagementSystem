package com.hrm.employeemanagement.application.port.outbound.user;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.user.UserId;

public interface LoadEmployeePort {
    Optional<Employee> findByUserId(UserId userId);
    Optional<Employee> findById(EmployeeId id);
    Optional<Employee> findByIdForUpdate(EmployeeId id);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCodeAndIdNot(String employeeCode, EmployeeId excludeId);
    List<Employee> findAllByUserIdIn(List<UserId> userIds);
    List<Employee> findAllByIdIn(List<EmployeeId> ids);
    List<Employee> findByOrgUnitId(Long orgUnitId);
    List<Employee> findActiveByOrgUnitId(Long orgUnitId);
    List<Employee> findAllPaged(int size, int offset);
    long countAll();
    List<Employee> findByOrgUnitBranch(Long scopeOrgUnitId, int size, int offset);
    long countByOrgUnitBranch(Long scopeOrgUnitId);
    List<Employee> findByProjectManager(Long pmEmployeeId, int size, int offset);
    long countByProjectManager(Long pmEmployeeId);
}
