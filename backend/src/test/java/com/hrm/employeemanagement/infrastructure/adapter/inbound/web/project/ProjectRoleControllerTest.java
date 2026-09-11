package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.project.demand.CreateProjectRoleCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleUsageResult;
import com.hrm.employeemanagement.application.dto.project.demand.UpdateProjectRoleCommand;
import com.hrm.employeemanagement.application.port.inbound.project.ActivateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CheckProjectRoleUsageUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.DeactivateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectRolesUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.UpdateProjectRoleUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectRoleController Web API Tests")
class ProjectRoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetProjectRolesUseCase getProjectRolesUseCase;

    @Mock
    private CreateProjectRoleUseCase createProjectRoleUseCase;

    @Mock
    private UpdateProjectRoleUseCase updateProjectRoleUseCase;

    @Mock
    private DeactivateProjectRoleUseCase deactivateProjectRoleUseCase;

    @Mock
    private ActivateProjectRoleUseCase activateProjectRoleUseCase;

    @Mock
    private CheckProjectRoleUsageUseCase checkProjectRoleUsageUseCase;

    @BeforeEach
    void setUp() {
        ProjectRoleController controller = new ProjectRoleController(
                getProjectRolesUseCase,
                createProjectRoleUseCase,
                updateProjectRoleUseCase,
                deactivateProjectRoleUseCase,
                activateProjectRoleUseCase,
                checkProjectRoleUsageUseCase);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ProjectExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/project-roles trả về danh sách vai trò chuyên môn dự án")
    void testGetProjectRoles_Success() throws Exception {
        List<ProjectRoleResult> roles = List.of(
                new ProjectRoleResult(1L, "DEV", "Developer", "Lập trình viên", 1L, "General", "ACTIVE"),
                new ProjectRoleResult(2L, "TEST", "Tester", "Kiểm thử viên", 1L, "General", "ACTIVE")
        );

        when(getProjectRolesUseCase.getProjectRoles()).thenReturn(roles);

        mockMvc.perform(get("/api/v1/project-roles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].code").value("DEV"))
                .andExpect(jsonPath("$.data[0].name").value("Developer"))
                .andExpect(jsonPath("$.data[0].skillGroupId").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].code").value("TEST"))
                .andExpect(jsonPath("$.data[1].name").value("Tester"));
    }

    @Test
    @DisplayName("GET /api/v1/project-roles?includeInactive=true gọi getProjectRoles(true)")
    void testGetProjectRoles_IncludeInactive() throws Exception {
        List<ProjectRoleResult> allRoles = List.of(
                new ProjectRoleResult(1L, "DEV", "Developer", "Lập trình", 1L, "General", "ACTIVE"),
                new ProjectRoleResult(3L, "OLD", "Old Role", "Cũ", 1L, "General", "INACTIVE")
        );

        when(getProjectRolesUseCase.getProjectRoles(true)).thenReturn(allRoles);

        mockMvc.perform(get("/api/v1/project-roles")
                .param("includeInactive", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[1].status").value("INACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/project-roles tạo mới vai trò chuyên môn thành công (201 Created)")
    void testCreateProjectRole_Success() throws Exception {
        ProjectRoleResult created = new ProjectRoleResult(
                10L, "SEC", "Security Engineer", "Kỹ sư bảo mật", 2L, "DevOps & Cloud", "ACTIVE");
        when(createProjectRoleUseCase.createProjectRole(any(CreateProjectRoleCommand.class))).thenReturn(created);

        String json = """
                {
                    "code": "SEC",
                    "name": "Security Engineer",
                    "description": "Kỹ sư bảo mật",
                    "skillGroupId": 2
                }
                """;

        mockMvc.perform(post("/api/v1/project-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.code").value("SEC"))
                .andExpect(jsonPath("$.data.name").value("Security Engineer"))
                .andExpect(jsonPath("$.data.skillGroupId").value(2))
                .andExpect(jsonPath("$.data.skillGroupName").value("DevOps & Cloud"));
    }

    @Test
    @DisplayName("PUT /api/v1/project-roles/{id} cập nhật vai trò chuyên môn thành công")
    void testUpdateProjectRole_Success() throws Exception {
        ProjectRoleResult updated = new ProjectRoleResult(
                10L, "SEC", "Lead Security Engineer", "Trưởng nhóm bảo mật", 2L, "DevOps & Cloud", "ACTIVE");
        when(updateProjectRoleUseCase.updateProjectRole(any(UpdateProjectRoleCommand.class))).thenReturn(updated);

        String json = """
                {
                    "name": "Lead Security Engineer",
                    "description": "Trưởng nhóm bảo mật",
                    "skillGroupId": 2
                }
                """;

        mockMvc.perform(put("/api/v1/project-roles/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Lead Security Engineer"));
    }

    @Test
    @DisplayName("PATCH /api/v1/project-roles/{id}/deactivate ngừng sử dụng vai trò thành công")
    void testDeactivateProjectRole_Success() throws Exception {
        ProjectRoleResult deactivated = new ProjectRoleResult(
                10L, "SEC", "Security Engineer", "Kỹ sư bảo mật", 2L, "DevOps & Cloud", "INACTIVE");
        when(deactivateProjectRoleUseCase.deactivateProjectRole(10L)).thenReturn(deactivated);

        mockMvc.perform(patch("/api/v1/project-roles/10/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/project-roles/{id}/activate kích hoạt lại vai trò thành công")
    void testActivateProjectRole_Success() throws Exception {
        ProjectRoleResult activated = new ProjectRoleResult(
                10L, "SEC", "Security Engineer", "Kỹ sư bảo mật", 2L, "DevOps & Cloud", "ACTIVE");
        when(activateProjectRoleUseCase.activateProjectRole(10L)).thenReturn(activated);

        mockMvc.perform(patch("/api/v1/project-roles/10/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/project-roles/{id}/usage kiểm tra tình trạng sử dụng vai trò")
    void testCheckUsage_Success() throws Exception {
        ProjectRoleUsageResult usage = new ProjectRoleUsageResult(1L, true, 5L, 2L, "Đang được sử dụng");
        when(checkProjectRoleUsageUseCase.checkUsage(1L)).thenReturn(usage);

        mockMvc.perform(get("/api/v1/project-roles/1/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inUse").value(true))
                .andExpect(jsonPath("$.data.demandCount").value(5))
                .andExpect(jsonPath("$.data.employeeCount").value(2));
    }
}