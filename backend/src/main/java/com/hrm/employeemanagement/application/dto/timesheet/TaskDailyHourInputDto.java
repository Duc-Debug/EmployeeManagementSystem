package com.hrm.employeemanagement.application.dto.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskDailyHourInputDto(
    LocalDate workDate,
    BigDecimal hours
) {}

