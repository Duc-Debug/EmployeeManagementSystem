package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveImpactResult;
import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadProjectAllocationForLeavePort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.leave.LeaveRequestNotFoundException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NCL-05-CN-003: Leave Approval & Impact Application Service Tests")
class LeaveApprovalApplicationServiceTest {

    @Mock
    private LoadLeaveRequestPort loadLeaveRequestPort;

    @Mock
    private SaveLeaveRequestPort saveLeaveRequestPort;

    @Mock
    private SaveLeaveAuditLogPort saveLeaveAuditLogPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadProjectAllocationForLeavePort loadProjectAllocationPort;

    @Mock
    private AuthorizationService authorizationService;

    private ApproveLeaveRequestService approveService;
    private RejectLeaveRequestService rejectService;
    private GetLeaveImpactService impactService;

    @BeforeEach
    void setUp() {
        approveService = new ApproveLeaveRequestService(loadLeaveRequestPort, saveLeaveRequestPort, saveLeaveAuditLogPort, authorizationService);
        rejectService = new RejectLeaveRequestService(loadLeaveRequestPort, saveLeaveRequestPort, saveLeaveAuditLogPort, authorizationService);
        impactService = new GetLeaveImpactService(loadLeaveRequestPort, loadEmployeePort, loadProjectAllocationPort, authorizationService);
    }

    private LeaveRequest createSamplePendingRequest() {
        return LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Xin nghỉ thường niên"
        );
    }

    @Test
    @DisplayName("TC-01: Duyệt đơn thành công -> Lưu DB trạng thái APPROVED và ghi Audit Log")
    void approveLeaveRequest_Success() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(99L);
        LeaveRequest sample = createSamplePendingRequest();
        when(loadLeaveRequestPort.findById(1L)).thenReturn(Optional.of(sample));
        when(saveLeaveRequestPort.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestResult result = approveService.approveLeaveRequest(1L, "Đã duyệt");

        assertThat(result.status()).isEqualTo(LeaveStatus.APPROVED);
        verify(saveLeaveRequestPort).save(sample);
        verify(saveLeaveAuditLogPort).recordAudit(eq(99L), eq("APPROVE_LEAVE_REQUEST"), contains("Phê duyệt đơn nghỉ phép"));
    }

    @Test
    @DisplayName("TC-03: Không có quyền LEAVE_REQUEST_APPROVE -> Bị chặn PermissionDeniedException")
    void approveLeaveRequest_AccessDenied() {
        doThrow(new PermissionDeniedException(PermissionCode.LEAVE_REQUEST_APPROVE))
                .when(authorizationService).require(PermissionCode.LEAVE_REQUEST_APPROVE);

        assertThatThrownBy(() -> approveService.approveLeaveRequest(1L, "Duyệt"))
                .isInstanceOf(PermissionDeniedException.class);

        verifyNoInteractions(loadLeaveRequestPort);
        verifyNoInteractions(saveLeaveRequestPort);
    }

    @Test
    @DisplayName("TC-04: Từ chối đơn kèm lý do -> Lưu DB trạng thái REJECTED và ghi Audit Log")
    void rejectLeaveRequest_Success() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(99L);
        LeaveRequest sample = createSamplePendingRequest();
        when(loadLeaveRequestPort.findById(1L)).thenReturn(Optional.of(sample));
        when(saveLeaveRequestPort.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequestResult result = rejectService.rejectLeaveRequest(1L, "Trùng lịch release sản phẩm");

        assertThat(result.status()).isEqualTo(LeaveStatus.REJECTED);
        verify(saveLeaveRequestPort).save(sample);
        verify(saveLeaveAuditLogPort).recordAudit(eq(99L), eq("REJECT_LEAVE_REQUEST"), contains("Từ chối đơn nghỉ phép"));
    }

    @Test
    @DisplayName("TC-02: Đánh giá ảnh hưởng dự án khi có 30h phân bổ trong tuần nghỉ -> hasConflict = true")
    void getLeaveImpact_WithAllocationConflict() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(99L);
        LeaveRequest sample = createSamplePendingRequest();
        when(loadLeaveRequestPort.findById(1L)).thenReturn(Optional.of(sample));

        Employee employee = mock(Employee.class);
        when(employee.getFullName()).thenReturn("Nguyễn Văn A");
        when(loadEmployeePort.findById(new EmployeeId(10L))).thenReturn(Optional.of(employee));

        // Giả lập nhân viên đang có 30h phân bổ trong tuần này
        when(loadProjectAllocationPort.findAllocations(eq(10L), eq(2026), anyList()))
                .thenReturn(List.of(
                        new LoadProjectAllocationForLeavePort.ProjectAllocationInfo(
                                101L, "Dự án Alpha", 2026, 45, BigDecimal.valueOf(30.00)
                        )
                ));

        LeaveImpactResult impact = impactService.getLeaveImpact(1L);

        assertThat(impact.hasConflict()).isTrue();
        assertThat(impact.totalAllocatedHoursInLeavePeriod()).isEqualByComparingTo(BigDecimal.valueOf(30.00));
        assertThat(impact.affectedProjects()).hasSize(1);
        assertThat(impact.affectedProjects().get(0).projectName()).isEqualTo("Dự án Alpha");
    }

    @Test
    @DisplayName("TC-01: Đánh giá ảnh hưởng khi không có phân bổ nào trong tuần nghỉ -> hasConflict = false")
    void getLeaveImpact_WithoutAllocationConflict() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(99L);
        LeaveRequest sample = createSamplePendingRequest();
        when(loadLeaveRequestPort.findById(1L)).thenReturn(Optional.of(sample));

        when(loadEmployeePort.findById(new EmployeeId(10L))).thenReturn(Optional.empty());
        when(loadProjectAllocationPort.findAllocations(eq(10L), eq(2026), anyList()))
                .thenReturn(List.of());

        LeaveImpactResult impact = impactService.getLeaveImpact(1L);

        assertThat(impact.hasConflict()).isFalse();
        assertThat(impact.totalAllocatedHoursInLeavePeriod()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(impact.affectedProjects()).isEmpty();
    }
}
