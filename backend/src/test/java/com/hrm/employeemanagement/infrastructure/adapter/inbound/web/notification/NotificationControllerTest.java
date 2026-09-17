package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.notification.NotificationCenterItemResult;
import com.hrm.employeemanagement.application.dto.notification.NotificationCenterPageResult;
import com.hrm.employeemanagement.application.dto.notification.UnreadNotificationCountResult;
import com.hrm.employeemanagement.application.port.inbound.notification.DeleteNotificationItemUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationCenterUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationDetailUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetUnreadNotificationCountUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkAllNotificationItemsReadUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkNotificationItemReadUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.domain.exception.notification.NotificationNotFoundException;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("NotificationController Web API Tests (6 User-Facing Endpoints)")
class NotificationControllerTest {

    private MockMvc mockMvc;

    private GetNotificationCenterUseCase getNotificationCenterUseCase;
    private GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase;
    private GetNotificationDetailUseCase getNotificationDetailUseCase;
    private MarkNotificationItemReadUseCase markNotificationItemReadUseCase;
    private MarkAllNotificationItemsReadUseCase markAllNotificationItemsReadUseCase;
    private DeleteNotificationItemUseCase deleteNotificationItemUseCase;
    private CurrentUserPort currentUserPort;

    private final Long currentUserId = 100L;

    @BeforeEach
    void setUp() {
        getNotificationCenterUseCase = mock(GetNotificationCenterUseCase.class);
        getUnreadNotificationCountUseCase = mock(GetUnreadNotificationCountUseCase.class);
        getNotificationDetailUseCase = mock(GetNotificationDetailUseCase.class);
        markNotificationItemReadUseCase = mock(MarkNotificationItemReadUseCase.class);
        markAllNotificationItemsReadUseCase = mock(MarkAllNotificationItemsReadUseCase.class);
        deleteNotificationItemUseCase = mock(DeleteNotificationItemUseCase.class);
        currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.getCurrentUserId()).thenReturn(Optional.of(currentUserId));

        NotificationController controller = new NotificationController(
                getNotificationCenterUseCase,
                getUnreadNotificationCountUseCase,
                getNotificationDetailUseCase,
                markNotificationItemReadUseCase,
                markAllNotificationItemsReadUseCase,
                deleteNotificationItemUseCase,
                currentUserPort
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications -> Trả về 200 OK kèm danh sách phân trang")
    void getNotifications_success() throws Exception {
        NotificationCenterItemResult item = new NotificationCenterItemResult(
                10L, 1L, "OVERLOAD_WARNING", NotificationLevel.CAO,
                "Cảnh báo quá tải", "Chi tiết", "CAPACITY_WEEK", "2026-W38",
                false, null, LocalDateTime.now()
        );
        NotificationCenterPageResult pageResult = new NotificationCenterPageResult(
                List.of(item), 1L, 1, 0, 20, 1L
        );

        when(getNotificationCenterUseCase.getNotifications(eq(currentUserId), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("status", "ALL")
                        .param("level", "CAO")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(10))
                .andExpect(jsonPath("$.data.items[0].level").value("CAO"))
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count -> Trả về 200 OK kèm số lượng chưa đọc")
    void getUnreadCount_success() throws Exception {
        when(getUnreadNotificationCountUseCase.getUnreadCount(currentUserId))
                .thenReturn(new UnreadNotificationCountResult(5L));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/{id} -> Trả về 200 OK kèm chi tiết thông báo")
    void getNotificationDetail_success() throws Exception {
        NotificationCenterItemResult item = new NotificationCenterItemResult(
                10L, 1L, "OVERLOAD_WARNING", NotificationLevel.CAO,
                "Cảnh báo quá tải", "Chi tiết", "CAPACITY_WEEK", "2026-W38",
                false, null, LocalDateTime.now()
        );

        when(getNotificationDetailUseCase.getNotificationDetail(10L, currentUserId))
                .thenReturn(item);

        mockMvc.perform(get("/api/v1/notifications/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.relatedEntityType").value("CAPACITY_WEEK"))
                .andExpect(jsonPath("$.data.relatedEntityId").value("2026-W38"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/{id} -> Trả về 404 khi không tìm thấy")
    void getNotificationDetail_notFound() throws Exception {
        when(getNotificationDetailUseCase.getNotificationDetail(99L, currentUserId))
                .thenThrow(new NotificationNotFoundException(99L));

        mockMvc.perform(get("/api/v1/notifications/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read -> Trả về 200 OK")
    void markAsRead_success() throws Exception {
        doNothing().when(markNotificationItemReadUseCase).markAsRead(10L, currentUserId);

        mockMvc.perform(patch("/api/v1/notifications/10/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(markNotificationItemReadUseCase, times(1)).markAsRead(10L, currentUserId);
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/read-all -> Trả về 200 OK")
    void markAllAsRead_success() throws Exception {
        doNothing().when(markAllNotificationItemsReadUseCase).markAllAsRead(currentUserId);

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(markAllNotificationItemsReadUseCase, times(1)).markAllAsRead(currentUserId);
    }

    @Test
    @DisplayName("DELETE /api/v1/notifications/{id} -> Trả về 200 OK xóa mềm")
    void deleteNotification_success() throws Exception {
        doNothing().when(deleteNotificationItemUseCase).deleteNotification(10L, currentUserId);

        mockMvc.perform(delete("/api/v1/notifications/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(deleteNotificationItemUseCase, times(1)).deleteNotification(10L, currentUserId);
    }

    @Test
    @DisplayName("GET /api/v1/notifications với status không hợp lệ -> Trả về 400 BAD_REQUEST")
    void getNotifications_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .param("status", "INVALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications với level không hợp lệ -> Trả về 400 BAD_REQUEST")
    void getNotifications_invalidLevel_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .param("level", "SUPER_HIGH")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications với size > 100 -> Trả về 400 BAD_REQUEST")
    void getNotifications_sizeOverLimit_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }
}
