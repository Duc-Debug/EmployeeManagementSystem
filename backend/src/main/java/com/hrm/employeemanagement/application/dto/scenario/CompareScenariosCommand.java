package com.hrm.employeemanagement.application.dto.scenario;

import java.util.List;

public record CompareScenariosCommand(
        List<Long> scenarioIds
) {
}
