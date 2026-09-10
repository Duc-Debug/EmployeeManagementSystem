package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.project.UpdateProjectCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectDetailUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectListUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.UpdateProjectUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetProjectListUseCase getProjectListUseCase;

    @Mock
    private GetProjectDetailUseCase getProjectDetailUseCase;

    @Mock
    private CreateProjectUseCase createProjectUseCase;

    @Mock
    private UpdateProjectUseCase updateProjectUseCase;

    @Mock
    private com.hrm.employeemanagement.application.port.inbound.projecttemplate.CreateProjectFromTemplateUseCase createProjectFromTemplateUseCase;

    @Mock
    private com.hrm.employeemanagement.application.port.inbound.projecttemplate.GetProjectTemplatesUseCase getProjectTemplatesUseCase;

    @BeforeEach
    void setUp() {
        ProjectController controller =
                new ProjectController(
                        getProjectListUseCase,
                        getProjectDetailUseCase,
                        createProjectUseCase,
                        updateProjectUseCase,
                        createProjectFromTemplateUseCase,
                        getProjectTemplatesUseCase
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new ProjectExceptionHandler(),
                        new GlobalExceptionHandler()
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
                        null,
                        null,
                        java.math.BigDecimal.ZERO,
                        null,
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

    @Test
    @DisplayName("GET /api/v1/projects/{id} tra ve detail project")
    void testGetProjectById_ReturnsProject()
            throws Exception {
        ProjectResult project =
                projectResult(7L);

        when(getProjectDetailUseCase.getProjectById(7L))
                .thenReturn(project);

        mockMvc.perform(
                        get("/api/v1/projects/7")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.projectCode").value("P-07"));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} tra ve 403 khi ngoai data scope")
    void testGetProjectById_OutsideScope_Returns403()
            throws Exception {
        when(getProjectDetailUseCase.getProjectById(8L))
                .thenThrow(
                        new PermissionDeniedException(
                                PermissionCode.PROJECT_READ
                        )
                );

        mockMvc.perform(
                        get("/api/v1/projects/8")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        containsString("PROJECT_READ")
                ));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} tra ve 404 khi project khong ton tai")
    void testGetProjectById_NotFound_Returns404()
            throws Exception {
        when(getProjectDetailUseCase.getProjectById(999L))
                .thenThrow(
                        new ProjectNotFoundException(
                                "Khong tim thay du an voi ID: 999"
                        )
                );

        mockMvc.perform(
                        get("/api/v1/projects/999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        containsString("999")
                ));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{id} cap nhat thanh cong")
    void testUpdateProject_Success() throws Exception {
        String jsonPayload = """
            {
                "projectName": "Dự án mới",
                "managerId": 10,
                "startDate": "2026-01-01",
                "endDate": "2026-12-31",
                "estimatedHours": 100.50,
                "description": "Mô tả hợp lệ"
            }
            """;

        ProjectResult result = projectResult(1L);
        when(updateProjectUseCase.updateProject(any(UpdateProjectCommand.class))).thenReturn(result);

        mockMvc.perform(
                put("/api/v1/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật dự án thành công"));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{id} tra ve 400 khi estimatedHours co qua 2 chu so thap phan (e.g. 0.00000001)")
    void testUpdateProject_InvalidEstimatedHoursFraction_Returns400() throws Exception {
        String jsonPayload = """
            {
                "projectName": "Dự án mới",
                "managerId": 10,
                "startDate": "2026-01-01",
                "endDate": "2026-12-31",
                "estimatedHours": 0.00000001,
                "description": "Mô tả hợp lệ"
            }
            """;

        mockMvc.perform(
                put("/api/v1/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("estimatedHours")));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{id} tra ve 400 khi description vuot qua 2000 ky tu")
    void testUpdateProject_DescriptionTooLong_Returns400() throws Exception {
        String longDescription = "X".repeat(2001);
        String jsonPayload = String.format("""
            {
                "projectName": "Dự án mới",
                "managerId": 10,
                "startDate": "2026-01-01",
                "endDate": "2026-12-31",
                "estimatedHours": 100.00,
                "description": "%s"
            }
            """, longDescription);

        mockMvc.perform(
                put("/api/v1/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("description")));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{id} tra ve 400 khi projectName de trong")
    void testUpdateProject_BlankProjectName_Returns400() throws Exception {
        String jsonPayload = """
            {
                "projectName": "   ",
                "managerId": 10,
                "startDate": "2026-01-01",
                "endDate": "2026-12-31",
                "estimatedHours": 100.00,
                "description": "Mô tả"
            }
            """;

        mockMvc.perform(
                put("/api/v1/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("projectName")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/from-template tra ve 201 khi du lieu hop le")
    void testCreateProjectFromTemplate_Success_Returns201() throws Exception {
        when(createProjectFromTemplateUseCase.createProjectFromTemplate(any())).thenReturn(projectResult(10L));

        String jsonPayload = """
            {
                "templateId": 1,
                "projectName": "Dự án mới từ mẫu",
                "orgUnitId": 5
            }
            """;

        mockMvc.perform(
                post("/api/v1/projects/from-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10L));
    }

    @Test
    @DisplayName("POST /api/v1/projects/from-template tra ve 400 khi templateId bi null")
    void testCreateProjectFromTemplate_NullTemplateId_Returns400() throws Exception {
        String jsonPayload = """
            {
                "projectName": "Dự án thiếu template",
                "orgUnitId": 5
            }
            """;

        mockMvc.perform(
                post("/api/v1/projects/from-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/from-template tra ve 404 khi template khong ton tai")
    void testCreateProjectFromTemplate_TemplateNotFound_Returns404() throws Exception {
        when(createProjectFromTemplateUseCase.createProjectFromTemplate(any()))
                .thenThrow(new com.hrm.employeemanagement.domain.exception.projecttemplate.ProjectTemplateNotFoundException(999L));

        String jsonPayload = """
            {
                "templateId": 999,
                "projectName": "Dự án mẫu không tồn tại",
                "orgUnitId": 5
            }
            """;

        mockMvc.perform(
                post("/api/v1/projects/from-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/projects/templates tra ve danh sach template active")
    void testGetActiveTemplates() throws Exception {
        com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateSummaryResult item =
                new com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateSummaryResult(
                        1L,
                        "TPL-DEV-001",
                        "Mẫu dự án triển khai phần mềm",
                        "Mô tả mẫu",
                        true,
                        new java.math.BigDecimal("120.00"),
                        3,
                        6
                );
        org.mockito.Mockito.when(getProjectTemplatesUseCase.getActiveTemplates()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/projects/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].templateCode").value("TPL-DEV-001"))
                .andExpect(jsonPath("$.data[0].totalEstimatedHours").value(120.00));
    }

    @Test
    @DisplayName("GET /api/v1/projects/templates/{id} tra ve chi tiet template")
    void testGetTemplateDetail() throws Exception {
        com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateDetailResult detail =
                new com.hrm.employeemanagement.application.dto.projecttemplate.ProjectTemplateDetailResult(
                        1L,
                        "TPL-DEV-001",
                        "Mẫu dự án triển khai phần mềm",
                        "Mô tả mẫu",
                        true,
                        new java.math.BigDecimal("120.00"),
                        3,
                        6,
                        List.of()
                );
        org.mockito.Mockito.when(getProjectTemplatesUseCase.getTemplateDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/projects/templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Mẫu dự án triển khai phần mềm"));
    }

    private ProjectResult projectResult(Long id) {
        return new ProjectResult(
                id,
                "P-0" + id,
                "Project " + id,
                5L,
                100L,
                null,
                null,
                java.math.BigDecimal.ZERO,
                null,
                ProjectStatus.ACTIVE,
                10L,
                LocalDateTime.now(),
                null
        );
    }
}
