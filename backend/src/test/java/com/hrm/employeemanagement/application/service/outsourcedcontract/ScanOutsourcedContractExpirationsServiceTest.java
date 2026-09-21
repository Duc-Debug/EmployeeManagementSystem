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

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ScanOutsourcedContractsResult;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadNotificationRecipientUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort.OutsourcedAllocationRecord;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedContractPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanOutsourcedContractExpirationsServiceTest {

    @Mock
    private GetAuthenticatedUserPort authenticatedUserPort;

    @Mock
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private LoadOutsourcedContractPort loadContractPort;

    @Mock
    private LoadOutsourcedAllocationPort loadAllocationPort;

    @Mock
    private LoadNotificationRecipientUserPort recipientUserPort;

    @Mock
    private CreateNotificationEventUseCase createNotificationEventUseCase;

    private ScanOutsourcedContractExpirationsService service;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new ScanOutsourcedContractExpirationsService(
                authenticatedUserPort,
                deniedAuditLogPort,
                saveAuditLogPort,
                loadContractPort,
                loadAllocationPort,
                recipientUserPort,
                createNotificationEventUseCase
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
    @DisplayName("NCL-14-CN-003-TC-01: Hợp đồng thuê còn 25 ngày là hết hạn -> Quản lý nguồn lực nhận cảnh báo kèm phân bổ vắt qua ngày hết hạn")
    void tc01_SendNotification_WhenContractExpiresIn25Days() {
        // Given: Hợp đồng còn đúng 25 ngày
        LocalDate contractEnd = today.plusDays(25);
        Employee emp = createMockOutsourcedEmployee(101L, "EXT-001", "Nguyễn Văn Thuê", contractEnd);
        when(loadContractPort.findAllOutsourcedEmployeesWithContract()).thenReturn(List.of(emp));
        when(loadContractPort.findOrgUnitNamesByIds(any())).thenReturn(Map.of(10L, "Phòng Phần mềm"));

        // Tuần phân bổ chứa ngày hết hạn (vắt qua ngày hết hạn theo QTN-21)
        YearWeek expWeek = YearWeek.from(contractEnd);
        OutsourcedAllocationRecord alloc = new OutsourcedAllocationRecord(
                501L, 101L, 201L, expWeek, BigDecimal.valueOf(40.0)
        );
        when(loadAllocationPort.findAllocationsByEmployeeIds(List.of(101L))).thenReturn(List.of(alloc));
        when(loadAllocationPort.findProjectNamesByIds(any())).thenReturn(Map.of(201L, "Dự án CRM"));

        // Danh sách người nhận là VT-03 và VT-05
        when(recipientUserPort.findResourceManagersAndHrUserIds()).thenReturn(List.of(1L, 2L));

        // When: Hệ thống chạy rà soát
        ScanOutsourcedContractsResult result = service.execute(false);

        // Then: Gửi cảnh báo kèm phân bổ vắt qua ngày hết hạn
        assertThat(result.totalScanned()).isEqualTo(1);
        assertThat(result.totalExpiringContractsFound()).isEqualTo(1);
        assertThat(result.notificationsSent()).isEqualTo(1);

        ArgumentCaptor<CreateNotificationEventCommand> notifCaptor = ArgumentCaptor.forClass(CreateNotificationEventCommand.class);
        verify(createNotificationEventUseCase).execute(notifCaptor.capture());

        CreateNotificationEventCommand command = notifCaptor.getValue();
        assertThat(command.title()).contains("Nguyễn Văn Thuê");
        assertThat(command.message()).contains("Dự án CRM");
        assertThat(command.message()).contains("còn 25 ngày");
        assertThat(command.recipientUserIds()).containsExactly(1L, 2L);
        // QTN-19: Chống gửi trùng lặp qua sourceEventKey
        assertThat(command.sourceEventKey()).isEqualTo("OUTSOURCED_CONTRACT_EXPIRY_101_" + contractEnd);
    }

    @Test
    @DisplayName("NCL-14-CN-003-TC-02: Dữ liệu rỗng - Không có hợp đồng thuê nào sắp hết hạn -> Hệ thống không gửi cảnh báo nào")
    void tc02_EmptyData_WhenNoExpiringContracts_ShouldNotSendNotifications() {
        // Given: Hợp đồng còn 90 ngày (dài hạn, không nằm trong ngưỡng 30 ngày)
        LocalDate contractEnd = today.plusDays(90);
        Employee emp = createMockOutsourcedEmployee(102L, "EXT-002", "Trần Thị Thuê", contractEnd);
        when(loadContractPort.findAllOutsourcedEmployeesWithContract()).thenReturn(List.of(emp));
        when(loadContractPort.findOrgUnitNamesByIds(any())).thenReturn(Map.of(10L, "Phòng Phần mềm"));
        when(loadAllocationPort.findAllocationsByEmployeeIds(List.of(102L))).thenReturn(List.of());
        when(loadAllocationPort.findProjectNamesByIds(any())).thenReturn(Map.of());

        // When: Hệ thống chạy rà soát
        ScanOutsourcedContractsResult result = service.execute(false);

        // Then: Hệ thống không gửi cảnh báo nào
        assertThat(result.totalScanned()).isEqualTo(1);
        assertThat(result.totalExpiringContractsFound()).isEqualTo(0);
        assertThat(result.notificationsSent()).isEqualTo(0);
        assertThat(result.details()).contains("Không có hợp đồng thuê ngoài nào sắp hết hạn");

        verify(createNotificationEventUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("NCL-14-CN-003-TC-03: Không có quyền - Người dùng vai trò VT-04 mở chức năng -> Từ chối và ghi nhật ký lần từ chối")
    void tc03_UnauthorizedUser_ShouldDenyAccessAndRecordAuditLog() {
        // Given: Người dùng là VT-04 (Nhân viên chuyên môn), không phải VT-03 hay VT-05
        User staffUser = createMockUser(99L, RoleCode.VT_04);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(staffUser);

        // When & Then: Gọi rà soát thủ công bị chặn với PermissionDeniedException
        assertThatThrownBy(() -> service.execute(true))
                .isInstanceOf(PermissionDeniedException.class);

        // Hệ thống ghi nhật ký lần từ chối vào audit_logs với action = ACCESS_DENIED
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(deniedAuditLogPort).save(auditCaptor.capture());

        AuditLog log = auditCaptor.getValue();
        assertThat(log.getAction()).isEqualTo("ACCESS_DENIED");
        assertThat(log.getTableName()).isEqualTo("OUTSOURCED_CONTRACT_EXPIRATION");
        assertThat(log.getUserId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("NCL-14-CN-003-TC-04: Lưu lịch sử - Người dùng hợp lệ thực hiện thao tác -> Hệ thống ghi lại người thực hiện, nội dung và thời điểm")
    void tc04_ValidUser_ShouldRecordAuditLogHistoryOnManualScan() {
        // Given: Người dùng là Quản lý nguồn lực (VT-03)
        User rmUser = createMockUser(33L, RoleCode.VT_03);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(rmUser);

        LocalDate contractEnd = today.plusDays(20);
        Employee emp = createMockOutsourcedEmployee(103L, "EXT-003", "Lê Thuê", contractEnd);
        when(loadContractPort.findAllOutsourcedEmployeesWithContract()).thenReturn(List.of(emp));
        when(loadContractPort.findOrgUnitNamesByIds(any())).thenReturn(Map.of());
        when(loadAllocationPort.findAllocationsByEmployeeIds(any())).thenReturn(List.of());
        when(loadAllocationPort.findProjectNamesByIds(any())).thenReturn(Map.of());
        when(recipientUserPort.findResourceManagersAndHrUserIds()).thenReturn(List.of(33L));

        // When: Xác nhận thao tác rà soát thủ công
        ScanOutsourcedContractsResult result = service.execute(true);

        // Then: Hệ thống ghi lại người thực hiện, nội dung và thời điểm vào audit_logs
        assertThat(result.notificationsSent()).isEqualTo(1);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());

        AuditLog log = auditCaptor.getValue();
        assertThat(log.getAction()).isEqualTo("MANUAL_SCAN");
        assertThat(log.getTableName()).isEqualTo("OUTSOURCED_CONTRACT_EXPIRATION");
        assertThat(log.getUserId()).isEqualTo(33L);
        assertThat(log.getNewValue()).contains("totalScanned=1");
        assertThat(log.getNewValue()).contains("notificationsSent=1");
    }
}
