package com.hrm.employeemanagement.application.service.allocation.threshold;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdHistoryResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.ConfigureCapacityThresholdCommand;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdHistoryPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.SaveCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdVersionConflictException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidCapacityThresholdException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CapacityThresholdServiceTest {

    private AuthorizationService authorizationService;
    private LoadCapacityThresholdPort loadCapacityThresholdPort;
    private SaveCapacityThresholdPort saveCapacityThresholdPort;
    private SaveAuditLogPort saveAuditLogPort;
    private LoadCapacityThresholdHistoryPort historyPort;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;

    private CapacityThresholdService service;

    private final Long VT01_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadCapacityThresholdPort = mock(LoadCapacityThresholdPort.class);
        saveCapacityThresholdPort = mock(SaveCapacityThresholdPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);
        historyPort = mock(LoadCapacityThresholdHistoryPort.class);
        loadUserPort = mock(LoadUserPort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);

        service = new CapacityThresholdService(
                authorizationService,
                loadCapacityThresholdPort,
                saveCapacityThresholdPort,
                saveAuditLogPort,
                historyPort,
                loadUserPort,
                loadOrgUnitPort
        );

        User defaultUser = mock(User.class);
        when(defaultUser.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(defaultUser));

        when(authorizationService.require(PermissionCode.CAPACITY_THRESHOLD_MANAGE)).thenReturn(VT01_USER_ID);
        when(authorizationService.requireAny(any())).thenReturn(VT01_USER_ID);
    }

    @Test
    @DisplayName("TC-01: Cấu hình ngưỡng cảnh báo thành công và lưu Audit Log (Atomic Transaction)")
    void testConfigureThreshold_Success() {
        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                null
        );

        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.empty());

        CapacityThresholdConfig savedMock = new CapacityThresholdConfig(
                1L, CapacityThresholdScope.COMPANY, "COMPANY", null,
                BigDecimal.valueOf(90.0), BigDecimal.valueOf(20.0),
                1L, VT01_USER_ID, LocalDateTime.now(), VT01_USER_ID, LocalDateTime.now()
        );
        when(saveCapacityThresholdPort.save(any())).thenReturn(savedMock);

        CapacityThresholdResult result = service.configureThreshold(command);

        assertThat(result).isNotNull();
        assertThat(result.overloadThreshold()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
        assertThat(result.idleThreshold()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(result.isDefault()).isFalse();
        assertThat(result.isInherited()).isFalse();

        verify(saveCapacityThresholdPort, times(1)).save(any(CapacityThresholdConfig.class));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort, times(1)).save(auditCaptor.capture());
        AuditLog capturedAudit = auditCaptor.getValue();
        assertThat(capturedAudit.getAction()).isEqualTo("CREATE_CAPACITY_THRESHOLD");
        assertThat(capturedAudit.getTableName()).isEqualTo("capacity_threshold_configs");
        assertThat(capturedAudit.getRecordId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("HIGH #1: Optimistic Locking - Ném CapacityThresholdVersionConflictException khi version gửi lên không khớp với DB")
    void testConfigureThreshold_VersionMismatch_ThrowsConflictException() {
        CapacityThresholdConfig existing = new CapacityThresholdConfig(
                1L, CapacityThresholdScope.COMPANY, "COMPANY", null,
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(50.0),
                5L, VT01_USER_ID, LocalDateTime.now(), VT01_USER_ID, LocalDateTime.now()
        );
        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.of(existing));

        // Admin B gửi version 4 trong khi DB đang là version 5
        ConfigureCapacityThresholdCommand staleCommand = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(120.0),
                BigDecimal.valueOf(40.0),
                4L
        );

        assertThatThrownBy(() -> service.configureThreshold(staleCommand))
                .isInstanceOf(CapacityThresholdVersionConflictException.class)
                .hasMessageContaining("Cấu hình ngưỡng đã được cập nhật bởi thao tác khác");

        verify(saveCapacityThresholdPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("HIGH #1: Optimistic Locking - Thành công khi version gửi lên khớp với version DB")
    void testConfigureThreshold_MatchingVersion_Succeeds() {
        CapacityThresholdConfig existing = new CapacityThresholdConfig(
                1L, CapacityThresholdScope.COMPANY, "COMPANY", null,
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(50.0),
                5L, VT01_USER_ID, LocalDateTime.now(), VT01_USER_ID, LocalDateTime.now()
        );
        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.of(existing));

        ConfigureCapacityThresholdCommand validCommand = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(110.0),
                BigDecimal.valueOf(45.0),
                5L
        );

        CapacityThresholdConfig updatedMock = new CapacityThresholdConfig(
                1L, CapacityThresholdScope.COMPANY, "COMPANY", null,
                BigDecimal.valueOf(110.0), BigDecimal.valueOf(45.0),
                6L, VT01_USER_ID, LocalDateTime.now(), VT01_USER_ID, LocalDateTime.now()
        );
        when(saveCapacityThresholdPort.save(any())).thenReturn(updatedMock);

        CapacityThresholdResult result = service.configureThreshold(validCommand);

        assertThat(result.overloadThreshold()).isEqualByComparingTo(BigDecimal.valueOf(110.0));
        assertThat(result.idleThreshold()).isEqualByComparingTo(BigDecimal.valueOf(45.0));
        verify(saveCapacityThresholdPort, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-02: Ràng buộc nghiệp vụ idleThreshold < overloadThreshold (ném InvalidCapacityThresholdException khi bằng nhau)")
    void testConfigureThreshold_IdleEqualsOverload_ThrowsException() {
        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(80.0),
                BigDecimal.valueOf(80.0),
                null
        );

        assertThatThrownBy(() -> service.configureThreshold(command))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("phải nhỏ hơn");

        verify(saveCapacityThresholdPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Gate #N: NO-OP check - Giá trị không thay đổi thì không lưu và không ghi Audit Log")
    void testConfigureThreshold_NoChange_NoOp() {
        CapacityThresholdConfig existing = new CapacityThresholdConfig(
                1L, CapacityThresholdScope.COMPANY, "COMPANY", null,
                BigDecimal.valueOf(90.0), BigDecimal.valueOf(20.0),
                1L, VT01_USER_ID, LocalDateTime.now(), VT01_USER_ID, LocalDateTime.now()
        );
        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.of(existing));

        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                1L
        );

        CapacityThresholdResult result = service.configureThreshold(command);

        assertThat(result).isNotNull();
        verify(saveCapacityThresholdPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Kiểm tra phân quyền - Ném PermissionDeniedException nếu không có quyền CAPACITY_THRESHOLD_MANAGE")
    void testConfigureThreshold_PermissionDenied() {
        when(authorizationService.require(PermissionCode.CAPACITY_THRESHOLD_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.CAPACITY_THRESHOLD_MANAGE));

        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                null
        );

        assertThatThrownBy(() -> service.configureThreshold(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(loadCapacityThresholdPort, never()).findByScopeKey(any());
        verify(saveCapacityThresholdPort, never()).save(any());
    }

    @Test
    @DisplayName("HIGH #2: DataScope - Ném PermissionDeniedException khi đọc ORG_UNIT ngoài branch của người dùng")
    void testGetEffectiveThreshold_OrgUnitOutsideBranch_ThrowsPermissionDenied() {
        Long branchUserId = 2L;
        when(authorizationService.requireAny(any())).thenReturn(branchUserId);

        User branchUser = mock(User.class);
        when(branchUser.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(branchUser.getScopeOrgUnitId()).thenReturn(10L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(branchUser));

        // orgUnit 999 không thuộc chi nhánh 10
        when(loadOrgUnitPort.existsInOrgUnitBranch(999L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.getEffectiveThreshold(CapacityThresholdScope.ORG_UNIT, 999L))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("HIGH #2: DataScope - Cho phép khi orgUnit nằm trong branch của người dùng")
    void testGetEffectiveThreshold_OrgUnitWithinBranch_Success() {
        Long branchUserId = 2L;
        when(authorizationService.requireAny(any())).thenReturn(branchUserId);

        User branchUser = mock(User.class);
        when(branchUser.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(branchUser.getScopeOrgUnitId()).thenReturn(10L);
        when(loadUserPort.findById(any())).thenReturn(Optional.of(branchUser));

        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(loadCapacityThresholdPort.findByScopeKey("ORG_UNIT_10")).thenReturn(Optional.empty());
        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.empty());

        CapacityThresholdResult result = service.getEffectiveThreshold(CapacityThresholdScope.ORG_UNIT, 10L);

        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    @DisplayName("HIGH #3: Khi ORG_UNIT fallback sang cấu hình tùy chỉnh của COMPANY -> isInherited = true")
    void testGetEffectiveThreshold_OrgUnitFallbackToCompany_ReturnsInheritedTrue() {
        when(loadCapacityThresholdPort.findByScopeKey("ORG_UNIT_10")).thenReturn(Optional.empty());

        CapacityThresholdConfig companyConfig = new CapacityThresholdConfig(
                1L, CapacityThresholdScope.COMPANY, "COMPANY", null,
                BigDecimal.valueOf(110.0), BigDecimal.valueOf(40.0),
                1L, VT01_USER_ID, LocalDateTime.now(), VT01_USER_ID, LocalDateTime.now()
        );
        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.of(companyConfig));

        CapacityThresholdResult result = service.getEffectiveThreshold(CapacityThresholdScope.ORG_UNIT, 10L);

        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isFalse();
        assertThat(result.isInherited()).isTrue();
        assertThat(result.overloadThreshold()).isEqualByComparingTo(BigDecimal.valueOf(110.0));
        assertThat(result.idleThreshold()).isEqualByComparingTo(BigDecimal.valueOf(40.0));
    }

    @Test
    @DisplayName("BR-04 / Gate #M: Khi chưa có cấu hình riêng -> Trả về giá trị mặc định hệ thống (100% quá tải, 50% nhàn rỗi, isInherited = false)")
    void testGetEffectiveThreshold_FallbackToDefault() {
        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.empty());

        CapacityThresholdResult result = service.getEffectiveThreshold(CapacityThresholdScope.COMPANY, null);

        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isTrue();
        assertThat(result.isInherited()).isFalse();
        assertThat(result.overloadThreshold()).isEqualByComparingTo(BigDecimal.valueOf(100.0));
        assertThat(result.idleThreshold()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
    }

    @Test
    @DisplayName("TC-04: Lấy lịch sử thay đổi cấu hình kiểm toán")
    void testGetHistory() {
        CapacityThresholdConfig config = new CapacityThresholdConfig(
                100L, CapacityThresholdScope.COMPANY, "COMPANY", null,
                BigDecimal.valueOf(90.0), BigDecimal.valueOf(20.0),
                1L, VT01_USER_ID, LocalDateTime.now(), VT01_USER_ID, LocalDateTime.now()
        );
        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.of(config));

        CapacityThresholdHistoryResult historyItem = new CapacityThresholdHistoryResult(
                1L, VT01_USER_ID, "Giám Đốc", "UPDATE_CAPACITY_THRESHOLD",
                "{\"overloadThreshold\":100.0}", "{\"overloadThreshold\":90.0}", LocalDateTime.now()
        );
        when(historyPort.loadHistory("capacity_threshold_configs", 100L)).thenReturn(List.of(historyItem));

        List<CapacityThresholdHistoryResult> results = service.getHistory(CapacityThresholdScope.COMPANY, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).action()).isEqualTo("UPDATE_CAPACITY_THRESHOLD");
    }
}
