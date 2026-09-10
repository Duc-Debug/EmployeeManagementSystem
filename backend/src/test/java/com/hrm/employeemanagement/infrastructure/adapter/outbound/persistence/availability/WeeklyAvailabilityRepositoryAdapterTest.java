package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.LeaveRequestJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataHolidayRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataLeaveRequestRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataWeeklyAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyAvailabilityRepositoryAdapter Unit Tests")
class WeeklyAvailabilityRepositoryAdapterTest {

    @Mock
    private SpringDataWeeklyAvailabilityRepository weeklyAvailabilityRepository;

    @Mock
    private SpringDataHolidayRepository holidayRepository;

    @Mock
    private SpringDataLeaveRequestRepository leaveRequestRepository;

    @Mock
    private WeeklyAvailabilityPersistenceMapper mapper;

    private WeeklyAvailabilityRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WeeklyAvailabilityRepositoryAdapter(
                weeklyAvailabilityRepository,
                holidayRepository,
                leaveRequestRepository,
                mapper
        );
    }

    @Test
    @DisplayName("getTotalApprovedLeaveHoursBetween: Phân bổ đúng giờ nghỉ khi đơn kéo dài qua nhiều tuần")
    void getTotalApprovedLeaveHoursBetween_MultiWeekLeave_PartitionsCorrectly() {
        Long employeeId = 1L;
        YearWeek week36 = YearWeek.of(2026, 36); // 2026-08-31 to 2026-09-06

        // Đơn nghỉ từ 2026-09-01 (Tue) đến 2026-09-11 (Fri) = 9 working days, 72 hours
        LeaveRequestJpaEntity leave = new LeaveRequestJpaEntity(
                1L,
                employeeId,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 11),
                "APPROVED",
                new BigDecimal("72.00")
        );

        when(leaveRequestRepository.findApprovedLeavesBetween(
                employeeId, week36.getStartDate(), week36.getEndDate()))
                .thenReturn(List.of(leave));

        BigDecimal result = adapter.getTotalApprovedLeaveHoursBetween(
                employeeId, week36.getStartDate(), week36.getEndDate());

        // Tuần 36 chỉ có 4 working days (Tue-Fri) -> 72 * 4 / 9 = 32.00h (thay vì bị trừ cả 72h)
        assertEquals(new BigDecimal("32.00"), result);
    }

    @Test
    @DisplayName("getTotalApprovedLeaveHoursBetween: Trả về 0.00 khi không có đơn nghỉ phép nào")
    void getTotalApprovedLeaveHoursBetween_NoLeaves_ReturnsZero() {
        Long employeeId = 1L;
        YearWeek week36 = YearWeek.of(2026, 36);

        when(leaveRequestRepository.findApprovedLeavesBetween(
                employeeId, week36.getStartDate(), week36.getEndDate()))
                .thenReturn(Collections.emptyList());

        BigDecimal result = adapter.getTotalApprovedLeaveHoursBetween(
                employeeId, week36.getStartDate(), week36.getEndDate());

        assertEquals(new BigDecimal("0.00"), result);
    }
}
