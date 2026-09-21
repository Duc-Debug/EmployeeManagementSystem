package com.hrm.employeemanagement.application.port.outbound.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleConfirmationPort {

    record ScheduleConfirmationRecord(
            Long id,
            Long userId,
            LocalDate weekStartDate,
            LocalDateTime confirmedAt,
            String ipAddress
    ) {}

    record SaveConfirmationResult(
            ScheduleConfirmationRecord record,
            boolean newlyCreated
    ) {}

    Optional<ScheduleConfirmationRecord> findByUserIdAndWeek(Long userId, LocalDate weekStartDate);

    List<ScheduleConfirmationRecord> findByUserIdAndWeeks(Long userId, List<LocalDate> weekStartDates);

    SaveConfirmationResult saveConfirmation(Long userId, LocalDate weekStartDate, LocalDateTime confirmedAt, String ipAddress);
}