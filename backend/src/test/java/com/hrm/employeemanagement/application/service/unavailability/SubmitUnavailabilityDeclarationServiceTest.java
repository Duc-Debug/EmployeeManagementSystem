package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.SubmitUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.outbound.unavailability.SaveUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException;
import com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityConflictException;
import java.util.List;

class SubmitUnavailabilityDeclarationServiceTest {

    private LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private SaveUnavailabilityDeclarationPort saveUnavailabilityPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadUserPort loadUserPort;
    private AuthorizationService authorizationService;
    private SaveAuditLogPort saveAuditLogPort;
    private SubmitUnavailabilityDeclarationService service;

    @BeforeEach
    void setUp() {
        loadUnavailabilityPort = mock(LoadUnavailabilityDeclarationPort.class);
        saveUnavailabilityPort = mock(SaveUnavailabilityDeclarationPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadUserPort = mock(LoadUserPort.class);
        authorizationService = mock(AuthorizationService.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new SubmitUnavailabilityDeclarationService(
                loadUnavailabilityPort,
                saveUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                authorizationService,
                saveAuditLogPort,
                null
        );
    }

    @Test
    @DisplayName("NCL-13-CN-003-TC-03: Người dùng không phải chính chủ tài khoản -> Từ chối truy cập và ghi nhật ký lần từ chối")
    void testDeclareForOtherEmployeeThrows403AndRecordsAudit() {
        Long currentUserId = 10L; // User A đang đăng nhập
        Long targetEmployeeId = 20L;
        Long otherUserId = 99L; // Employee B thuộc về User B (ID 99 != 10)

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE)).thenReturn(currentUserId);

        Role role = mock(Role.class);
        when(role.getCode()).thenReturn(RoleCode.VT_04);
        User currentUser = mock(User.class);
        when(currentUser.getIdValue()).thenReturn(currentUserId);
        when(currentUser.getRole()).thenReturn(role);
        when(currentUser.getDataScope()).thenReturn(DataScope.SELF);

        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));

        Employee targetEmployee = new Employee(
                new EmployeeId(targetEmployeeId),
                new UserId(otherUserId),
                1L,
                "EMP002",
                "Nguyễn Văn B",
                "DEV",
                LocalDate.of(2023, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(targetEmployeeId))).thenReturn(Optional.of(targetEmployee));

        SubmitUnavailabilityCommand command = new SubmitUnavailabilityCommand(
                targetEmployeeId,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Đào tạo kỹ năng"
        );

        // Chặn quyền 403
        assertThrows(PermissionDeniedException.class, () -> service.submit(command));

        // Kiểm tra TC-03: Phải ghi nhật ký lần từ chối vào audit_logs
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog recordedLog = auditCaptor.getValue();
        assertEquals("ACCESS_DENIED_UNAVAILABILITY_DECLARE", recordedLog.getAction());
        assertEquals(currentUserId, recordedLog.getUserId());
        assertTrue(recordedLog.getNewValue().contains("target_employee_id=20"));

        // Đảm bảo không lưu khai báo
        verify(saveUnavailabilityPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-13-CN-003-TC-04: Khai báo thành công ghi lại người thực hiện, nội dung và thời điểm vào audit log")
    void testSubmitSuccessRecordsAuditLog() {
        Long currentUserId = 10L;
        Long employeeId = 10L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE)).thenReturn(currentUserId);

        Role role = mock(Role.class);
        when(role.getCode()).thenReturn(RoleCode.VT_04);
        User currentUser = mock(User.class);
        when(currentUser.getIdValue()).thenReturn(currentUserId);
        when(currentUser.getRole()).thenReturn(role);
        when(currentUser.getDataScope()).thenReturn(DataScope.SELF);

        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));

        Employee employee = new Employee(
                new EmployeeId(employeeId),
                new UserId(currentUserId),
                1L,
                "EMP001",
                "Nguyễn Văn A",
                "DEV",
                LocalDate.of(2023, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        when(saveUnavailabilityPort.save(any(UnavailabilityDeclaration.class))).thenAnswer(invocation -> {
            UnavailabilityDeclaration decl = invocation.getArgument(0);
            return new UnavailabilityDeclaration(
                    100L,
                    decl.getEmployeeId(),
                    decl.getStartDate(),
                    decl.getEndDate(),
                    decl.getReasonType(),
                    decl.getReasonDetail(),
                    decl.getTotalHoursDeducted(),
                    decl.getStatus(),
                    decl.getApproverId(),
                    decl.getApproverComment(),
                    decl.getApprovedAt(),
                    decl.getCreatedAt(),
                    decl.getUpdatedAt(),
                    0L
            );
        });

        SubmitUnavailabilityCommand command = new SubmitUnavailabilityCommand(
                employeeId,
                LocalDate.of(2026, 9, 22),
                LocalDate.of(2026, 9, 23),
                UnavailabilityReasonType.TRAINING,
                "Đi học khóa AWS Solutions Architect"
        );

        UnavailabilityDeclarationResult result = service.submit(command);

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals(UnavailabilityStatus.PENDING, result.status());
        assertEquals(BigDecimal.valueOf(16.00).setScale(2), result.totalHoursDeducted());

        // Kiểm tra TC-04: Lưu lịch sử thao tác
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals("SUBMIT_UNAVAILABILITY", audit.getAction());
        assertEquals(currentUserId, audit.getUserId());
        assertEquals(100L, audit.getRecordId());
        assertTrue(audit.getNewValue().contains("2026-09-22"));
        assertTrue(audit.getNewValue().contains("TRAINING"));
    }

    @Test
    @DisplayName("Nộp đơn với ngày bắt đầu trong quá khứ -> Ném ngoại lệ InvalidUnavailabilityPeriodException")
    void testSubmitPastDateThrowsException() {
        Long currentUserId = 10L;
        Long employeeId = 10L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE)).thenReturn(currentUserId);
        User currentUser = mock(User.class);
        when(currentUser.getIdValue()).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));

        Employee employee = new Employee(
                new EmployeeId(employeeId),
                new UserId(currentUserId),
                1L, "EMP001", "Nguyễn Văn A", "DEV",
                LocalDate.of(2023, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        SubmitUnavailabilityCommand command = new SubmitUnavailabilityCommand(
                employeeId,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(2),
                UnavailabilityReasonType.TRAINING,
                "Quá khứ"
        );

        assertThrows(InvalidUnavailabilityPeriodException.class, () -> service.submit(command));
    }

    @Test
    @DisplayName("Nộp đơn bị trùng khoảng ngày với đơn PENDING/APPROVED khác -> Ném ngoại lệ UnavailabilityConflictException")
    void testSubmitOverlappingActiveDeclarationThrowsException() {
        Long currentUserId = 10L;
        Long employeeId = 10L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE)).thenReturn(currentUserId);
        User currentUser = mock(User.class);
        when(currentUser.getIdValue()).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));

        Employee employee = new Employee(
                new EmployeeId(employeeId),
                new UserId(currentUserId),
                1L, "EMP001", "Nguyễn Văn A", "DEV",
                LocalDate.of(2023, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        LocalDate start = LocalDate.of(2026, 9, 22);
        LocalDate end = LocalDate.of(2026, 9, 25);

        UnavailabilityDeclaration existing = UnavailabilityDeclaration.create(
                employeeId, start, end, UnavailabilityReasonType.TRAINING, "Trùng lặp", BigDecimal.valueOf(32)
        );
        when(loadUnavailabilityPort.findActiveOverlapping(employeeId, start, end)).thenReturn(List.of(existing));

        SubmitUnavailabilityCommand command = new SubmitUnavailabilityCommand(
                employeeId, start, end, UnavailabilityReasonType.TRAINING, "Đào tạo mới"
        );

        assertThrows(UnavailabilityConflictException.class, () -> service.submit(command));
    }

    @Test
    @DisplayName("Nộp đơn chỉ rơi vào ngày nghỉ cuối tuần (0 giờ làm việc) -> Ném ngoại lệ InvalidUnavailabilityPeriodException")
    void testSubmitWeekendOnlyThrowsException() {
        Long currentUserId = 10L;
        Long employeeId = 10L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE)).thenReturn(currentUserId);
        User currentUser = mock(User.class);
        when(currentUser.getIdValue()).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));

        Employee employee = new Employee(
                new EmployeeId(employeeId),
                new UserId(currentUserId),
                1L, "EMP001", "Nguyễn Văn A", "DEV",
                LocalDate.of(2023, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        // 2026-09-26 is Saturday, 2026-09-27 is Sunday
        LocalDate start = LocalDate.of(2026, 9, 26);
        LocalDate end = LocalDate.of(2026, 9, 27);

        SubmitUnavailabilityCommand command = new SubmitUnavailabilityCommand(
                employeeId, start, end, UnavailabilityReasonType.TRAINING, "Cuối tuần"
        );

        assertThrows(InvalidUnavailabilityPeriodException.class, () -> service.submit(command));
    }
}
