package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.employee.Employee;

public interface SaveEmployeePort {
    Employee save(Employee employee);
}
