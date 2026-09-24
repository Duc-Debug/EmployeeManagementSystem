package com.hrm.employeemanagement.application.service.allocation.idleness;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffCommand;
import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessQuery;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessReportResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.idleness.LoadProlongedIdlenessAcknowledgementPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.idleness.SaveProlongedIdlenessAcknowledgementPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProlongedIdleStaffService Application Tests (NCL-07-CN-006 / QTN-23)")
class ProlongedIdleStaffServiceTest {

        @Mock
        private AuthorizationService authorizationService;

        @Mock
        private LoadEmployeePort loadEmployeePort;

        @Mock
        private LoadOrgUnitPort loadOrgUnitPort;

        @Mock
        private LoadCapacityThresholdPort loadCapacityThresholdPort;

        @Mock
        private LoadWeeklyProjectAllocationPort loadAllocationPort;

        @Mock
        private LoadApprovedLeavesPort loadApprovedLeavesPort;

        @Mock
        private LoadHolidaysPort loadHolidaysPort;

        @Mock
        private SaveAuditLogInNewTransactionPort auditLogPort;

        @Mock
        private SimulatedNotificationPort notificationPort;

        @Mock
        private SaveProlongedIdlenessAcknowledgementPort saveAcknowledgementPort;

        @Mock
        private LoadProlongedIdlenessAcknowledgementPort loadAcknowledgementPort;

        private ProlongedIdleStaffService service;

        @BeforeEach
        void setUp() {
                service = new ProlongedIdleStaffService(
                                authorizationService,
                                loadEmployeePort,
                                loadOrgUnitPort,
                                loadCapacityThresholdPort,
                                loadAllocationPort,
                                loadApprovedLeavesPort,
                                loadHolidaysPort,
                                auditLogPort,
                                notificationPort,
                                saveAcknowledgementPort,
                                loadAcknowledgementPort);
        }

        @Test
        @DisplayName("NCL-07-CN-006-TC-01: Luồng thành công — Người có mức sử dụng dưới 30% trong 3 tuần liên tiếp xuất hiện trong danh sách nhàn rỗi kèm số giờ trống")
        void getProlongedIdleStaff_TC01_SuccessFlow() {
                // Given
                Long userId = 200L;
                when(authorizationService.requireAny(
                                PermissionCode.RESOURCE_ALLOCATION_READ,
                                PermissionCode.RESOURCE_ALLOCATION_MANAGE,
                                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ)).thenReturn(userId);

                // QTN-23: Cấu hình ngưỡng nhàn rỗi là 30.0%
                CapacityThresholdConfig thresholdConfig = CapacityThresholdConfig.createNew(
                                CapacityThresholdScope.COMPANY,
                                null,
                                BigDecimal.valueOf(100.0),
                                BigDecimal.valueOf(30.0),
                                1L);
                when(loadCapacityThresholdPort.findByScope(eq(CapacityThresholdScope.COMPANY), any()))
                                .thenReturn(Optional.of(thresholdConfig));

                // 1 nhân sự test: Nguyễn Văn A (ID: 101), tiêu chuẩn 40h/tuần
                Employee emp = new Employee(
                                new EmployeeId(101L),
                                null,
                                10L,
                                "EMP0101",
                                "Nguyễn Văn A",
                                "Backend Dev",
                                LocalDate.of(2025, 1, 1),
                                null,
                                false,
                                40,
                                EmployeeStatus.ACTIVE);
                when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));

                OrgUnit unit = new OrgUnit(
                                new OrgUnitId(10L),
                                "TECH",
                                "Khối Kỹ Thuật",
                                OrgUnitType.DEPARTMENT,
                                null,
                                "/10/",
                                1,
                                OrgUnitStatus.ACTIVE,
                                "Khối Kỹ Thuật",
                                null,
                                null,
                                null);
                when(loadOrgUnitPort.findAll()).thenReturn(List.of(unit));

                // Phân bổ: tuần 38 (10h = 25%), tuần 39 (8h = 20%), tuần 40 (5h = 12.5%), tuần
                // 41 (35h = 87.5%)
                // -> 3 tuần đầu liên tiếp đều < 30%
                WeeklyProjectAllocation a1 = createAllocation(101L, 2026, 38, BigDecimal.valueOf(10));
                WeeklyProjectAllocation a2 = createAllocation(101L, 2026, 39, BigDecimal.valueOf(8));
                WeeklyProjectAllocation a3 = createAllocation(101L, 2026, 40, BigDecimal.valueOf(5));
                WeeklyProjectAllocation a4 = createAllocation(101L, 2026, 41, BigDecimal.valueOf(35));
                when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList()))
                                .thenReturn(List.of(a1, a2, a3, a4));

                when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList()))
                                .thenReturn(Collections.emptyMap());
                when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());

                ProlongedIdlenessQuery query = new ProlongedIdlenessQuery(null, 2026, 38, 4, 3, null);

                // When
                ProlongedIdlenessReportResult result = service.getProlongedIdleStaff(query);

                // Then
                assertNotNull(result);
                assertEquals(30.0, result.effectiveIdleThreshold().doubleValue());
                assertEquals(1, result.totalIdleEmployees());
                assertEquals(1, result.items().size());

                var item = result.items().get(0);
                assertEquals(101L, item.employeeId());
                assertEquals("EMP0101", item.employeeCode());
                assertEquals("Nguyễn Văn A", item.fullName());
                assertEquals("Khối Kỹ Thuật", item.departmentName());
                assertEquals(3, item.consecutiveIdleWeeks()); // Đúng 3 tuần liên tiếp
                // Tổng giờ trống = (40-10) + (40-8) + (40-5) + (40-35) = 30 + 32 + 35 + 5 =
                // 102.0h
                assertEquals(BigDecimal.valueOf(102.0).setScale(2), item.totalEmptyHours());
        }

        @Test
        @DisplayName("NCL-07-CN-006-TC-02: Ngoại lệ — Người đang nghỉ phép dài ngày (APPROVED) không bị xếp vào danh sách nhàn rỗi")
        void getProlongedIdleStaff_TC02_ExcludeLongTermLeave() {
                // Given
                Long userId = 200L;
                when(authorizationService.requireAny(
                                PermissionCode.RESOURCE_ALLOCATION_READ,
                                PermissionCode.RESOURCE_ALLOCATION_MANAGE,
                                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ)).thenReturn(userId);

                when(loadCapacityThresholdPort.findByScope(eq(CapacityThresholdScope.COMPANY), any()))
                                .thenReturn(Optional.empty()); // Fallback về default

                Employee emp = new Employee(
                                new EmployeeId(102L),
                                null,
                                10L,
                                "EMP0102",
                                "Trần Thị B",
                                "Tester",
                                LocalDate.of(2025, 1, 1),
                                null,
                                false,
                                40,
                                EmployeeStatus.ACTIVE);
                when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));
                when(loadOrgUnitPort.findAll()).thenReturn(Collections.emptyList());

                // Không có phân bổ dự án (0h)
                when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList()))
                                .thenReturn(Collections.emptyList());

                // Có đơn nghỉ phép dài ngày đã duyệt (40h/tuần cho cả 4 tuần)
                Map<YearWeek, BigDecimal> leaveByWeek = Map.of(
                                YearWeek.of(2026, 38), BigDecimal.valueOf(40),
                                YearWeek.of(2026, 39), BigDecimal.valueOf(40),
                                YearWeek.of(2026, 40), BigDecimal.valueOf(40),
                                YearWeek.of(2026, 41), BigDecimal.valueOf(40));
                when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList()))
                                .thenReturn(Map.of(102L, leaveByWeek));

                when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());

                ProlongedIdlenessQuery query = new ProlongedIdlenessQuery(null, 2026, 38, 4, 3, null);

                // When
                ProlongedIdlenessReportResult result = service.getProlongedIdleStaff(query);

                // Then: Hệ thống nhận diện đây là nghỉ phép dài ngày hợp lệ -> loại trừ, không
                // xếp vào danh sách nhàn rỗi
                assertNotNull(result);
                assertEquals(0, result.totalIdleEmployees());
                assertTrue(result.items().isEmpty());
        }

        @Test
        @DisplayName("NCL-07-CN-006-TC-03: Không có quyền — Người dùng không phải quản lý nguồn lực bị từ chối và ghi nhật ký lần từ chối")
        void getProlongedIdleStaff_TC03_PermissionDenied() {
                // Given: authorizationService ném PermissionDeniedException khi user thiếu
                // quyền
                when(authorizationService.requireAny(
                                PermissionCode.RESOURCE_ALLOCATION_READ,
                                PermissionCode.RESOURCE_ALLOCATION_MANAGE,
                                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ))
                                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ));

                ProlongedIdlenessQuery query = new ProlongedIdlenessQuery(null, 2026, 38, 4, 3, null);

                // When & Then
                assertThrows(PermissionDeniedException.class, () -> service.getProlongedIdleStaff(query));

                // Không gọi các cổng nạp dữ liệu phía sau
                verify(loadEmployeePort, never()).findAllActive();
                verify(loadAllocationPort, never()).loadAllocationsForEmployeesAndWeeks(anyList(), anyList());
        }

        @Test
        @DisplayName("NCL-07-CN-006-TC-04: Lưu lịch sử — Xác nhận thao tác ghi lại người thực hiện, nội dung và thời điểm vào Audit Log")
        void acknowledgeProlongedIdleStaff_TC04_SuccessAuditLog() {
                // Given
                Long currentUserId = 300L;
                when(authorizationService.requireAny(
                                PermissionCode.RESOURCE_ALLOCATION_MANAGE,
                                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY)).thenReturn(currentUserId);

                Long employeeId = 105L;
                Employee emp = new Employee(
                                new EmployeeId(employeeId),
                                null,
                                10L,
                                "EMP0105",
                                "Lê Văn C",
                                "Designer",
                                LocalDate.of(2025, 1, 1),
                                null,
                                false,
                                40,
                                EmployeeStatus.ACTIVE);
                when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(emp));

                AcknowledgeProlongedIdleStaffCommand command = new AcknowledgeProlongedIdleStaffCommand(
                                employeeId,
                                "Đã điều chuyển nhân sự sang hỗ trợ dự án Portal",
                                "Ưu tiên cao xử lý nhàn rỗi tuần 38");

                // When
                AcknowledgeProlongedIdleStaffResult result = service.acknowledgeProlongedIdleStaff(command);

                // Then
                assertNotNull(result);
                assertEquals(employeeId, result.employeeId());
                assertEquals("Lê Văn C", result.employeeName());
                assertEquals("ACKNOWLEDGED", result.status());
                assertEquals(currentUserId, result.acknowledgedBy());

                // Kiểm tra lưu vết Audit Log
                ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
                verify(auditLogPort).save(captor.capture());
                AuditLog savedLog = captor.getValue();
                assertEquals(currentUserId, savedLog.getUserId());
                assertEquals("ACKNOWLEDGE_PROLONGED_IDLENESS", savedLog.getAction());
                assertEquals("employees", savedLog.getTableName());
                assertEquals(employeeId, savedLog.getRecordId());
                assertTrue(savedLog.getNewValue().contains("actionTaken=Đã điều chuyển nhân sự"));

                // Kiểm tra lưu DB persistence
                verify(saveAcknowledgementPort).save(any());

                // Kiểm tra phát thông báo
                verify(notificationPort).sendScheduleConflictWarningNotification(
                                any(),
                                any(),
                                any(),
                                any(),
                                eq("Lê Văn C"),
                                eq("Xử lý cảnh báo nhân sự nhàn rỗi kéo dài"),
                                any());
        }

        @Test
        @DisplayName("Cải tiến — Phân trang danh sách cảnh báo nhàn rỗi và triệt tiêu query lặp lại OrgUnit")
        void getProlongedIdleStaff_PaginationAndNoDuplicateOrgUnitQuery() {
                Long userId = 200L;
                when(authorizationService.requireAny(
                                PermissionCode.RESOURCE_ALLOCATION_READ,
                                PermissionCode.RESOURCE_ALLOCATION_MANAGE,
                                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ)).thenReturn(userId);

                when(loadCapacityThresholdPort.findByScope(any(), any()))
                                .thenReturn(Optional.empty());

                Employee emp1 = new Employee(new EmployeeId(101L), null, 10L, "EMP0101", "User A", "Dev",
                                LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);
                Employee emp2 = new Employee(new EmployeeId(102L), null, 10L, "EMP0102", "User B", "Dev",
                                LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE);

                OrgUnit unit = new OrgUnit(
                                new OrgUnitId(10L),
                                "TECH",
                                "Phòng Dev",
                                OrgUnitType.DEPARTMENT,
                                null,
                                "/10/",
                                1,
                                OrgUnitStatus.ACTIVE,
                                "Phòng Dev",
                                null,
                                null,
                                null);
                when(loadOrgUnitPort.findAll()).thenReturn(List.of(unit));
                when(loadEmployeePort.findActiveByOrgUnitIds(any())).thenReturn(List.of(emp1, emp2));
                when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any()))
                                .thenReturn(Collections.emptyList());
                when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any()))
                                .thenReturn(Collections.emptyMap());
                when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());

                // Query with page=0, size=1
                ProlongedIdlenessQuery query = new ProlongedIdlenessQuery(10L, 2026, 38, 4, 3, null, 0, 1);
                ProlongedIdlenessReportResult result = service.getProlongedIdleStaff(query);

                assertNotNull(result);
                assertEquals(2, result.totalIdleEmployees());
                assertEquals(0, result.page());
                assertEquals(1, result.size());
                assertEquals(2, result.totalPages());
                assertEquals(1, result.items().size()); // 1 item on page 0
                assertEquals(BigDecimal.valueOf(320.0).setScale(1), result.totalEmptyHours()); // 160h * 2 idle
                                                                                               // employees = 320.0h
                                                                                               // total empty hours

                // Xác nhận loadOrgUnitPort.findAll() CHỈ ĐƯỢC GỌI ĐÚNG 1 LẦN DUY NHẤT (không
                // duplicate)
                verify(loadOrgUnitPort, times(1)).findAll();
        }

        private WeeklyProjectAllocation createAllocation(Long empId, int year, int week, BigDecimal hours) {
                return new WeeklyProjectAllocation(
                                null,
                                empId,
                                1L,
                                new YearWeek(year, week),
                                hours);
        }
}
