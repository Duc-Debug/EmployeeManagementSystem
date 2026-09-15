package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.task.tracking.ProjectTaskTrackingResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingItemResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingQuery;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectTaskTrackingUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.TaskStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectTaskTrackingController Web Tests (NCL-04-CN-003)")
class ProjectTaskTrackingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetProjectTaskTrackingUseCase getProjectTaskTrackingUseCase;

    @BeforeEach
    void setUp() {
        ProjectTaskTrackingController controller = new ProjectTaskTrackingController(getProjectTaskTrackingUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TaskExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("NCL-04-CN-003-TC-01: GET /api/v1/projects/{projectId}/task-tracking - Trả về 200 OK kèm dữ liệu bảng theo dõi")
    void shouldReturn200WhenGetTaskTrackingSucceeds() throws Exception {
        TaskTrackingItemResult item = new TaskTrackingItemResult(
                1L, "TSK-01", "Thiết kế kiến trúc", "Mô tả", 10L, "Phân tích & Thiết kế",
                Collections.emptyList(), TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                BigDecimal.valueOf(40), BigDecimal.valueOf(20), BigDecimal.valueOf(50),
                false, com.hrm.employeemanagement.domain.task.TaskBudgetBurnStatus.SAFE,
                false, 0L, 1
        );

        ProjectTaskTrackingResult result = new ProjectTaskTrackingResult(
                100L, "PRJ-01", "Dự án CRM", ProjectStatus.ACTIVE,
                1, 0, 0, 1, BigDecimal.valueOf(40), BigDecimal.valueOf(20),
                null, List.of(item)
        );

        when(getProjectTaskTrackingUseCase.getTaskTracking(any(TaskTrackingQuery.class))).thenReturn(result);

        mockMvc.perform(get("/api/v1/projects/100/task-tracking?keyword=thiet-ke")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy bảng theo dõi công việc dự án thành công"))
                .andExpect(jsonPath("$.data.projectId").value(100))
                .andExpect(jsonPath("$.data.projectCode").value("PRJ-01"))
                .andExpect(jsonPath("$.data.projectName").value("Dự án CRM"))
                .andExpect(jsonPath("$.data.totalTasks").value(1))
                .andExpect(jsonPath("$.data.tasks[0].taskCode").value("TSK-01"))
                .andExpect(jsonPath("$.data.tasks[0].categoryName").value("Phân tích & Thiết kế"))
                .andExpect(jsonPath("$.data.tasks[0].isOverBudget").value(false))
                .andExpect(jsonPath("$.data.tasks[0].budgetBurnStatus").value("SAFE"));

        verify(getProjectTaskTrackingUseCase).getTaskTracking(any(TaskTrackingQuery.class));
    }

    @Test
    @DisplayName("NCL-04-CN-003-TC-04: GET /api/v1/projects/{projectId}/task-tracking - Không có quyền trả về 403 FORBIDDEN")
    void shouldReturn403WhenPermissionDenied() throws Exception {
        when(getProjectTaskTrackingUseCase.getTaskTracking(any(TaskTrackingQuery.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_READ));

        mockMvc.perform(get("/api/v1/projects/100/task-tracking")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Quy tắc QTN-04: Dự án đã đóng (CLOSED) vẫn trả về 200 OK ở chế độ xem lưu trữ")
    void shouldReturn200WhenProjectClosed() throws Exception {
        ProjectTaskTrackingResult closedResult = new ProjectTaskTrackingResult(
                100L,
                "PRJ-CLOSED",
                "Dự án đã đóng",
                ProjectStatus.CLOSED,
                5,
                0,
                5,
                0,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(95),
                null,
                Collections.emptyList()
        );
        when(getProjectTaskTrackingUseCase.getTaskTracking(any(TaskTrackingQuery.class)))
                .thenReturn(closedResult);

        mockMvc.perform(get("/api/v1/projects/100/task-tracking")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data.totalTasks").value(5));
    }

    @Test
    @DisplayName("Ngoại lệ: Dự án không tồn tại trả về 404 NOT FOUND")
    void shouldReturn404WhenProjectNotFound() throws Exception {
        when(getProjectTaskTrackingUseCase.getTaskTracking(any(TaskTrackingQuery.class)))
                .thenThrow(new ProjectNotFoundException("Không tìm thấy dự án với ID: 999"));

        mockMvc.perform(get("/api/v1/projects/999/task-tracking")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
