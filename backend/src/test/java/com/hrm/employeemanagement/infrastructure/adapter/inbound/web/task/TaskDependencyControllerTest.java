package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.task.dependency.CreateTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.DeleteTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyGraphResult;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyResult;
import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.DeleteTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskDependenciesUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.CreateTaskDependencyRequest;

@ExtendWith(MockitoExtension.class)
class TaskDependencyControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CreateTaskDependencyUseCase createUseCase;

    @Mock
    private DeleteTaskDependencyUseCase deleteUseCase;

    @Mock
    private GetTaskDependenciesUseCase getUseCase;

    private TaskDependencyResult sampleResult;

    @BeforeEach
    void setUp() {
        TaskDependencyController controller = new TaskDependencyController(createUseCase, deleteUseCase, getUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TaskExceptionHandler())
                .build();

        sampleResult = new TaskDependencyResult(
                1L, 1L, 10L, "TK-001", "Thiết kế", 20L, "TK-002", "Lập trình", "FINISH_TO_START", 0, 100L, null
        );
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/tasks/dependencies - Tạo phụ thuộc công việc thành công (HTTP 201)")
    void shouldCreateDependencySuccessfully() throws Exception {
        when(createUseCase.createDependency(any(CreateTaskDependencyCommand.class))).thenReturn(sampleResult);

        CreateTaskDependencyRequest request = new CreateTaskDependencyRequest(10L, 20L, "FINISH_TO_START", 0);

        mockMvc.perform(post("/api/v1/projects/1/tasks/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.predecessorTaskCode").value("TK-001"))
                .andExpect(jsonPath("$.data.successorTaskCode").value("TK-002"));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/tasks/dependencies - Lấy đồ thị phụ thuộc công việc (HTTP 200)")
    void shouldGetTaskDependenciesSuccessfully() throws Exception {
        TaskDependencyGraphResult graphResult = new TaskDependencyGraphResult(
                1L, "PROJ-01", "Dự án A", List.of(sampleResult)
        );

        when(getUseCase.getTaskDependencies(1L)).thenReturn(graphResult);

        mockMvc.perform(get("/api/v1/projects/1/tasks/dependencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.dependencies[0].predecessorTaskCode").value("TK-001"));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/tasks/dependencies/{dependencyId} - Xóa phụ thuộc công việc (HTTP 200)")
    void shouldDeleteDependencySuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/1/tasks/dependencies/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(deleteUseCase).deleteDependency(new DeleteTaskDependencyCommand(1L, 100L));
    }
}
