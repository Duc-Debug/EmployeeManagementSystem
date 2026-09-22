package com.hrm.employeemanagement.application.service.outsourcedcontract;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedContractPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcknowledgeOutsourcedContractWarningServiceTest {

    @Mock
    private GetAuthenticatedUserPort authenticatedUserPort;

    @Mock
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private LoadOutsourcedContractPort loadContractPort;

    private AcknowledgeOutsourcedContractWarningService service;

    @BeforeEach
    void setUp() {
        service = new AcknowledgeOutsourcedContractWarningService(
                authenticatedUserPort,
                deniedAuditLogPort,
                saveAuditLogPort,
                loadContractPort
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

    private Employee createMockOutsourcedEmployee(Long empId, String code, String name) {
        return new Employee(
                new EmployeeId(empId),
                new UserId(empId),
                10L,
                code,
                name,
                "Backend Developer",
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusDays(25),
                true,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("NCL-14-CN-003-TC-04: Lưu lịch sử - Xác nhận thao tác xử lý cảnh báo -> Ghi lại người thực hiện, nội dung và thời điểm vào audit_logs")
    void tc04_ShouldRecordAuditLogHistoryWhenAcknowledged() {
        User rmUser = createMockUser(40L, RoleCode.VT_03);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(rmUser);

        Employee emp = createMockOutsourcedEmployee(110L, "EXT-010", "Phạm Văn Thuê");
        when(loadContractPort.findById(110L)).thenReturn(Optional.of(emp));

        AcknowledgeOutsourcedContractCommand command = new AcknowledgeOutsourcedContractCommand(
                110L,
                "Đang tiến hành đàm phán gia hạn hợp đồng thêm 6 tháng",
                LocalDate.now().plusDays(10)
        );

        AcknowledgeOutsourcedContractResult result = service.execute(command);

        assertThat(result.employeeId()).isEqualTo(110L);
        assertThat(result.status()).isEqualTo("ACKNOWLEDGED");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(captor.capture());

        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("ACKNOWLEDGE_WARNING");
        assertThat(log.getTableName()).isEqualTo("OUTSOURCED_CONTRACT_EXPIRATION");
        assertThat(log.getUserId()).isEqualTo(40L);
        assertThat(log.getRecordId()).isEqualTo(110L);
        assertThat(log.getNewValue()).contains("Đang tiến hành đàm phán gia hạn hợp đồng");
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("NCL-14-CN-003-TC-03: Người dùng vai trò khác (VT-04) xác nhận -> Bị chặn và ghi log ACCESS_DENIED")
    void tc03_UnauthorizedUser_ShouldDenyAndLogAccessDenied() {
        User staffUser = createMockUser(77L, RoleCode.VT_04);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(staffUser);

        AcknowledgeOutsourcedContractCommand command = new AcknowledgeOutsourcedContractCommand(
                110L, "Note", null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(PermissionDeniedException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(deniedAuditLogPort).save(captor.capture());

        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("ACCESS_DENIED");
        assertThat(log.getTableName()).isEqualTo("OUTSOURCED_CONTRACT_EXPIRATION");
        assertThat(log.getUserId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("DataScope: VT-03 xác nhận nhân sự thuộc chi nhánh khác -> Bị chặn và ghi log ACCESS_DENIED")
    void dataScope_VT03AcknowledgeOtherBranch_ShouldDenyAndLogAccessDenied() {
        // User VT-03 quản lý chi nhánh 10L
        User rmUser = createMockUser(40L, RoleCode.VT_03);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(rmUser);

        // Nhân viên thuộc chi nhánh 20L (chi nhánh khác)
        Employee otherBranchEmp = new Employee(
                new EmployeeId(111L),
                new UserId(111L),
                20L,
                "EXT-011",
                "Nhân Viên Chi Nhánh Khác",
                "Backend Developer",
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusDays(25),
                true,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadContractPort.findById(111L)).thenReturn(Optional.of(otherBranchEmp));

        AcknowledgeOutsourcedContractCommand command = new AcknowledgeOutsourcedContractCommand(
                111L, "Thử xác nhận", null
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(PermissionDeniedException.class);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(deniedAuditLogPort).save(captor.capture());

        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("ACCESS_DENIED");
        assertThat(log.getTableName()).isEqualTo("OUTSOURCED_CONTRACT_EXPIRATION");
        assertThat(log.getUserId()).isEqualTo(40L);
        assertThat(log.getRecordId()).isEqualTo(111L);
    }
}
