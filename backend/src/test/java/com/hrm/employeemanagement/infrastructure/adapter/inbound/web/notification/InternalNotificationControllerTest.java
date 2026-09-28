package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("InternalNotificationController Web API Tests (1 Internal Endpoint)")
class InternalNotificationControllerTest {

    private MockMvc mockMvc;
    private CreateNotificationEventUseCase createNotificationEventUseCase;
    private final String expectedToken = "test-secret-token-123";

    @BeforeEach
    void setUp() {
        createNotificationEventUseCase = mock(CreateNotificationEventUseCase.class);
        InternalNotificationController controller = new InternalNotificationController(
                createNotificationEventUseCase,
                expectedToken
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /internal/notifications không có token -> Trả về 403 FORBIDDEN")
    void createNotification_withoutToken_returns403() throws Exception {
        String requestJson = """
                {
                    "eventType": "OVERLOAD_WARNING",
                    "level": "CAO",
                    "title": "Cảnh báo quá tải",
                    "message": "Nội dung",
                    "sourceEventKey": "OVERLOAD:E001:2026-W38",
                    "recipientUserIds": [101]
                }
                """;

        mockMvc.perform(post("/internal/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_CALL"));

        verify(createNotificationEventUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST /internal/notifications với token sai -> Trả về 403 FORBIDDEN")
    void createNotification_withWrongToken_returns403() throws Exception {
        String requestJson = """
                {
                    "eventType": "OVERLOAD_WARNING",
                    "level": "CAO",
                    "title": "Cảnh báo quá tải",
                    "message": "Nội dung",
                    "sourceEventKey": "OVERLOAD:E001:2026-W38",
                    "recipientUserIds": [101]
                }
                """;

        mockMvc.perform(post("/internal/notifications")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_CALL"));

        verify(createNotificationEventUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST /internal/notifications với token đúng -> Trả về 201 CREATED")
    void createNotification_withValidToken_returns201() throws Exception {
        when(createNotificationEventUseCase.execute(any())).thenReturn(88L);

        String requestJson = """
                {
                    "eventType": "OVERLOAD_WARNING",
                    "level": "CAO",
                    "title": "Cảnh báo quá tải",
                    "message": "Nội dung",
                    "sourceEventKey": "OVERLOAD:E001:2026-W38",
                    "recipientUserIds": [101, 102]
                }
                """;

        mockMvc.perform(post("/internal/notifications")
                        .header("X-Internal-Token", expectedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(88));

        verify(createNotificationEventUseCase, times(1)).execute(any());
    }

    @Test
    @DisplayName("Khởi tạo InternalNotificationController không có token hoặc token rỗng -> Bắn lỗi IllegalStateException (Fail-fast)")
    void initController_withoutToken_throwsIllegalStateException() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new InternalNotificationController(createNotificationEventUseCase, null)
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new InternalNotificationController(createNotificationEventUseCase, "   ")
        );
    }
}
