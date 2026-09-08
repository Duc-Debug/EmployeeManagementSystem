package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.allocation.EmployeeSkillCandidate;
import com.hrm.employeemanagement.application.dto.allocation.ResourceSearchResult;
import com.hrm.employeemanagement.application.dto.allocation.SearchResourceQuery;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SearchResourcePort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class SearchResourceBySkillAndAvailabilityServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private SearchResourcePort searchResourcePort;
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private SaveAuditLogInNewTransactionPort saveAuditLogPort;

    private SearchResourceBySkillAndAvailabilityService service;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        searchResourcePort = mock(SearchResourcePort.class);
        loadWeeklyAvailabilityPort = mock(LoadWeeklyAvailabilityPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        saveAuditLogPort = mock(SaveAuditLogInNewTransactionPort.class);

        service = new SearchResourceBySkillAndAvailabilityService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                searchResourcePort,
                loadWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAuditLogPort
        );
    }

    private User createRMUser() {
        Role rmRole = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        return new User(
                new UserId(1L),
                "rm_user",
                "encoded_pw",
                rmRole,
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                0L
        );
    }

    private Employee createEmployee(Long id, String code, String name) {
        return new Employee(
                new EmployeeId(id),
                new UserId(id),
                10L,
                code,
                name,
                "Developer",
                null,
                null,
                false,
                40,
                EmployeeStatus.ACTIVE,
                0L
        );
    }

    @Test
    @DisplayName("NCL-02-CN-004-TC-01: Có 3 nhân sự cùng kỹ năng, trả về sắp xếp theo mức rảnh giảm dần")
    void testSearch_TC01_SuccessSortByRemainingHoursDescending() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(createRMUser()));

        Employee emp1 = createEmployee(101L, "NV01", "Nguyễn Văn A");
        Employee emp2 = createEmployee(102L, "NV02", "Trần Thị B");
        Employee emp3 = createEmployee(103L, "NV03", "Lê Văn C");

        List<EmployeeSkillCandidate> candidates = List.of(
                new EmployeeSkillCandidate(emp1, 1L, "Java", 3, BigDecimal.valueOf(3)),
                new EmployeeSkillCandidate(emp2, 1L, "Java", 4, BigDecimal.valueOf(4)),
                new EmployeeSkillCandidate(emp3, 1L, "Java", 3, BigDecimal.valueOf(2))
        );
        when(searchResourcePort.findActiveEmployeesBySkill(1L, 3)).thenReturn(candidates);

        YearWeek yw = YearWeek.of(2026, 10);
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(any(), any())).thenReturn(Optional.empty()); // Mặc định 40h

        // NV01: đã bị phân bổ 30h -> rảnh 10h
        when(loadAllocationPort.loadAllocationsForEmployee(101L, yw)).thenReturn(List.of(
                new WeeklyProjectAllocation(1L, 101L, 50L, yw, BigDecimal.valueOf(30))
        ));
        // NV02: chưa bị phân bổ (0h) -> rảnh 40h
        when(loadAllocationPort.loadAllocationsForEmployee(102L, yw)).thenReturn(List.of());
        // NV03: đã bị phân bổ 20h -> rảnh 20h
        when(loadAllocationPort.loadAllocationsForEmployee(103L, yw)).thenReturn(List.of(
                new WeeklyProjectAllocation(2L, 103L, 50L, yw, BigDecimal.valueOf(20))
        ));

        SearchResourceQuery query = new SearchResourceQuery(1L, 3, null, 2026, 10, 2026, 10);
        List<ResourceSearchResult> results = service.search(query);

        assertEquals(3, results.size());
        // Thứ tự mong đợi: NV02 (40h) -> NV03 (20h) -> NV01 (10h)
        assertEquals(102L, results.get(0).employeeId());
        assertEquals(new BigDecimal("40"), results.get(0).totalRemainingHours());

        assertEquals(103L, results.get(1).employeeId());
        assertEquals(new BigDecimal("20"), results.get(1).totalRemainingHours());

        assertEquals(101L, results.get(2).employeeId());
        assertEquals(new BigDecimal("10"), results.get(2).totalRemainingHours());
    }

    @Test
    @DisplayName("NCL-02-CN-004-TC-02: Không ai có kỹ năng được lọc, trả về danh sách rỗng")
    void testSearch_TC02_EmptyResults() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(createRMUser()));
        when(searchResourcePort.findActiveEmployeesBySkill(99L, 5)).thenReturn(List.of());

        SearchResourceQuery query = new SearchResourceQuery(99L, 5, null, 2026, 10, 2026, 10);
        List<ResourceSearchResult> results = service.search(query);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("NCL-02-CN-004-TC-03: Không có quyền truy cập, từ chối và ghi nhật ký")
    void testSearch_TC03_PermissionDeniedLogsAudit() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_SEARCH));

        SearchResourceQuery query = new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10);

        assertThrows(PermissionDeniedException.class, () -> service.search(query));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-02-CN-004-TC-04: Lưu lịch sử thao tác tìm kiếm thành công")
    void testSearch_TC04_AuditLogSavedOnSuccess() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(createRMUser()));
        when(searchResourcePort.findActiveEmployeesBySkill(1L, 2)).thenReturn(List.of());

        SearchResourceQuery query = new SearchResourceQuery(1L, 2, null, 2026, 10, 2026, 10);
        service.search(query);

        verify(saveAuditLogPort).save(any(AuditLog.class));
    }
}
