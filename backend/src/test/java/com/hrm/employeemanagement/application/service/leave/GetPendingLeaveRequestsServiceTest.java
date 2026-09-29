package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("P1-4: GetPendingLeaveRequestsService - Trả tên nhân viên và tên phòng ban")
class GetPendingLeaveRequestsServiceTest {

    @Mock
    private LoadLeaveRequestPort loadLeaveRequestPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private AuthorizationService authorizationService;

    private GetPendingLeaveRequestsService service;

    private User rmUser;

    @BeforeEach
    void setUp() {
        service = new GetPendingLeaveRequestsService(
                loadLeaveRequestPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                authorizationService
        );

        rmUser = new User(
                new UserId(100L),
                "rm_user",
                "hash",
                new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực"),
                UserStatus.ACTIVE,
                new EmployeeId(10L),
                DataScope.ORGANIZATION_BRANCH,
                1L,
                0L
        );
    }

    @Test
    @DisplayName("Nghiệm thu P1-4: Danh sách đơn chờ duyệt trả đúng employeeName và orgUnitName")
    void testGetPendingLeaveRequests_ShouldEnrichEmployeeNameAndOrgUnitName() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(rmUser));

        LeaveRequest lr1 = new LeaveRequest(
                1L,
                5L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 3),
                3,
                BigDecimal.valueOf(24.0),
                "Nghỉ phép cá nhân",
                LeaveStatus.PENDING,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                null
        );

        PageResult<LeaveRequest> pagedRequests = new PageResult<>(List.of(lr1), 0, 10, 1L);
        when(loadLeaveRequestPort.findPendingRequests(eq(DataScope.ORGANIZATION_BRANCH), eq(1L), eq(100L), eq(0), eq(10)))
                .thenReturn(pagedRequests);

        Employee devEmployee = new Employee(
                new EmployeeId(5L),
                new UserId(205L),
                20L,
                "DEV-005",
                "Nguyễn Văn Dev",
                "Developer",
                LocalDate.of(2025, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(5L))))
                .thenReturn(List.of(devEmployee));

        OrgUnit softwareDept = new OrgUnit(
                new OrgUnitId(20L),
                "DEV-DEP",
                "Phòng Phát triển Phần mềm",
                OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L),
                "/1/20/",
                2,
                OrgUnitStatus.ACTIVE,
                "Phòng Dev",
                null,
                LocalDateTime.now(),
                null
        );
        when(loadOrgUnitPort.findAllByIdIn(List.of(20L)))
                .thenReturn(List.of(softwareDept));

        PageResult<LeaveRequestResult> result = service.getPendingLeaveRequests(0, 10);

        assertThat(result.getContent()).hasSize(1);
        LeaveRequestResult item = result.getContent().get(0);
        assertThat(item.id()).isEqualTo(1L);
        assertThat(item.employeeId()).isEqualTo(5L);
        assertThat(item.employeeName()).isEqualTo("Nguyễn Văn Dev");
        assertThat(item.orgUnitName()).isEqualTo("Phòng Phát triển Phần mềm");
    }

    @Test
    @DisplayName("N+1 queries prevention: Nhiều đơn của cùng nhân viên/phòng ban chỉ gọi findAllByIdIn 1 lần")
    void testGetPendingLeaveRequests_MultipleRequests_ShouldBatchQueryDistinctIds() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(rmUser));

        LeaveRequest lr1 = new LeaveRequest(
                1L, 5L, LeaveType.ANNUAL, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2),
                2, BigDecimal.valueOf(16.0), "Đơn 1", LeaveStatus.PENDING, null, null, null, null, LocalDateTime.now(), null
        );
        LeaveRequest lr2 = new LeaveRequest(
                2L, 5L, LeaveType.SICK, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5),
                1, BigDecimal.valueOf(8.0), "Đơn 2", LeaveStatus.PENDING, null, null, null, null, LocalDateTime.now(), null
        );

        PageResult<LeaveRequest> pagedRequests = new PageResult<>(List.of(lr1, lr2), 0, 10, 2L);
        when(loadLeaveRequestPort.findPendingRequests(any(), any(), any(), eq(0), eq(10)))
                .thenReturn(pagedRequests);

        Employee devEmployee = new Employee(
                new EmployeeId(5L), new UserId(205L), 20L, "DEV-005", "Nguyễn Văn Dev",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(5L))))
                .thenReturn(List.of(devEmployee));

        OrgUnit softwareDept = new OrgUnit(
                new OrgUnitId(20L), "DEV-DEP", "Phòng Phát triển Phần mềm", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/20/", 2, OrgUnitStatus.ACTIVE, "Phòng Dev", null, LocalDateTime.now(), null
        );
        when(loadOrgUnitPort.findAllByIdIn(List.of(20L)))
                .thenReturn(List.of(softwareDept));

        PageResult<LeaveRequestResult> result = service.getPendingLeaveRequests(0, 10);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).employeeName()).isEqualTo("Nguyễn Văn Dev");
        assertThat(result.getContent().get(0).orgUnitName()).isEqualTo("Phòng Phát triển Phần mềm");
        assertThat(result.getContent().get(1).employeeName()).isEqualTo("Nguyễn Văn Dev");
        assertThat(result.getContent().get(1).orgUnitName()).isEqualTo("Phòng Phát triển Phần mềm");

        verify(loadEmployeePort, times(1)).findAllByIdIn(any());
        verify(loadOrgUnitPort, times(1)).findAllByIdIn(any());
    }

    @Test
    @DisplayName("Nếu Employee hoặc OrgUnit không tìm thấy, trả về null thay vì crash")
    void testGetPendingLeaveRequests_MissingEmployeeOrOrgUnit_ShouldReturnNullGracefully() {
        when(authorizationService.require(PermissionCode.LEAVE_REQUEST_APPROVE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(rmUser));

        LeaveRequest lr1 = new LeaveRequest(
                1L, 99L, LeaveType.ANNUAL, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2),
                2, BigDecimal.valueOf(16.0), "Đơn nhân viên lạ", LeaveStatus.PENDING, null, null, null, null, LocalDateTime.now(), null
        );

        when(loadLeaveRequestPort.findPendingRequests(any(), any(), any(), eq(0), eq(10)))
                .thenReturn(new PageResult<>(List.of(lr1), 0, 10, 1L));
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(99L))))
                .thenReturn(List.of());

        PageResult<LeaveRequestResult> result = service.getPendingLeaveRequests(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).employeeName()).isNull();
        assertThat(result.getContent().get(0).orgUnitName()).isNull();
    }
}
