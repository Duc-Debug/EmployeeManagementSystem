package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek;

import com.hrm.employeemanagement.domain.workweek.CapacityUnit;
import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekConfig;
import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekDay;
import com.hrm.employeemanagement.domain.workweek.WeekStartDay;
import com.hrm.employeemanagement.domain.workweek.WorkWeekScope;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek.entity.StandardWorkWeekConfigJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek.entity.StandardWorkWeekDayJpaEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Component
public class StandardWorkWeekPersistenceMapper {

    public StandardWorkWeekConfig toDomain(StandardWorkWeekConfigJpaEntity entity) {
        if (entity == null) return null;

        WorkWeekScope scope = "COMPANY".equalsIgnoreCase(entity.getScopeType())
                ? WorkWeekScope.company()
                : WorkWeekScope.orgUnit(entity.getOrgUnitId());

        CapacityUnit capacityUnit;
        try {
            capacityUnit = CapacityUnit.valueOf(entity.getCapacityUnit());
        } catch (Exception e) {
            capacityUnit = CapacityUnit.HOURS;
        }

        WeekStartDay weekStartDay;
        try {
            weekStartDay = WeekStartDay.valueOf(entity.getWeekStartDay());
        } catch (Exception e) {
            weekStartDay = WeekStartDay.MONDAY;
        }

        List<StandardWorkWeekDay> domainDays = new ArrayList<>();
        if (entity.getDays() != null) {
            for (StandardWorkWeekDayJpaEntity dayEntity : entity.getDays()) {
                DayOfWeek dow = DayOfWeek.valueOf(dayEntity.getDayOfWeek());
                domainDays.add(new StandardWorkWeekDay(dow, Boolean.TRUE.equals(dayEntity.getIsWorkingDay()), dayEntity.getWorkingHours()));
            }
        }

        return new StandardWorkWeekConfig(
                entity.getId(),
                scope,
                capacityUnit,
                weekStartDay,
                entity.getStandardHoursPerDay(),
                domainDays,
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public StandardWorkWeekConfigJpaEntity toJpaEntity(StandardWorkWeekConfig domain, StandardWorkWeekConfigJpaEntity existingEntity) {
        StandardWorkWeekConfigJpaEntity entity = existingEntity != null ? existingEntity : new StandardWorkWeekConfigJpaEntity();

        entity.setScopeType(domain.getScope().scopeType().name());
        entity.setScopeKey(domain.getScope().toScopeKey());
        entity.setOrgUnitId(domain.getScope().orgUnitId());
        entity.setCapacityUnit(domain.getCapacityUnit().name());
        entity.setWeekStartDay(domain.getWeekStartDay().name());
        entity.setStandardHoursPerDay(domain.getStandardHoursPerDay());
        entity.setStandardHoursPerWeek(domain.getStandardHoursPerWeek());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        // Update day entities in-place to prevent Hibernate unique constraint issues
        List<StandardWorkWeekDayJpaEntity> currentDayEntities = entity.getDays();
        if (currentDayEntities == null) {
            currentDayEntities = new ArrayList<>();
            entity.setDays(currentDayEntities);
        }

        java.util.Map<String, StandardWorkWeekDayJpaEntity> existingMap = new java.util.HashMap<>();
        for (StandardWorkWeekDayJpaEntity d : currentDayEntities) {
            existingMap.put(d.getDayOfWeek(), d);
        }

        for (StandardWorkWeekDay domainDay : domain.getDays()) {
            String dow = domainDay.getDayOfWeek().name();
            StandardWorkWeekDayJpaEntity dayEntity = existingMap.get(dow);
            if (dayEntity != null) {
                dayEntity.setIsWorkingDay(domainDay.isWorkingDay());
                dayEntity.setWorkingHours(domainDay.getWorkingHours());
            } else {
                currentDayEntities.add(new StandardWorkWeekDayJpaEntity(
                        entity,
                        dow,
                        domainDay.isWorkingDay(),
                        domainDay.getWorkingHours()
                ));
            }
        }

        return entity;
    }
}
