package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.department.Department;
import com.hrm.employeemanagement.domain.department.DepartmentId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound Port for loading Department aggregate domain data.
 */
public interface LoadDepartmentPort {

    Optional<Department> findById(DepartmentId id);

    Optional<Department> findByCode(String code);

    List<Department> findAllByIdIn(List<Long> ids);
}
