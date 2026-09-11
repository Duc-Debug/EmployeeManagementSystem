package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.calendar.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.calendar.entity.WorkingCalendarConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataWorkingCalendarRepository extends JpaRepository<WorkingCalendarConfigJpaEntity, Long> {

    List<WorkingCalendarConfigJpaEntity> findByCompanyCode(String companyCode);

    Optional<WorkingCalendarConfigJpaEntity> findByCompanyCodeAndDayOfWeek(String companyCode, String dayOfWeek);
}
