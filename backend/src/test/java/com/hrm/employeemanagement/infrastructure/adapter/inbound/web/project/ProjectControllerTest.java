package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectListUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.project.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetProjectListUseCase getProjectListUseCase;

    @BeforeEach
    void setUp() {
        ProjectController controller =
                new ProjectController(
                        getProjectListUseCase
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new ProjectExceptionHandler()
                )
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/projects tra ve PageResult project")
    void testGetProjects_ReturnsPageResult()
            throws Exception {
        ProjectResult project =
                new ProjectResult(
                        1L,
                        "P-01",
                        "Project One",
                        5L,
                        100L,
                        ProjectStatus.ACTIVE,
                        10L,
                        LocalDateTime.now(),
                        null
                );

        when(getProjectListUseCase.getProjects(0, 20))
                .thenReturn(
                        new PageResult<>(
                                List.of(project),
                                0,
                                20,
                                1L
                        )
                );

        mockMvc.perform(
                        get("/api/v1/projects?page=0&size=20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].projectCode").value("P-01"))
                .andExpect(jsonPath("$.data.content[0].orgUnitId").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/projects tra ve 403 khi thieu PROJECT_READ")
    void testGetProjects_NoProjectRead_Returns403()
            throws Exception {
        when(getProjectListUseCase.getProjects(
                eq(0),
                eq(20)
        )).thenThrow(
                new PermissionDeniedException(
                        PermissionCode.PROJECT_READ
                )
        );

        mockMvc.perform(
                        get("/api/v1/projects?page=0&size=20")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        containsString("PROJECT_READ")
                ));
    }
}
