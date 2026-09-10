package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;

@Repository
public interface SpringDataWeeklyProjectAllocationRepository extends JpaRepository<WeeklyProjectAllocationJpaEntity, Long> {

    // Tìm bản ghi phân bổ cụ thể của 1 nhân sự cho 1 dự án trong 1 tuần
    Optional<WeeklyProjectAllocationJpaEntity> findByEmployeeIdAndProjectIdAndYearAndWeekNumber(
            Long employeeId, Long projectId, Integer year, Integer weekNumber);

    // Lấy tất cả các phân bổ của 1 nhân sự trong 1 tuần (để tính tổng số giờ đã gán cho nhiều dự án khác nhau)
    List<WeeklyProjectAllocationJpaEntity> findByEmployeeIdAndYearAndWeekNumber(
            Long employeeId, Integer year, Integer weekNumber);

    // Lấy danh sách phân bổ cho nhiều nhân sự trong khoảng tuần (phục vụ hiển thị Bảng Ma trận Năng lực)
    List<WeeklyProjectAllocationJpaEntity> findByEmployeeIdInAndYearAndWeekNumberBetween(
            List<Long> employeeIds, Integer year, Integer startWeek, Integer endWeek);

    // Lấy danh sách phân bổ theo danh sách nhân sự và danh sách tuần cụ thể (phục vụ batch search)
    List<WeeklyProjectAllocationJpaEntity> findByEmployeeIdInAndYearAndWeekNumberIn(
            List<Long> employeeIds, Integer year, List<Integer> weekNumbers);

    List<WeeklyProjectAllocationJpaEntity> findByProjectIdAndYearAndWeekNumberBetween(
            Long projectId, Integer year, Integer startWeek, Integer endWeek);
}
