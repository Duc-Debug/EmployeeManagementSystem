package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.hrm.employeemanagement.application.dto.task.CreateTaskCommand;
import com.hrm.employeemanagement.application.dto.task.TaskNodeResult;
import com.hrm.employeemanagement.application.dto.task.TaskResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskCommand;
import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectWbsUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.task.CyclicTaskHierarchyException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateTaskUseCase createTaskUseCase;

    @Mock
    private UpdateTaskUseCase updateTaskUseCase;

    @Mock
    private GetProjectWbsUseCase getProjectWbsUseCase;

    @BeforeEach
    void setUp() {
        TaskController controller = new TaskController(
                createTaskUseCase,
                updateTaskUseCase,
                getProjectWbsUseCase);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new TaskExceptionHandler(),
                        new GlobalExceptionHandler())
                .build();
    }

    private TaskResult createSampleResult(Long id, String name, TaskType type) {
        return new TaskResult(
                id,
                100L,
                null,
                "PRJ-01-T001",
                name,
                "Mô tả",
                type,
                null,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                TaskStatus.TODO,
                1,
                1L,
                LocalDateTime.now(),
                null,
                0L);
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/tasks - Thành công trả về 201 Created")
    void testCreateTask_Success() throws Exception {
        when(createTaskUseCase.createTask(any(CreateTaskCommand.class)))
                .thenReturn(createSampleResult(1L, "Thiết kế API", TaskType.TASK));

        String requestJson = """
                {
                    "name": "Thiết kế API",
                    "taskType": "TASK",
                    "estimatedHours": 10.00,
                    "sortOrder": 1
                }
                """;

        mockMvc.perform(post("/api/v1/projects/100/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Thiết kế API"))
                .andExpect(jsonPath("$.data.taskCode").value("PRJ-01-T001"));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId}/tasks/{taskId} - Thành công trả về 200 OK")
    void testUpdateTask_Success() throws Exception {
        when(updateTaskUseCase.updateTask(any(UpdateTaskCommand.class)))
                .thenReturn(createSampleResult(1L, "Tên đã sửa", TaskType.TASK));

        String requestJson = """
                {
                    "name": "Tên đã sửa",
                    "estimatedHours": 15.00
                }
                """;

        mockMvc.perform(put("/api/v1/projects/100/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Tên đã sửa"));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/wbs - Thành công trả về 200 OK")
    void testGetProjectWbs_Success() throws Exception {
        TaskNodeResult node = TaskNodeResult.from(createSampleResult(1L, "Gốc", TaskType.CATEGORY));
        when(getProjectWbsUseCase.getProjectWbs(100L)).thenReturn(List.of(node));

        mockMvc.perform(get("/api/v1/projects/100/wbs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Gốc"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/tasks - Bị chặn khi dự án đã đóng (400 Bad Request)")
    void testCreateTask_ProjectClosed_Returns400() throws Exception {
        when(createTaskUseCase.createTask(any(CreateTaskCommand.class)))
                .thenThrow(new ProjectClosedException(100L));

        String requestJson = """
                {
                    "name": "Task mới",
                    "taskType": "TASK"
                }
                """;

        mockMvc.perform(post("/api/v1/projects/100/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Không thể thêm hoặc chỉnh sửa công việc trong dự án đã đóng")));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId}/tasks/{taskId} - Phát hiện chu trình lặp (400 Bad Request)")
    void testUpdateTask_CyclicHierarchy_Returns400() throws Exception {
        when(updateTaskUseCase.updateTask(any(UpdateTaskCommand.class)))
                .thenThrow(CyclicTaskHierarchyException.forCycle(1L, 2L));

        String requestJson = """
                {
                    "name": "Task 1",
                    "parentId": 2
                }
                """;

        mockMvc.perform(put("/api/v1/projects/100/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Phát hiện chu trình phân cấp")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/wbs - Không có quyền (403 Forbidden)")
    void testGetProjectWbs_PermissionDenied_Returns403() throws Exception {
        when(getProjectWbsUseCase.getProjectWbs(100L))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_READ));

        mockMvc.perform(get("/api/v1/projects/100/wbs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId}/tasks/{taskId} - Không tìm thấy task (404 Not Found)")
    void testUpdateTask_NotFound_Returns404() throws Exception {
        when(updateTaskUseCase.updateTask(any(UpdateTaskCommand.class)))
                .thenThrow(new TaskNotFoundException(999L));

        String requestJson = """
                {
                    "name": "Task 999"
                }
                """;

        mockMvc.perform(put("/api/v1/projects/100/tasks/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Không tìm thấy hạng mục/công việc với ID: 999")));
    }
}
