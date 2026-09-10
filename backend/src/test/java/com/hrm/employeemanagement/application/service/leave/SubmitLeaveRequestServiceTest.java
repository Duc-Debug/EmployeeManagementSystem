package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.leave.SubmitLeaveRequestCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.leave.DuplicateLeaveRequestException;
import com.hrm.employeemanagement.domain.exception.leave.InvalidLeaveDateRangeException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitLeaveRequestServiceTest {

    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadLeaveRequestPort loadLeaveRequestPort;
    @Mock
    private SaveLeaveRequestPort saveLeaveRequestPort;
    @Mock
    private SaveAuditLogInNewTransactionPort auditLogRepository;
    @Mock
    private AuthorizationService authorizationService;

    private SubmitLeaveRequestService service;

    @BeforeEach
    void setUp() {
        service = new SubmitLeaveRequestService(
                loadEmployeePort,
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                auditLogRepository,
                authorizationService
        );
    }

    private Employee createMockEmployee(Long id, Long userId) {
        return new Employee(
                new EmployeeId(id),
                new UserId(userId),
                1L,
                "EMP001",
                "Nguyen Van A",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("TC-01 & TC-05: Nộp đơn hợp lệ -> PENDING, 16h nghỉ, ghi audit log CREATE_LEAVE_REQUEST")
    void testSubmitLeaveRequest_Success() {
        Long empId = 10L;
        Long userId = 100L;
        LocalDate start = LocalDate.of(2026, 4, 13); // Thứ 2
        LocalDate end = LocalDate.of(2026, 4, 14);   // Thứ 3 (2 ngày = 16h)

        SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                empId,
                LeaveType.ANNUAL,
                start,
                end,
                "Xin nghỉ phép thường niên"
        );

        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee(empId, userId)));
        when(loadLeaveRequestPort.existsOverlappingLeave(empId, start, end)).thenReturn(false);
        when(saveLeaveRequestPort.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest req = invocation.getArgument(0);
            return new LeaveRequest(
                    99L, req.getEmployeeId(), req.getLeaveType(),
                    req.getStartDate(), req.getEndDate(), req.getDaysCount(),
                    req.getHoursDeducted(), req.getReason(), req.getStatus(),
                    LocalDateTime.now(), LocalDateTime.now()
            );
        });

        LeaveRequestResult result = service.submitLeaveRequest(command);

        assertNotNull(result);
        assertEquals(99L, result.id());
        assertEquals(LeaveStatus.PENDING, result.status());
        assertEquals(new BigDecimal("16.00"), result.hoursDeducted());

        // Kiểm tra TC-05: Audit log được ghi
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(auditCaptor.capture());
        assertEquals("CREATE_LEAVE_REQUEST", auditCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("TC-02: Bị từ chối khi khoảng ngày bị trùng lặp -> DuplicateLeaveRequestException")
    void testSubmitLeaveRequest_DuplicateDates() {
        Long empId = 10L;
        Long userId = 100L;
        LocalDate start = LocalDate.of(2026, 4, 13);
        LocalDate end = LocalDate.of(2026, 4, 14);

        SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                empId, LeaveType.ANNUAL, start, end, "Trùng ngày"
        );

        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee(empId, userId)));
        when(loadLeaveRequestPort.existsOverlappingLeave(empId, start, end)).thenReturn(true);

        assertThrows(DuplicateLeaveRequestException.class, () -> service.submitLeaveRequest(command));
        verify(saveLeaveRequestPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Ngày kết thúc trước ngày bắt đầu -> InvalidLeaveDateRangeException")
    void testSubmitLeaveRequest_InvalidDateRange() {
        Long empId = 10L;
        Long userId = 100L;
        LocalDate start = LocalDate.of(2026, 4, 15);
        LocalDate end = LocalDate.of(2026, 4, 10);

        SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                empId, LeaveType.ANNUAL, start, end, "Sai ngày"
        );

        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee(empId, userId)));

        assertThrows(InvalidLeaveDateRangeException.class, () -> service.submitLeaveRequest(command));
        verify(saveLeaveRequestPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Nộp đơn hộ cho nhân viên khác -> PermissionDeniedException")
    void testSubmitLeaveRequest_AccessDenied_WhenNotSelf() {
        Long empId = 10L;
        Long otherEmpId = 20L;
        Long userId = 100L;
        LocalDate start = LocalDate.of(2026, 4, 13);
        LocalDate end = LocalDate.of(2026, 4, 14);

        SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                otherEmpId, LeaveType.ANNUAL, start, end, "Nộp hộ"
        );

        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee(empId, userId)));

        assertThrows(PermissionDeniedException.class, () -> service.submitLeaveRequest(command));
        verify(saveLeaveRequestPort, never()).save(any());
    }
}
