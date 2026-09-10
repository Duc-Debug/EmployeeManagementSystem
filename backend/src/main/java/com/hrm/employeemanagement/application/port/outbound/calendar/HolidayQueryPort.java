package com.hrm.employeemanagement.application.port.outbound.calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayQueryPort {
    Optional<HolidayRecord> findById(Long id);
    boolean existsByDate(LocalDate date);
    boolean existsByDateAndIdNot(LocalDate date, Long id);
    List<HolidayRecord> findByYear(int year);
}
