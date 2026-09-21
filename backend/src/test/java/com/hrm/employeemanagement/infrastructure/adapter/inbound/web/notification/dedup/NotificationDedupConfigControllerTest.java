package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dedup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.notification.dedup.NotificationDedupConfigResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.GetNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.TriggerManualOverloadScanUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.UpdateNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

class NotificationDedupConfigControllerTest {

    private MockMvc mockMvc;

    private GetNotificationDedupConfigUseCase getConfigUseCase;
    private UpdateNotificationDedupConfigUseCase updateConfigUseCase;
    private TriggerManualOverloadScanUseCase triggerManualScanUseCase;
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void setUp() {
        getConfigUseCase = mock(GetNotificationDedupConfigUseCase.class);
        updateConfigUseCase = mock(UpdateNotificationDedupConfigUseCase.class);
        triggerManualScanUseCase = mock(TriggerManualOverloadScanUseCase.class);
        currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.getCurrentUserId()).thenReturn(Optional.of(1L));

        NotificationDedupConfigController controller = new NotificationDedupConfigController(
                getConfigUseCase,
                updateConfigUseCase,
                triggerManualScanUseCase,
                currentUserPort
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/notification-dedup-config: Quản trị viên lấy cấu hình thành công")
    void getConfig_success() throws Exception {
        NotificationDedupConfigResult mockResult = new NotificationDedupConfigResult(
                true,
                7,
                60,
                1L,
                LocalDateTime.now(),
                0L
        );
        when(getConfigUseCase.getConfig()).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/admin/notification-dedup-config")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isEnabled").value(true))
                .andExpect(jsonPath("$.data.dedupWindowDays").value(7))
                .andExpect(jsonPath("$.data.scanIntervalMinutes").value(60));
    }

    @Test
    @DisplayName("TC-03: Người dùng không có quyền truy cập GET /api/v1/admin/notification-dedup-config -> Trả về 403 Forbidden")
    void getConfig_accessDenied_returns403() throws Exception {
        when(getConfigUseCase.getConfig())
                .thenThrow(new PermissionDeniedException(PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE));

        mockMvc.perform(get("/api/v1/admin/notification-dedup-config")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/notification-dedup-config: Quản trị viên cập nhật thành công (TC-04)")
    void updateConfig_success() throws Exception {
        NotificationDedupConfigResult updatedResult = new NotificationDedupConfigResult(
                false,
                14,
                120,
                1L,
                LocalDateTime.now(),
                1L
        );
        when(updateConfigUseCase.updateConfig(any(), eq(1L))).thenReturn(updatedResult);

        String requestJson = """
                {
                    "isEnabled": false,
                    "dedupWindowDays": 14,
                    "scanIntervalMinutes": 120
                }
                """;

        mockMvc.perform(put("/api/v1/admin/notification-dedup-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isEnabled").value(false))
                .andExpect(jsonPath("$.data.dedupWindowDays").value(14))
                .andExpect(jsonPath("$.data.scanIntervalMinutes").value(120));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/notification-dedup-config: Request vi phạm validate -> Trả về 400 Bad Request")
    void updateConfig_validationError_returns400() throws Exception {
        String invalidRequestJson = """
                {
                    "isEnabled": true,
                    "dedupWindowDays": 0,
                    "scanIntervalMinutes": 3
                }
                """;

        mockMvc.perform(put("/api/v1/admin/notification-dedup-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/admin/notification-dedup-config/trigger-scan: Kích hoạt quét thành công -> Trả về 200 OK")
    void triggerScan_success() throws Exception {
        OverloadScanResult mockResult = new OverloadScanResult(10, 2, 2, 0, 0);
        when(triggerManualScanUseCase.triggerManualScan(null, null)).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/admin/notification-dedup-config/trigger-scan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalScanned").value(10))
                .andExpect(jsonPath("$.data.newlyAlertedCount").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/admin/notification-dedup-config/trigger-scan: Tham số không hợp lệ -> Trả về 400 Bad Request")
    void triggerScan_validationError_returns400() throws Exception {
        when(triggerManualScanUseCase.triggerManualScan(eq(2026), eq(null)))
                .thenThrow(new IllegalArgumentException("Cả hai tham số 'year' và 'weekNumber' phải cùng được cung cấp hoặc cùng để trống."));

        mockMvc.perform(post("/api/v1/admin/notification-dedup-config/trigger-scan")
                        .param("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cả hai tham số 'year' và 'weekNumber' phải cùng được cung cấp hoặc cùng để trống."));
    }

    @Test
    @DisplayName("POST /api/v1/admin/notification-dedup-config/trigger-scan: Không có quyền truy cập -> Trả về 403 Forbidden")
    void triggerScan_accessDenied_returns403() throws Exception {
        when(triggerManualScanUseCase.triggerManualScan(any(), any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE));

        mockMvc.perform(post("/api/v1/admin/notification-dedup-config/trigger-scan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
