package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.allocation.AllocationNotificationItemResult;
import com.hrm.employeemanagement.application.dto.allocation.AllocationNotificationPageResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetAllocationNotificationsUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AllocationNotificationController Web API Tests (NCL-07-CN-003, AC-03)")
class AllocationNotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetAllocationNotificationsUseCase getAllocationNotificationsUseCase;

    @BeforeEach
    void setUp() {
        AllocationNotificationController controller = new AllocationNotificationController(getAllocationNotificationsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/allocations/notifications - Thành công 200 OK")
    void getAllocationNotifications_Success() throws Exception {
        AllocationNotificationItemResult item = new AllocationNotificationItemResult(
                1L, 20L, "Trần PM", 10L, "Lê RM",
                "ALLOCATION_CHANGED", "PROJECT_ALLOCATION", 5L,
                "Thay đổi phân bổ", "Nội dung chi tiết", false, LocalDateTime.now()
        );
        AllocationNotificationPageResult pageResult = new AllocationNotificationPageResult(
                List.of(item), 1L, 1, 0
        );

        when(getAllocationNotificationsUseCase.getAllocationNotifications(eq(5L), eq(0), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/allocations/notifications")
                        .param("projectId", "5")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Thay đổi phân bổ"))
                .andExpect(jsonPath("$.data.items[0].recipientName").value("Trần PM"));
    }

    @Test
    @DisplayName("GET /api/v1/allocations/notifications - Bị từ chối quyền truy cập 403 Forbidden (AC-03)")
    void getAllocationNotifications_Forbidden() throws Exception {
        when(getAllocationNotificationsUseCase.getAllocationNotifications(eq(99L), anyInt(), anyInt()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ));

        mockMvc.perform(get("/api/v1/allocations/notifications")
                        .param("projectId", "99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
