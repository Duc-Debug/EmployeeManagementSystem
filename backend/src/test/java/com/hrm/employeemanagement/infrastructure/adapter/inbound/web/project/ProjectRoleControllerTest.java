package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectRolesUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectRoleController Web API Tests")
class ProjectRoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetProjectRolesUseCase getProjectRolesUseCase;

    @BeforeEach
    void setUp() {
        ProjectRoleController controller = new ProjectRoleController(getProjectRolesUseCase);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/project-roles trả về danh sách vai trò chuyên môn dự án")
    void testGetProjectRoles_Success() throws Exception {
        List<ProjectRoleResult> roles = List.of(
                new ProjectRoleResult(1L, "DEV", "Developer", "Lập trình viên"),
                new ProjectRoleResult(2L, "TEST", "Tester", "Kiểm thử viên")
        );

        when(getProjectRolesUseCase.getProjectRoles()).thenReturn(roles);

        mockMvc.perform(get("/api/v1/project-roles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].code").value("DEV"))
                .andExpect(jsonPath("$.data[0].name").value("Developer"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].code").value("TEST"))
                .andExpect(jsonPath("$.data[1].name").value("Tester"));
    }
}
