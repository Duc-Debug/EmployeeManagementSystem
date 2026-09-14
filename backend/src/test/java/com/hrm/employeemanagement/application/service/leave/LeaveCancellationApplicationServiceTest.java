package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.leave.PastLeaveCancellationException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Use Case: Cancel Approved Leave Request - Application Service Tests")
class LeaveCancellationApplicationServiceTest {

    private static final Clock BUSINESS_CLOCK = Clock.fixed(
            Instant.parse("2026-09-14T05:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDate TODAY = LocalDate.now(BUSINESS_CLOCK);

    @Mock
    private LoadLeaveRequestPort loadLeaveRequestPort;

    @Mock
    private SaveLeaveRequestPort saveLeaveRequestPort;

    @Mock
    private SaveLeaveAuditLogPort saveLeaveAuditLogPort;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;

    @Mock
    private SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort;

    @Mock
    private LoadHolidaysPort loadHolidaysPort;

    @Mock
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    @Mock
    private LoadWorkingCalendarPort loadWorkingCalendarPort;

    @Mock
    private LoadWeeklyProjectAllocationPort loadWeeklyProjectAllocationPort;

    @Mock
    private SaveWeeklyProjectAllocationPort saveWeeklyProjectAllocationPort;

    private RequestCancelApprovedLeaveService requestCancelService;
    private ApproveCancelLeaveRequestService approveCancelService;
    private RejectCancelLeaveRequestService rejectCancelService;

    private final Long userId = 1L;
    private final Long employeeId = 10L;
    private final Long approverUserId = 99L;
    private final LocalDate futureStartDate = TODAY.plusDays(5);
    private final LocalDate futureEndDate = TODAY.plusDays(6);

    @BeforeEach
    void setUp() {
        requestCancelService = new RequestCancelApprovedLeaveService(
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                loadEmployeePort,
                saveLeaveAuditLogPort,
                authorizationService,
                BUSINESS_CLOCK
        );

        approveCancelService = createApproveCancelService(
                loadWorkingCalendarPort,
                loadWeeklyProjectAllocationPort,
                saveWeeklyProjectAllocationPort
        );

        rejectCancelService = new RejectCancelLeaveRequestService(
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                saveLeaveAuditLogPort,
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadEmployeePort
        );
    }

    private ApproveCancelLeaveRequestService createApproveCancelService(
            LoadWorkingCalendarPort workingCalendarPort,
            LoadWeeklyProjectAllocationPort weeklyProjectAllocationPort,
            SaveWeeklyProjectAllocationPort weeklyProjectAllocationSavePort) {
        return new ApproveCancelLeaveRequestService(
                loadLeaveRequestPort,
                saveLeaveRequestPort,
                saveLeaveAuditLogPort,
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadEmployeePort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                workingCalendarPort,
                weeklyProjectAllocationPort,
                weeklyProjectAllocationSavePort,
                BUSINESS_CLOCK
        );
    }

    @Test
    @DisplayName("Fail-fast khi thiếu working calendar integration")
    void approveCancelLeaveRequest_MissingWorkingCalendarPort_FailsFast() {
        assertThatThrownBy(() -> createApproveCancelService(
                null, loadWeeklyProjectAllocationPort, saveWeeklyProjectAllocationPort))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("loadWorkingCalendarPort must not be null");
    }

    @Test
    @DisplayName("Fail-fast khi thiếu allocation loading integration")
    void approveCancelLeaveRequest_MissingAllocationLoadPort_FailsFast() {
        assertThatThrownBy(() -> createApproveCancelService(
                loadWorkingCalendarPort, null, saveWeeklyProjectAllocationPort))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("loadWeeklyProjectAllocationPort must not be null");
    }

    @Test
    @DisplayName("Fail-fast khi thiếu allocation saving integration")
    void approveCancelLeaveRequest_MissingAllocationSavePort_FailsFast() {
        assertThatThrownBy(() -> createApproveCancelService(
                loadWorkingCalendarPort, loadWeeklyProjectAllocationPort, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("saveWeeklyProjectAllocationPort must not be null");
    }

    private LeaveRequest createApprovedLeave(LocalDate startDate, LocalDate endDate) {
        LeaveRequest req = LeaveRequest.createPending(
                employeeId,
                LeaveType.ANNUAL,
                startDate,
                endDate,
                2,
                BigDecimal.valueOf(16.0),
                "Kế hoạch cá nhân"
        );
        req.approve(approverUserId, "Đã duyệt nghỉ");
        return req;
    }

    private Employee createMockEmployee(Long empId, Long uId) {
        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(empId);
        when(employee.getUserIdValue()).thenReturn(uId);
        when(employee.getStandardHoursPerWeek()).thenReturn(40);
        return employee;
    }

    private User createMockApproverUser(Long uId) {
        User user = mock(User.class);
        when(user.getIdValue()).thenReturn(uId);
        when(user.getDataScope()).thenReturn(DataScope.COMPANY);
        return user;
    }

    // -------------------------------------------------------------
    // RequestCancelApprovedLeaveService Tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("TC-01: Nhân viên yêu cầu hủy đơn đã duyệt trong tương lai -> Thành công, chuyển sang CANCEL_REQUESTED")
    void requestCancelApprovedLeave_Success() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE)).thenReturn(userId);
        Employee employee = createMockEmployee(employeeId, userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(employee));

        LeaveRequest approvedLeave = createApprovedLeave(futureStartDate, futureEndDate);
        when(loadLeaveRequestPort.findByIdForUpdate(1L)).thenReturn(Optional.of(approvedLeave));
        when(saveLeaveRequestPort.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequestResult result = requestCancelService.requestCancelApprovedLeave(1L, "Thay đổi kế hoạch gia đình");

        assertThat(result.status()).isEqualTo(LeaveStatus.CANCEL_REQUESTED);
        assertThat(result.cancellationReason()).isEqualTo("Thay đổi kế hoạch gia đình");
        assertThat(result.cancellationRequestedAt()).isNotNull();

        verify(saveLeaveRequestPort).save(approvedLeave);
        verify(saveLeaveAuditLogPort).recordAudit(eq(userId), eq("REQUEST_CANCEL_LEAVE_REQUEST"), contains("Gửi yêu cầu hủy đơn nghỉ phép"));
    }

    @Test
    @DisplayName("TC-02: Không thể yêu cầu hủy nếu ngày nghỉ đã diễn ra hoặc là hôm nay -> Ném PastLeaveCancellationException")
    void requestCancelApprovedLeave_PastOrToday_ThrowsException() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE)).thenReturn(userId);
        Employee employee = createMockEmployee(employeeId, userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(employee));

        // Leave starts today
        LeaveRequest approvedLeave = createApprovedLeave(TODAY, TODAY.plusDays(1));
        when(loadLeaveRequestPort.findByIdForUpdate(1L)).thenReturn(Optional.of(approvedLeave));

        assertThatThrownBy(() -> requestCancelService.requestCancelApprovedLeave(1L, "Muốn hủy hôm nay"))
                .isInstanceOf(PastLeaveCancellationException.class);

        verify(saveLeaveRequestPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Nhân viên không sở hữu đơn nghỉ -> Bị từ chối với PermissionDeniedException")
    void requestCancelApprovedLeave_NotOwner_ThrowsException() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE)).thenReturn(userId);
        Employee otherEmployee = createMockEmployee(999L, userId); // Different employee ID
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(otherEmployee));

        LeaveRequest approvedLeave = createApprovedLeave(futureStartDate, futureEndDate);
        when(loadLeaveRequestPort.findByIdForUpdate(1L)).thenReturn(Optional.of(approvedLeave));

        assertThatThrownBy(() -> requestCancelService.requestCancelApprovedLeave(1L, "Hủy đơn người khác"))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveLeaveRequestPort, never()).save(any());
    }

    // -------------------------------------------------------------
    // ApproveCancelLeaveRequestService Tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("TC-04: Quản lý phê duyệt hủy đơn -> Thành công, chuyển sang CANCELLED, hoàn trả capacity")
    void approveCancelLeaveRequest_Success() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(approverUserId);
        Employee employee = createMockEmployee(employeeId, userId);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        User approver = createMockApproverUser(approverUserId);
        when(loadUserPort.findById(new UserId(approverUserId))).thenReturn(Optional.of(approver));

        LeaveRequest req = createApprovedLeave(futureStartDate, futureEndDate);
        req.requestCancellation("Bận việc đột xuất", TODAY);
        when(loadLeaveRequestPort.findEmployeeIdById(1L)).thenReturn(Optional.of(employeeId));
        when(loadLeaveRequestPort.findByIdForUpdate(1L)).thenReturn(Optional.of(req));
        when(saveLeaveRequestPort.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        CompanyWorkingCalendar calendar = mock(CompanyWorkingCalendar.class);
        when(calendar.getWorkingDays()).thenReturn(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
        when(loadWorkingCalendarPort.loadCompanyCalendar()).thenReturn(calendar);
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(Collections.emptyList());
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(any(), any())).thenReturn(Optional.empty());
        when(loadWeeklyProjectAllocationPort.loadAllocationsForEmployee(any(), any())).thenReturn(Collections.emptyList());

        LeaveRequestResult result = approveCancelService.approveCancelLeaveRequest(1L, "Đồng ý cho hủy đơn");

        assertThat(result.status()).isEqualTo(LeaveStatus.CANCELLED);
        InOrder lockOrder = inOrder(loadEmployeePort, loadLeaveRequestPort);
        lockOrder.verify(loadEmployeePort).findByIdForUpdate(new EmployeeId(employeeId));
        lockOrder.verify(loadLeaveRequestPort).findByIdForUpdate(1L);
        verify(saveWeeklyAvailabilityPort, atLeastOnce()).save(any());
        verify(saveLeaveRequestPort).save(req);
        verify(saveLeaveAuditLogPort).recordAudit(eq(approverUserId), eq("APPROVE_CANCEL_LEAVE_REQUEST"), contains("Duyệt hủy đơn nghỉ phép"));
    }

    @Test
    @DisplayName("TC-05: Quản lý duyệt hủy nhưng ngày nghỉ đã tới ngày hoặc quá khứ -> Ném PastLeaveCancellationException")
    void approveCancelLeaveRequest_PastDate_ThrowsException() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(approverUserId);
        Employee employee = createMockEmployee(employeeId, userId);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        User approver = createMockApproverUser(approverUserId);
        when(loadUserPort.findById(new UserId(approverUserId))).thenReturn(Optional.of(approver));

        // Created as future, but now simulated as starting today
        LeaveRequest req = createApprovedLeave(TODAY, TODAY.plusDays(1));
        // Force status to CANCEL_REQUESTED using reflection or earlier date request
        req.requestCancellation("Hủy", TODAY.minusDays(1));
        when(loadLeaveRequestPort.findEmployeeIdById(1L)).thenReturn(Optional.of(employeeId));
        when(loadLeaveRequestPort.findByIdForUpdate(1L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> approveCancelService.approveCancelLeaveRequest(1L, "Duyệt"))
                .isInstanceOf(PastLeaveCancellationException.class);

        verify(saveLeaveRequestPort, never()).save(any());
        verify(saveWeeklyAvailabilityPort, never()).save(any());
    }

    // -------------------------------------------------------------
    // RejectCancelLeaveRequestService Tests
    // -------------------------------------------------------------

    @Test
    @DisplayName("TC-06: Quản lý từ chối yêu cầu hủy -> Chuyển lại trạng thái APPROVED kèm lý do từ chối")
    void rejectCancelLeaveRequest_Success() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(approverUserId);
        Employee employee = createMockEmployee(employeeId, userId);
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        User approver = createMockApproverUser(approverUserId);
        when(loadUserPort.findById(new UserId(approverUserId))).thenReturn(Optional.of(approver));

        LeaveRequest req = createApprovedLeave(futureStartDate, futureEndDate);
        req.requestCancellation("Lý do cá nhân", TODAY);
        when(loadLeaveRequestPort.findByIdForUpdate(1L)).thenReturn(Optional.of(req));
        when(saveLeaveRequestPort.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequestResult result = rejectCancelService.rejectCancelLeaveRequest(1L, "Dự án đang gấp rút, không thể hủy");

        assertThat(result.status()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(result.approverComment()).isEqualTo("Dự án đang gấp rút, không thể hủy");

        verify(saveLeaveRequestPort).save(req);
        verify(saveLeaveAuditLogPort).recordAudit(eq(approverUserId), eq("REJECT_CANCEL_LEAVE_REQUEST"), contains("Từ chối yêu cầu hủy đơn"));
    }

    @Test
    @DisplayName("TC-07: Từ chối hủy trên đơn không ở trạng thái CANCEL_REQUESTED -> Ném IllegalStateException")
    void rejectCancelLeaveRequest_InvalidStatus_ThrowsException() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(approverUserId);
        Employee employee = createMockEmployee(employeeId, userId);
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        User approver = createMockApproverUser(approverUserId);
        when(loadUserPort.findById(new UserId(approverUserId))).thenReturn(Optional.of(approver));

        LeaveRequest req = createApprovedLeave(futureStartDate, futureEndDate); // State is APPROVED, not CANCEL_REQUESTED
        when(loadLeaveRequestPort.findByIdForUpdate(1L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> rejectCancelService.rejectCancelLeaveRequest(1L, "Từ chối"))
                .isInstanceOf(IllegalStateException.class);

        verify(saveLeaveRequestPort, never()).save(any());
    }
}
