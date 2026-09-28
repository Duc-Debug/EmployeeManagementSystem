package com.hrm.employeemanagement.application.port.outbound.scenario.recruitment;

import com.hrm.employeemanagement.domain.scenario.recruitment.SimulatedEmployeeId;

public interface DeleteSimulatedEmployeePort {
    void deleteById(SimulatedEmployeeId id);
}
