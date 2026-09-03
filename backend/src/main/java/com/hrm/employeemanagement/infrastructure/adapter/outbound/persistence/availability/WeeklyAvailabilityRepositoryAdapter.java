package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability;

import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.WeeklyAvailabilityJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataHolidayRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataLeaveRequestRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataWeeklyAvailabilityRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class WeeklyAvailabilityRepositoryAdapter implements LoadWeeklyAvailabilityPort,
        SaveWeeklyAvailabilityPort, LoadHolidaysPort, LoadApprovedLeavesPort {

    private final SpringDataWeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final SpringDataHolidayRepository holidayRepository;
    private final SpringDataLeaveRequestRepository leaveRequestRepository;
    private final WeeklyAvailabilityPersistenceMapper mapper;

    public WeeklyAvailabilityRepositoryAdapter(
            SpringDataWeeklyAvailabilityRepository weeklyAvailabilityRepository,
            SpringDataHolidayRepository holidayRepository,
            SpringDataLeaveRequestRepository leaveRequestRepository,
            WeeklyAvailabilityPersistenceMapper mapper) {
        this.weeklyAvailabilityRepository = Objects.requireNonNull(weeklyAvailabilityRepository, "weeklyAvailabilityRepository must not be null");
        this.holidayRepository = Objects.requireNonNull(holidayRepository, "holidayRepository must not be null");
        this.leaveRequestRepository = Objects.requireNonNull(leaveRequestRepository, "leaveRequestRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<WeeklyAvailability> findByEmployeeIdAndYearWeek(Long employeeId, YearWeek yearWeek) {
        return weeklyAvailabilityRepository.findByEmployeeIdAndYearAndWeekNumber(
                employeeId, yearWeek.year(), yearWeek.weekNumber())
                .map(mapper::toDomain);
    }

    @Override
    public WeeklyAvailability save(WeeklyAvailability availability) {
        WeeklyAvailabilityJpaEntity entity = mapper.toJpaEntity(availability);
        WeeklyAvailabilityJpaEntity saved = weeklyAvailabilityRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<LocalDate> getHolidayDatesBetween(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findHolidayDatesBetween(startDate, endDate);
    }

    @Override
    public List<com.hrm.employeemanagement.domain.availability.Holiday> getHolidaysBetween(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findHolidaysBetween(startDate, endDate).stream()
                .map(entity -> new com.hrm.employeemanagement.domain.availability.Holiday(
                        entity.getHolidayDate(),
                        entity.getName(),
                        entity.getWorkingHoursDeducted() != null ? entity.getWorkingHoursDeducted() : 8
                ))
                .toList();
    }

    @Override
    public BigDecimal getTotalApprovedLeaveHoursBetween(Long employeeId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = leaveRequestRepository.sumApprovedLeaveHoursBetween(employeeId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
}
