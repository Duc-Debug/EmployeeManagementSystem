package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.HolidayJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SpringDataHolidayRepository extends JpaRepository<HolidayJpaEntity, Long> {

    @Query("SELECT h.holidayDate FROM HolidayJpaEntity h WHERE h.holidayDate >= :startDate AND h.holidayDate <= :endDate")
    List<LocalDate> findHolidayDatesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
