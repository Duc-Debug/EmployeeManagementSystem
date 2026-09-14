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
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidCapacityThresholdException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;

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

        service = new CapacityThresholdService(
                authorizationService,
                loadCapacityThresholdPort,
                saveCapacityThresholdPort,
                saveAuditLogPort,
                historyPort,
                loadUserPort
        );

        when(authorizationService.require(PermissionCode.CAPACITY_THRESHOLD_MANAGE)).thenReturn(VT01_USER_ID);
        when(authorizationService.requireAny(any())).thenReturn(VT01_USER_ID);
    }

    @Test
    @DisplayName("TC-01: VT-01 cấu hình ngưỡng hợp lệ lần đầu -> tạo mới bản ghi và ghi audit log")
    void testConfigureThreshold_HappyPath_CreateNew() {
        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                null
        );

        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.empty());
        when(saveCapacityThresholdPort.save(any())).thenAnswer(invocation -> {
            CapacityThresholdConfig c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        CapacityThresholdResult result = service.configureThreshold(command);

        assertThat(result).isNotNull();
        assertThat(result.overloadThreshold()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
        assertThat(result.idleThreshold()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(result.isDefault()).isFalse();

        verify(saveCapacityThresholdPort).save(any(CapacityThresholdConfig.class));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getAction()).isEqualTo("CREATE_CAPACITY_THRESHOLD");
        assertThat(savedAudit.getNewValue()).contains("\"overloadThreshold\":90.0");
    }

    @Test
    @DisplayName("TC-01: VT-01 cập nhật cấu hình đã tồn tại -> sửa giá trị và ghi audit log có oldValue và newValue")
    void testConfigureThreshold_HappyPath_UpdateExisting() {
        CapacityThresholdConfig existing = new CapacityThresholdConfig(
                100L,
                CapacityThresholdScope.COMPANY,
                "COMPANY",
                null,
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(50.0),
                1L,
                VT01_USER_ID,
                LocalDateTime.now(),
                VT01_USER_ID,
                LocalDateTime.now()
        );

        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                1L
        );

        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.of(existing));
        when(saveCapacityThresholdPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapacityThresholdResult result = service.configureThreshold(command);

        assertThat(result.overloadThreshold()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
        assertThat(result.idleThreshold()).isEqualByComparingTo(BigDecimal.valueOf(20.0));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getAction()).isEqualTo("UPDATE_CAPACITY_THRESHOLD");
        assertThat(savedAudit.getOldValue()).contains("\"overloadThreshold\":100.0");
        assertThat(savedAudit.getNewValue()).contains("\"overloadThreshold\":90.0");
    }

    @Test
    @DisplayName("Gate #N: Yêu cầu NO-OP (giá trị không đổi) -> Không gọi save DB và không ghi audit log")
    void testConfigureThreshold_NOOP_DoesNotSaveOrAudit() {
        CapacityThresholdConfig existing = new CapacityThresholdConfig(
                100L,
                CapacityThresholdScope.COMPANY,
                "COMPANY",
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                1L,
                VT01_USER_ID,
                LocalDateTime.now(),
                VT01_USER_ID,
                LocalDateTime.now()
        );

        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                1L
        );

        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.of(existing));

        CapacityThresholdResult result = service.configureThreshold(command);

        assertThat(result.overloadThreshold()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
        verify(saveCapacityThresholdPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-02: Dữ liệu không hợp lệ (idleThreshold > overloadThreshold) -> Báo lỗi, không lưu")
    void testConfigureThreshold_InvalidData_ThrowsException() {
        ConfigureCapacityThresholdCommand command = new ConfigureCapacityThresholdCommand(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(80.0),
                null
        );

        assertThatThrownBy(() -> service.configureThreshold(command))
                .isInstanceOf(InvalidCapacityThresholdException.class);

        verify(saveCapacityThresholdPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Không có quyền (chặn bởi authorizationService) -> Ném PermissionDeniedException, không chạm business logic")
    void testConfigureThreshold_Unauthorized_ThrowsPermissionDenied() {
        reset(authorizationService);
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
    @DisplayName("BR-04 / Gate #M: Khi chưa có cấu hình riêng -> Trả về giá trị mặc định hệ thống (100% quá tải, 50% nhàn rỗi)")
    void testGetEffectiveThreshold_FallbackToDefault() {
        when(loadCapacityThresholdPort.findByScopeKey("COMPANY")).thenReturn(Optional.empty());

        CapacityThresholdResult result = service.getEffectiveThreshold(CapacityThresholdScope.COMPANY, null);

        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isTrue();
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
