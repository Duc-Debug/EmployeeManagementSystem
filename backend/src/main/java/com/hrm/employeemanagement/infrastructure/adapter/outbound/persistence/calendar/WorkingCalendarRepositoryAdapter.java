package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.calendar;

import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayCommandPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayQueryPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayRecord;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.SaveWorkingCalendarPort;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.calendar.WorkingCalendarDay;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.HolidayJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataHolidayRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.calendar.entity.WorkingCalendarConfigJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.calendar.repository.SpringDataWorkingCalendarRepository;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class WorkingCalendarRepositoryAdapter implements
        LoadWorkingCalendarPort,
        SaveWorkingCalendarPort,
        HolidayCommandPort,
        HolidayQueryPort {

    private static final String COMPANY_CODE = "DEFAULT";

    private final SpringDataWorkingCalendarRepository workingCalendarRepository;
    private final SpringDataHolidayRepository holidayRepository;

    public WorkingCalendarRepositoryAdapter(
            SpringDataWorkingCalendarRepository workingCalendarRepository,
            SpringDataHolidayRepository holidayRepository) {
        this.workingCalendarRepository = Objects.requireNonNull(workingCalendarRepository, "workingCalendarRepository must not be null");
        this.holidayRepository = Objects.requireNonNull(holidayRepository, "holidayRepository must not be null");
    }

    @Override
    public CompanyWorkingCalendar loadCompanyCalendar() {
        List<WorkingCalendarConfigJpaEntity> entities = workingCalendarRepository.findByCompanyCode(COMPANY_CODE);
        if (entities.isEmpty()) {
            return CompanyWorkingCalendar.createDefault();
        }

        List<WorkingCalendarDay> days = new ArrayList<>();
        for (WorkingCalendarConfigJpaEntity entity : entities) {
            try {
                DayOfWeek dow = DayOfWeek.valueOf(entity.getDayOfWeek());
                days.add(new WorkingCalendarDay(dow, Boolean.TRUE.equals(entity.getIsWorkingDay())));
            } catch (IllegalArgumentException ignored) {}
        }

        if (days.isEmpty()) {
            return CompanyWorkingCalendar.createDefault();
        }

        return new CompanyWorkingCalendar(days);
    }

    @Override
    public CompanyWorkingCalendar saveCompanyCalendar(CompanyWorkingCalendar calendar) {
        for (WorkingCalendarDay day : calendar.getDays()) {
            Optional<WorkingCalendarConfigJpaEntity> existingOpt =
                    workingCalendarRepository.findByCompanyCodeAndDayOfWeek(COMPANY_CODE, day.dayOfWeek().name());

            WorkingCalendarConfigJpaEntity entity;
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                entity.setIsWorkingDay(day.isWorkingDay());
            } else {
                entity = new WorkingCalendarConfigJpaEntity(null, COMPANY_CODE, day.dayOfWeek().name(), day.isWorkingDay());
            }
            workingCalendarRepository.save(entity);
        }

        return loadCompanyCalendar();
    }

    @Override
    public HolidayRecord create(LocalDate date, String name, int workingHoursDeducted) {
        HolidayJpaEntity entity = new HolidayJpaEntity(null, date, name, workingHoursDeducted);
        HolidayJpaEntity saved = holidayRepository.save(entity);
        return toRecord(saved);
    }

    @Override
    public HolidayRecord update(Long id, LocalDate date, String name, int workingHoursDeducted) {
        HolidayJpaEntity entity = new HolidayJpaEntity(id, date, name, workingHoursDeducted);
        HolidayJpaEntity saved = holidayRepository.save(entity);
        return toRecord(saved);
    }

    @Override
    public void deleteById(Long id) {
        holidayRepository.deleteById(id);
    }

    @Override
    public Optional<HolidayRecord> findById(Long id) {
        return holidayRepository.findById(id).map(this::toRecord);
    }

    @Override
    public boolean existsByDate(LocalDate date) {
        return holidayRepository.existsByHolidayDate(date);
    }

    @Override
    public boolean existsByDateAndIdNot(LocalDate date, Long id) {
        return holidayRepository.existsByHolidayDateAndIdNot(date, id);
    }

    @Override
    public List<HolidayRecord> findByYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end).stream()
                .map(this::toRecord)
                .toList();
    }

    private HolidayRecord toRecord(HolidayJpaEntity entity) {
        return new HolidayRecord(
                entity.getId(),
                entity.getHolidayDate(),
                entity.getName(),
                entity.getWorkingHoursDeducted() != null ? entity.getWorkingHoursDeducted() : 8
        );
    }
}
