package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto;

import java.time.LocalDate;
import java.util.List;

public record AssignTaskRequest(
        List<Long> employeeIds,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate
) {}

