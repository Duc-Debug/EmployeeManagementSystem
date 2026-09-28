package com.hrm.employeemanagement.domain.availability;

import java.time.LocalDate;
import java.util.Objects;

public record Holiday(
        LocalDate date,
        String name,
        int workingHoursDeducted
) {
    public Holiday {
        Objects.requireNonNull(date, "Ngày lễ không được null");
        name = (name != null && !name.isBlank()) ? name.trim() : "Ngày lễ";
        workingHoursDeducted = Math.max(0, workingHoursDeducted);
    }
}
