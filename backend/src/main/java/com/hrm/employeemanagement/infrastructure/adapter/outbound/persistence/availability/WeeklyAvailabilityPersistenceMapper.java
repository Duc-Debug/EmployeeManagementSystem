package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability;

import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.WeeklyAvailabilityJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class WeeklyAvailabilityPersistenceMapper {

    public WeeklyAvailability toDomain(WeeklyAvailabilityJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        YearWeek yearWeek = YearWeek.of(entity.getYear(), entity.getWeekNumber());
        return new WeeklyAvailability(
                entity.getId(),
                entity.getEmployeeId(),
                yearWeek,
                entity.getStandardHours(),
                entity.getHolidayHours(),
                entity.getApprovedLeaveHours(),
                entity.getNetAvailableHours()
        );
    }

    public WeeklyAvailabilityJpaEntity toJpaEntity(WeeklyAvailability domain) {
        if (domain == null) {
            return null;
        }
        WeeklyAvailabilityJpaEntity entity = new WeeklyAvailabilityJpaEntity(
                domain.getId(),
                domain.getEmployeeId(),
                domain.getYear(),
                domain.getWeekNumber(),
                domain.getStandardHours(),
                domain.getHolidayHours(),
                domain.getApprovedLeaveHours(),
                domain.getNetAvailableHours()
        );
        return entity;
    }
}
