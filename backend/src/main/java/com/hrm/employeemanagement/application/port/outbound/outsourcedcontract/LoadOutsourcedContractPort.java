package com.hrm.employeemanagement.application.port.outbound.outsourcedcontract;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.hrm.employeemanagement.domain.employee.Employee;

public interface LoadOutsourcedContractPort {
    List<Employee> findAllOutsourcedEmployeesWithContract();
    Optional<Employee> findById(Long employeeId);
    Map<Long, String> findOrgUnitNamesByIds(List<Long> orgUnitIds);
}
