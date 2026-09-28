package com.hrm.employeemanagement.application.service.outsourcedcontract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractListResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort.OutsourcedAllocationRecord;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedContractPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetExpiringOutsourcedContractsServiceTest {

    @Mock
    private GetAuthenticatedUserPort authenticatedUserPort;

    @Mock
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    @Mock
    private LoadOutsourcedContractPort loadContractPort;

    @Mock
    private LoadOutsourcedAllocationPort loadAllocationPort;

    private GetExpiringOutsourcedContractsService service;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new GetExpiringOutsourcedContractsService(
                authenticatedUserPort,
                deniedAuditLogPort,
                loadContractPort,
                loadAllocationPort
        );
    }

    private User createMockUser(Long userId, RoleCode roleCode) {
        com.hrm.employeemanagement.domain.role.Role role = new com.hrm.employeemanagement.domain.role.Role(
                new com.hrm.employeemanagement.domain.role.RoleId(1L), roleCode, roleCode.getName()
        );
        Long scopeOrgUnitId = (roleCode == RoleCode.VT_03) ? 10L : null;
        com.hrm.employeemanagement.domain.authorization.DataScope scope = switch (roleCode) {
            case VT_03 -> com.hrm.employeemanagement.domain.authorization.DataScope.ORGANIZATION_BRANCH;
            case VT_04 -> com.hrm.employeemanagement.domain.authorization.DataScope.SELF;
            default -> com.hrm.employeemanagement.domain.authorization.DataScope.COMPANY;
        };
        return new User(new UserId(userId), "user_" + userId, "hash", role, UserStatus.ACTIVE, new EmployeeId(userId), scope, scopeOrgUnitId, 0L);
    }

    private Employee createMockOutsourcedEmployee(Long empId, String code, String name, LocalDate contractEnd) {
        return new Employee(
                new EmployeeId(empId),
                new UserId(empId),
                10L,
                code,
                name,
                "Backend Developer",
                today.minusMonths(6),
                contractEnd,
                true,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("Lấy danh sách thành công khi người dùng là VT-05 (Nhân sự)")
    void shouldGetExpiringContractsSuccessfullyForHrUser() {
        User hrUser = createMockUser(50L, RoleCode.VT_05);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(hrUser);

        LocalDate contractEnd = today.plusDays(25);
        Employee emp = createMockOutsourcedEmployee(105L, "EXT-005", "Vũ Thuê", contractEnd);
        when(loadContractPort.findAllOutsourcedEmployeesWithContract()).thenReturn(List.of(emp));
        when(loadContractPort.findOrgUnitNamesByIds(List.of(10L))).thenReturn(Map.of(10L, "Phòng Ban Đầu tư"));

        YearWeek week = YearWeek.from(contractEnd);
        OutsourcedAllocationRecord alloc = new OutsourcedAllocationRecord(
                701L, 105L, 301L, week, BigDecimal.valueOf(35.0)
        );
        when(loadAllocationPort.findAllocationsByEmployeeIds(List.of(105L))).thenReturn(List.of(alloc));
        when(loadAllocationPort.findProjectNamesByIds(List.of(301L))).thenReturn(Map.of(301L, "Hệ thống ERP"));

        ExpiringOutsourcedContractListResult result = service.execute(30);

        assertThat(result.totalExpiringContracts()).isEqualTo(1);
        assertThat(result.items().get(0).fullName()).isEqualTo("Vũ Thuê");
        assertThat(result.items().get(0).status()).isEqualTo("EXPIRING_SOON");
        assertThat(result.items().get(0).affectedAllocations()).hasSize(1);
        assertThat(result.items().get(0).affectedAllocations().get(0).projectName()).isEqualTo("Hệ thống ERP");
    }

    @Test
    @DisplayName("NCL-14-CN-003-TC-03: Người dùng không phải VT-03 hoặc VT-05 -> Bị chặn và ghi nhật ký lần từ chối")
    void shouldDenyAccessAndLogAuditWhenUnauthorized() {
        User unauthorizedUser = createMockUser(88L, RoleCode.VT_04);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(unauthorizedUser);

        assertThatThrownBy(() -> service.execute(30))
                .isInstanceOf(PermissionDeniedException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(deniedAuditLogPort).save(captor.capture());

        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("ACCESS_DENIED");
        assertThat(log.getTableName()).isEqualTo("OUTSOURCED_CONTRACT_EXPIRATION");
        assertThat(log.getUserId()).isEqualTo(88L);
    }

    @Test
    @DisplayName("Bảo mật DataScope: Quản lý nguồn lực (VT-03) chỉ xem nhân sự thuộc chi nhánh của mình")
    void shouldFilterByOrgUnitScopeWhenUserIsVt03() {
        User branchManager = createMockUser(30L, RoleCode.VT_03); // scopeOrgUnitId = 10L
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(branchManager);

        Employee inScopeEmp = createMockOutsourcedEmployee(101L, "EXT-101", "Nhân Viên Chi Nhánh Mình", today.plusDays(10));
        Employee outScopeEmp = new Employee(
                new EmployeeId(102L), new UserId(102L), 20L, "EXT-102", "Nhân Viên Chi Nhánh Khác",
                "Tester", today.minusMonths(3), today.plusDays(15), true, 40, EmployeeStatus.ACTIVE
        );

        when(loadContractPort.findAllOutsourcedEmployeesWithContract()).thenReturn(List.of(inScopeEmp, outScopeEmp));
        when(loadContractPort.findOrgUnitNamesByIds(List.of(10L))).thenReturn(Map.of(10L, "Chi Nhánh 10"));
        when(loadAllocationPort.findAllocationsByEmployeeIds(List.of(101L))).thenReturn(List.of());

        ExpiringOutsourcedContractListResult result = service.execute(30);

        assertThat(result.totalExpiringContracts()).isEqualTo(1);
        assertThat(result.items().get(0).employeeId()).isEqualTo(101L);
        assertThat(result.items().get(0).fullName()).isEqualTo("Nhân Viên Chi Nhánh Mình");
    }

    @Test
    @DisplayName("Sắp xếp kết quả ưu tiên hợp đồng gấp nhất: daysRemaining tăng dần")
    void shouldSortExpiringContractsByDaysRemainingAscending() {
        User hrUser = createMockUser(50L, RoleCode.VT_05);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(hrUser);

        Employee empLater = createMockOutsourcedEmployee(101L, "EXT-101", "Hết hạn sau 25 ngày", today.plusDays(25));
        Employee empUrgent = createMockOutsourcedEmployee(102L, "EXT-102", "Hết hạn sau 5 ngày", today.plusDays(5));

        when(loadContractPort.findAllOutsourcedEmployeesWithContract()).thenReturn(List.of(empLater, empUrgent));
        when(loadContractPort.findOrgUnitNamesByIds(List.of(10L))).thenReturn(Map.of(10L, "Phòng Kỹ Thuật"));
        when(loadAllocationPort.findAllocationsByEmployeeIds(List.of(101L, 102L))).thenReturn(List.of());

        ExpiringOutsourcedContractListResult result = service.execute(30);

        assertThat(result.totalExpiringContracts()).isEqualTo(2);
        // Hợp đồng khẩn cấp hơn (5 ngày) phải đứng trước hợp đồng 25 ngày
        assertThat(result.items().get(0).employeeId()).isEqualTo(102L);
        assertThat(result.items().get(0).daysRemaining()).isEqualTo(5);
        assertThat(result.items().get(1).employeeId()).isEqualTo(101L);
        assertThat(result.items().get(1).daysRemaining()).isEqualTo(25);
    }
}
