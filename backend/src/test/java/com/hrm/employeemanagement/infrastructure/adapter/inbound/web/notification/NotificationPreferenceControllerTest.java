package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.notification.NotificationPreferenceResult;
import com.hrm.employeemanagement.application.dto.notification.UpdateNotificationPreferenceCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.ResetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.UpdateNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.domain.notification.NotificationDeliveryChannel;
import com.hrm.employeemanagement.domain.notification.NotificationFrequency;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@DisplayName("NotificationPreferenceController Web API Tests")
class NotificationPreferenceControllerTest {

    private MockMvc mockMvc;

    private GetNotificationPreferenceUseCase getNotificationPreferenceUseCase;
    private UpdateNotificationPreferenceUseCase updateNotificationPreferenceUseCase;
    private ResetNotificationPreferenceUseCase resetNotificationPreferenceUseCase;
    private CurrentUserPort currentUserPort;

    private final Long currentUserId = 100L;

    @BeforeEach
    void setUp() {
        getNotificationPreferenceUseCase = mock(GetNotificationPreferenceUseCase.class);
        updateNotificationPreferenceUseCase = mock(UpdateNotificationPreferenceUseCase.class);
        resetNotificationPreferenceUseCase = mock(ResetNotificationPreferenceUseCase.class);
        currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.getCurrentUserId()).thenReturn(Optional.of(currentUserId));

        NotificationPreferenceController controller = new NotificationPreferenceController(
                getNotificationPreferenceUseCase,
                updateNotificationPreferenceUseCase,
                resetNotificationPreferenceUseCase,
                currentUserPort
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notification-preferences/me trả về 200 OK với cấu hình của user")
    void shouldReturnMyPreferenceSuccessfully() throws Exception {
        NotificationPreferenceResult dummy = new NotificationPreferenceResult(
                1L, currentUserId, true, true,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationFrequency.IMMEDIATE, 3, false, null, null, 0L
        );

        when(getNotificationPreferenceUseCase.getMyPreference(currentUserId)).thenReturn(dummy);

        mockMvc.perform(get("/api/v1/notification-preferences/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(currentUserId))
                .andExpect(jsonPath("$.data.inAppEnabled").value(true))
                .andExpect(jsonPath("$.data.emailEnabled").value(true))
                .andExpect(jsonPath("$.data.taskDueReminderDays").value(3));

        verify(getNotificationPreferenceUseCase).getMyPreference(currentUserId);
    }

    @Test
    @DisplayName("PUT /api/v1/notification-preferences/me cập nhật thành công")
    void shouldUpdateMyPreferenceSuccessfully() throws Exception {
        NotificationPreferenceResult updated = new NotificationPreferenceResult(
                1L, currentUserId, true, false,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.IN_APP_ONLY,
                NotificationFrequency.DAILY_DIGEST, 5, true, null, null, 1L
        );

        when(updateNotificationPreferenceUseCase.updateMyPreference(eq(currentUserId), any(UpdateNotificationPreferenceCommand.class)))
                .thenReturn(updated);

        String json = """
                {
                    "inAppEnabled": true,
                    "emailEnabled": false,
                    "taskAssignedChannel": "IN_APP_ONLY",
                    "taskDueReminderChannel": "IN_APP_ONLY",
                    "taskCommentChannel": "IN_APP_ONLY",
                    "timesheetReminderChannel": "IN_APP_ONLY",
                    "allocationChangedChannel": "IN_APP_ONLY",
                    "scheduleConflictChannel": "IN_APP_ONLY",
                    "frequency": "DAILY_DIGEST",
                    "taskDueReminderDays": 5,
                    "quietHoursEnabled": true,
                    "quietHoursStart": "22:00:00",
                    "quietHoursEnd": "07:00:00"
                }
                """;

        mockMvc.perform(put("/api/v1/notification-preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.emailEnabled").value(false))
                .andExpect(jsonPath("$.data.taskDueReminderDays").value(5))
                .andExpect(jsonPath("$.data.frequency").value("DAILY_DIGEST"));

        verify(updateNotificationPreferenceUseCase).updateMyPreference(eq(currentUserId), any(UpdateNotificationPreferenceCommand.class));
    }

    @Test
    @DisplayName("POST /api/v1/notification-preferences/me/reset khôi phục cấu hình mặc định thành công")
    void shouldResetMyPreferenceSuccessfully() throws Exception {
        NotificationPreferenceResult defaultPref = new NotificationPreferenceResult(
                1L, currentUserId, true, true,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.IN_APP_ONLY, NotificationDeliveryChannel.ALL,
                NotificationDeliveryChannel.ALL, NotificationDeliveryChannel.ALL,
                NotificationFrequency.IMMEDIATE, 3, false, null, null, 2L
        );

        when(resetNotificationPreferenceUseCase.resetMyPreference(currentUserId)).thenReturn(defaultPref);

        mockMvc.perform(post("/api/v1/notification-preferences/me/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.emailEnabled").value(true))
                .andExpect(jsonPath("$.data.frequency").value("IMMEDIATE"));

        verify(resetNotificationPreferenceUseCase).resetMyPreference(currentUserId);
    }
}
