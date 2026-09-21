package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dedup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.GetNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.ScanOverloadAndAlertUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.UpdateNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

class NotificationDedupConfigControllerTest {

    private MockMvc mockMvc;

    private GetNotificationDedupConfigUseCase getConfigUseCase;
    private UpdateNotificationDedupConfigUseCase updateConfigUseCase;
    private ScanOverloadAndAlertUseCase scanUseCase;
    private CurrentUserPort currentUserPort;

    @BeforeEach
    void setUp() {
        getConfigUseCase = mock(GetNotificationDedupConfigUseCase.class);
        updateConfigUseCase = mock(UpdateNotificationDedupConfigUseCase.class);
        scanUseCase = mock(ScanOverloadAndAlertUseCase.class);
        currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.getCurrentUserId()).thenReturn(Optional.of(1L));

        NotificationDedupConfigController controller = new NotificationDedupConfigController(
                getConfigUseCase,
                updateConfigUseCase,
                scanUseCase,
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
}
