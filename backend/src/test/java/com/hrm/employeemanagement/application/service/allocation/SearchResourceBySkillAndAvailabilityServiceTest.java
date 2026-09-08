package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.hrm.employeemanagement.application.dto.allocation.ResourceCandidate;
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
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.skill.SkillNotFoundException;
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
        return createUserWithScope(1L, DataScope.COMPANY, null);
    }

    private User createUserWithScope(Long userId, DataScope dataScope, Long scopeOrgUnitId) {
        Role rmRole = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        return new User(
                new UserId(userId),
                "user_" + userId,
                "encoded_pw",
                rmRole,
                UserStatus.ACTIVE,
                null,
                dataScope,
                scopeOrgUnitId,
                0L
        );
    }

    private ResourceCandidate createCandidate(Long id, Long userId, Long orgUnitId, String code, String name) {
        return new ResourceCandidate(
                id,
                userId,
                code,
                name,
                orgUnitId,
                "Developer",
                40,
                null,
                1L,
                "Java",
                3,
                BigDecimal.valueOf(3)
        );
    }

    @Test
    @DisplayName("NCL-02-CN-004-TC-01: Có 3 nhân sự cùng kỹ năng, trả về sắp xếp theo mức rảnh giảm dần")
    void testSearch_TC01_SuccessSortByRemainingHoursDescending() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(createRMUser()));

        ResourceCandidate c1 = createCandidate(101L, 101L, 10L, "NV01", "Nguyễn Văn A");
        ResourceCandidate c2 = createCandidate(102L, 102L, 10L, "NV02", "Trần Thị B");
        ResourceCandidate c3 = createCandidate(103L, 103L, 10L, "NV03", "Lê Văn C");

        when(searchResourcePort.findActiveEmployeesBySkill(1L, 3)).thenReturn(List.of(c1, c2, c3));

        YearWeek yw = YearWeek.of(2026, 10);
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of()); // Mặc định 40h

        // NV01: đã bị phân bổ 30h -> rảnh 10h
        // NV02: chưa bị phân bổ (0h) -> rảnh 40h
        // NV03: đã bị phân bổ 20h -> rảnh 20h
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(
                new WeeklyProjectAllocation(1L, 101L, 50L, yw, BigDecimal.valueOf(30)),
                new WeeklyProjectAllocation(2L, 103L, 50L, yw, BigDecimal.valueOf(20))
        ));

        SearchResourceQuery query = new SearchResourceQuery(1L, 3, null, 2026, 10, 2026, 10);
        List<ResourceSearchResult> results = service.search(query);

        assertEquals(3, results.size());
        // Thứ tự mong đợi: NV02 (40h) -> NV03 (20h) -> NV01 (10h)
        assertEquals(102L, results.get(0).employeeId());
        assertEquals(0, new BigDecimal("40").compareTo(results.get(0).totalRemainingHours()));

        assertEquals(103L, results.get(1).employeeId());
        assertEquals(0, new BigDecimal("20").compareTo(results.get(1).totalRemainingHours()));

        assertEquals(101L, results.get(2).employeeId());
        assertEquals(0, new BigDecimal("10").compareTo(results.get(2).totalRemainingHours()));
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

    @Test
    @DisplayName("Edge case 1: fromWeek > toWeek ném IllegalArgumentException")
    void testSearch_InvalidWeekRange_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SearchResourceQuery(1L, 1, null, 2026, 20, 2026, 10));
        assertTrue(ex.getMessage().contains("Khoảng thời gian bắt đầu phải nhỏ hơn hoặc bằng"));
    }

    @Test
    @DisplayName("Edge case 2: Cross-year range 2026-W52 đến 2027-W02 chạy thành công")
    void testSearch_CrossYearRange_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(createRMUser()));

        ResourceCandidate c1 = createCandidate(101L, 101L, 10L, "NV01", "Nguyễn Văn A");
        when(searchResourcePort.findActiveEmployeesBySkill(1L, 1)).thenReturn(List.of(c1));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        // 2026 có 53 tuần: 2026-W52, 2026-W53, 2027-W01, 2027-W02 -> 4 tuần
        SearchResourceQuery query = new SearchResourceQuery(1L, 1, null, 2026, 52, 2027, 2);
        List<ResourceSearchResult> results = service.search(query);

        assertEquals(1, results.size());
        assertEquals(4, results.get(0).weeklyAvailabilities().size());
        assertEquals(0, new BigDecimal("160").compareTo(results.get(0).totalRemainingHours()));
    }

    @Test
    @DisplayName("Edge case 3: Năm không có tuần 53 ném IllegalArgumentException")
    void testSearch_InvalidWeek53_ThrowsIllegalArgumentException() {
        // Năm 2025 chỉ có 52 tuần ISO
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SearchResourceQuery(1L, 1, null, 2025, 53, 2025, 53));
        assertTrue(ex.getMessage().contains("chỉ có 52 tuần"));
    }

    @Test
    @DisplayName("Edge case 4: Hợp đồng kết thúc trước tuần mục tiêu -> số giờ rảnh còn lại = 0")
    void testSearch_ContractExpired_RemainingIsZero() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(createRMUser()));

        YearWeek yw = YearWeek.of(2026, 15);
        LocalDate expiredDate = yw.getStartDate().minusDays(2); // Hết hạn trước thứ Hai của tuần 15

        ResourceCandidate expiredCandidate = new ResourceCandidate(
                101L, 101L, "NV01", "Nguyễn Văn A", 10L, "Developer", 40,
                expiredDate, 1L, "Java", 3, BigDecimal.valueOf(3)
        );

        when(searchResourcePort.findActiveEmployeesBySkill(1L, 1)).thenReturn(List.of(expiredCandidate));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        SearchResourceQuery query = new SearchResourceQuery(1L, 1, null, 2026, 15, 2026, 15);
        List<ResourceSearchResult> results = service.search(query);

        assertEquals(1, results.size());
        assertEquals(0, BigDecimal.ZERO.compareTo(results.get(0).totalRemainingHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(results.get(0).weeklyAvailabilities().get(0).netAvailableHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(results.get(0).weeklyAvailabilities().get(0).remainingHours()));
    }

    @Test
    @DisplayName("Edge case 5: Over-allocation (gán 60h trên 40h chuẩn) -> số giờ rảnh còn lại bị chặn ở 0 thay vì âm")
    void testSearch_OverAllocation_RemainingClampedToZero() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(createRMUser()));

        ResourceCandidate c1 = createCandidate(101L, 101L, 10L, "NV01", "Nguyễn Văn A");
        when(searchResourcePort.findActiveEmployeesBySkill(1L, 1)).thenReturn(List.of(c1));

        YearWeek yw = YearWeek.of(2026, 10);
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of()); // 40h chuẩn
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(
                new WeeklyProjectAllocation(1L, 101L, 50L, yw, BigDecimal.valueOf(60)) // Gán 60h > 40h
        ));

        SearchResourceQuery query = new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10);
        List<ResourceSearchResult> results = service.search(query);

        assertEquals(1, results.size());
        assertEquals(0, BigDecimal.ZERO.compareTo(results.get(0).totalRemainingHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(results.get(0).weeklyAvailabilities().get(0).remainingHours()));
    }

    @Test
    @DisplayName("Edge case 6: Scope ORGANIZATION_BRANCH chỉ lọc nhân sự thuộc nhánh phòng ban")
    void testSearch_OrganizationBranchScope_FiltersCorrectly() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        User branchUser = createUserWithScope(1L, DataScope.ORGANIZATION_BRANCH, 10L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(branchUser));

        ResourceCandidate c1 = createCandidate(101L, 101L, 11L, "NV01", "Nhân viên trong nhánh");
        ResourceCandidate c2 = createCandidate(102L, 102L, 20L, "NV02", "Nhân viên ngoài nhánh");

        when(searchResourcePort.findActiveEmployeesBySkill(1L, 1)).thenReturn(List.of(c1, c2));
        when(loadOrgUnitPort.existsInOrgUnitBranch(11L, 10L)).thenReturn(true);
        when(loadOrgUnitPort.existsInOrgUnitBranch(20L, 10L)).thenReturn(false);

        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        SearchResourceQuery query = new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10);
        List<ResourceSearchResult> results = service.search(query);

        assertEquals(1, results.size());
        assertEquals(101L, results.get(0).employeeId());
    }

    @Test
    @DisplayName("Edge case 6b: Scope ORGANIZATION_BRANCH yêu cầu orgUnitId ngoài nhánh bị từ chối và ghi audit log")
    void testSearch_OrganizationBranchScope_RequestingOrgUnitOutsideBranch_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(1L);
        User branchUser = createUserWithScope(1L, DataScope.ORGANIZATION_BRANCH, 10L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(branchUser));
        when(loadOrgUnitPort.existsInOrgUnitBranch(99L, 10L)).thenReturn(false);

        SearchResourceQuery query = new SearchResourceQuery(1L, 1, 99L, 2026, 10, 2026, 10);
        assertThrows(PermissionDeniedException.class, () -> service.search(query));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Edge case 7: Scope SELF chỉ cho phép xem chính mình (match userId)")
    void testSearch_SelfScope_FiltersCorrectly() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(500L);
        User selfUser = createUserWithScope(500L, DataScope.SELF, null);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(selfUser));

        ResourceCandidate c1 = createCandidate(101L, 500L, 10L, "NV01", "Chính người dùng");
        ResourceCandidate c2 = createCandidate(102L, 999L, 10L, "NV02", "Người dùng khác");

        when(searchResourcePort.findActiveEmployeesBySkill(1L, 1)).thenReturn(List.of(c1, c2));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        SearchResourceQuery query = new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10);
        List<ResourceSearchResult> results = service.search(query);

        assertEquals(1, results.size());
        assertEquals(101L, results.get(0).employeeId());
    }

    @Test
    @DisplayName("Edge case 7b: Scope SELF cố tình lọc theo orgUnitId bị từ chối và ghi audit log")
    void testSearch_SelfScope_RequestingOrgUnit_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(500L);
        User selfUser = createUserWithScope(500L, DataScope.SELF, null);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(selfUser));

        SearchResourceQuery query = new SearchResourceQuery(1L, 1, 10L, 2026, 10, 2026, 10);
        assertThrows(PermissionDeniedException.class, () -> service.search(query));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Edge case 8: Kiểm tra biên chính xác 52 tuần được chấp nhận, 53 tuần bị từ chối")
    void testSearch_BoundaryWeekCount_52WeeksPass_53WeeksReject() {
        // 2026-W01 đến 2026-W52: đúng 52 tuần -> hợp lệ
        SearchResourceQuery query52 = new SearchResourceQuery(1L, 1, null, 2026, 1, 2026, 52);
        assertEquals(52, query52.toWeek());

        // 2026-W01 đến 2026-W53: đúng 53 tuần -> bị từ chối
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SearchResourceQuery(1L, 1, null, 2026, 1, 2026, 53)
        );
        assertTrue(ex.getMessage().contains("Khoảng thời gian tìm kiếm không được vượt quá 52 tuần"));
    }

    @Test
    @DisplayName("Edge case 9: Phân trang page=0 size=2 và page=1 size=2 hoạt động chính xác")
    void testSearch_Pagination_ReturnsCorrectPageAndSize() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(100L);
        User globalUser = createUserWithScope(100L, DataScope.COMPANY, null);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(globalUser));

        ResourceCandidate c1 = createCandidate(101L, 1L, 10L, "NV01", "A");
        ResourceCandidate c2 = createCandidate(102L, 2L, 10L, "NV02", "B");
        ResourceCandidate c3 = createCandidate(103L, 3L, 10L, "NV03", "C");

        when(searchResourcePort.findActiveEmployeesBySkill(1L, 1)).thenReturn(List.of(c1, c2, c3));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        // Page 0, size 2 -> should return first 2
        SearchResourceQuery page0Query = new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10, 0, 2);
        List<ResourceSearchResult> page0 = service.search(page0Query);
        assertEquals(2, page0.size());
        assertEquals(101L, page0.get(0).employeeId());
        assertEquals(102L, page0.get(1).employeeId());

        // Page 1, size 2 -> should return last 1
        SearchResourceQuery page1Query = new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10, 1, 2);
        List<ResourceSearchResult> page1 = service.search(page1Query);
        assertEquals(1, page1.size());
        assertEquals(103L, page1.get(0).employeeId());
    }

    @Test
    @DisplayName("Edge case 10: Skill không tồn tại ném SkillNotFoundException")
    void testSearch_SkillNotFound_ThrowsSkillNotFoundException() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(100L);
        User globalUser = createUserWithScope(100L, DataScope.COMPANY, null);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(globalUser));

        when(searchResourcePort.findActiveEmployeesBySkill(999L, 1))
                .thenThrow(new SkillNotFoundException("Không tìm thấy kỹ năng với ID: 999"));

        SearchResourceQuery query = new SearchResourceQuery(999L, 1, null, 2026, 10, 2026, 10);
        assertThrows(SkillNotFoundException.class, () -> service.search(query));
    }

    @Test
    @DisplayName("Edge case 11: size > 100 ném IllegalArgumentException")
    void testSearch_SizeGreaterThan100_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10, 0, 101)
        );
        assertTrue(ex.getMessage().contains("Kích thước trang không được vượt quá 100"));
    }

    @Test
    @DisplayName("Edge case 12: page rất lớn (nguy cơ integer overflow) trả về danh sách rỗng an toàn")
    void testSearch_LargePageOverflow_ReturnsEmptyList() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(100L);
        User globalUser = createUserWithScope(100L, DataScope.COMPANY, null);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(globalUser));

        ResourceCandidate c1 = createCandidate(101L, 1L, 10L, "NV01", "A");
        when(searchResourcePort.findActiveEmployeesBySkill(1L, 1)).thenReturn(List.of(c1));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        // page = Integer.MAX_VALUE / 2, size = 100 -> page * size overflows int
        SearchResourceQuery overflowQuery = new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10, 1_500_000_000, 100);
        List<ResourceSearchResult> results = service.search(overflowQuery);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge case 13: Hợp đồng kết thúc giữa tuần (ví dụ thứ 4 - 3 ngày làm việc) tính tỷ lệ giờ khả dụng chính xác")
    void testSearch_ContractEndsMidWeek_ProRatesAvailableHours() {
        when(authorizationService.require(PermissionCode.RESOURCE_SEARCH)).thenReturn(100L);
        User globalUser = createUserWithScope(100L, DataScope.COMPANY, null);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(globalUser));

        YearWeek yw = YearWeek.of(2026, 10);
        // Thứ 2 là 2026-03-02, Thứ 4 là 2026-03-04 (3 ngày làm việc: T2, T3, T4)
        LocalDate wednesday = yw.getStartDate().plusDays(2);

        ResourceCandidate midWeekCandidate = new ResourceCandidate(
                101L, 101L, "NV01", "Nguyễn Văn A", 10L, "Developer", 40,
                wednesday, 1L, "Java", 3, BigDecimal.valueOf(3)
        );

        when(searchResourcePort.findActiveEmployeesBySkill(1L, 1)).thenReturn(List.of(midWeekCandidate));
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        SearchResourceQuery query = new SearchResourceQuery(1L, 1, null, 2026, 10, 2026, 10);
        List<ResourceSearchResult> results = service.search(query);

        assertEquals(1, results.size());
        // 40h * 3 / 5 = 24.00h khả dụng
        BigDecimal expectedHours = new BigDecimal("24.00");
        assertEquals(0, expectedHours.compareTo(results.get(0).totalRemainingHours()));
        assertEquals(0, expectedHours.compareTo(results.get(0).weeklyAvailabilities().get(0).netAvailableHours()));
        assertEquals(0, expectedHours.compareTo(results.get(0).weeklyAvailabilities().get(0).remainingHours()));
    }
}

