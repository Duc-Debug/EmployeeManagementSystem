package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.template;

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

import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateResult;
import com.hrm.employeemanagement.application.dto.allocation.template.CreateRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.PreviewRoleAllocationResult;
import com.hrm.employeemanagement.application.dto.allocation.template.ProjectRoleStructureItem;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateDetailResult;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateItemResult;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.ApplyRoleAllocationTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.CreateRoleAllocationTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.GetProjectRoleAllocationStructureUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.GetRoleAllocationTemplatesUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.PreviewRoleAllocationSuggestionUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleAllocationTemplateController Web API Tests")
class RoleAllocationTemplateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateRoleAllocationTemplateUseCase createUseCase;
    @Mock
    private GetRoleAllocationTemplatesUseCase getTemplatesUseCase;
    @Mock
    private GetProjectRoleAllocationStructureUseCase getStructureUseCase;
    @Mock
    private PreviewRoleAllocationSuggestionUseCase previewUseCase;
    @Mock
    private ApplyRoleAllocationTemplateUseCase applyUseCase;

    @BeforeEach
    void setUp() {
        RoleAllocationTemplateController controller = new RoleAllocationTemplateController(
                createUseCase,
                getTemplatesUseCase,
                getStructureUseCase,
                previewUseCase,
                applyUseCase
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/role-allocation-templates thành công -> 201 Created")
    void createTemplate_Success_ShouldReturn201() throws Exception {
        RoleAllocationTemplateDetailResult result = new RoleAllocationTemplateDetailResult(
                1L,
                "TPL_01",
                "Mẫu chuẩn",
                "Mô tả",
                100L,
                99L,
                LocalDateTime.now(),
                null,
                0L,
                List.of(new RoleAllocationTemplateItemResult(10L, 1L, "DEV", "Developer", BigDecimal.valueOf(40.0)))
        );

        when(createUseCase.createTemplate(any(CreateRoleAllocationTemplateCommand.class))).thenReturn(result);

        String json = """
                {
                    "templateCode": "TPL_01",
                    "name": "Mẫu chuẩn",
                    "description": "Mô tả",
                    "sourceProjectId": 100,
                    "items": [
                        {
                            "roleId": 1,
                            "hoursPerWeek": 40.0
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/role-allocation-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.templateCode").value("TPL_01"))
                .andExpect(jsonPath("$.data.items[0].roleCode").value("DEV"));
    }

    @Test
    @DisplayName("GET /api/v1/role-allocation-templates thành công -> 200 OK")
    void getAllTemplates_Success_ShouldReturn200() throws Exception {
        List<RoleAllocationTemplateSummaryResult> list = List.of(
                new RoleAllocationTemplateSummaryResult(1L, "TPL_01", "Mẫu 1", "Mô tả", 100L, 3, 99L, LocalDateTime.now())
        );

        when(getTemplatesUseCase.getAllTemplates()).thenReturn(list);

        mockMvc.perform(get("/api/v1/role-allocation-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].templateCode").value("TPL_01"));
    }

    @Test
    @DisplayName("GET /api/v1/role-allocation-templates/extract-from-project/{projectId} thành công -> 200 OK")
    void extractFromProject_Success_ShouldReturn200() throws Exception {
        List<ProjectRoleStructureItem> structure = List.of(
                new ProjectRoleStructureItem(1L, "DEV", "Developer", BigDecimal.valueOf(40.0)),
                new ProjectRoleStructureItem(2L, "TEST", "Tester", BigDecimal.valueOf(20.0))
        );

        when(getStructureUseCase.getStructureFromProject(100L)).thenReturn(structure);

        mockMvc.perform(get("/api/v1/role-allocation-templates/extract-from-project/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].roleCode").value("DEV"))
                .andExpect(jsonPath("$.data[1].roleCode").value("TEST"));
    }

    @Test
    @DisplayName("POST /api/v1/role-allocation-templates/{id}/preview-apply/{targetProjectId} thành công -> 200 OK")
    void previewApply_Success_ShouldReturn200() throws Exception {
        PreviewRoleAllocationResult preview = new PreviewRoleAllocationResult(
                1L,
                "TPL_01",
                "Mẫu chuẩn",
                200L,
                "Dự án mới",
                4,
                List.of(new PreviewRoleAllocationResult.RoleSuggestionItemResult(
                        1L, "DEV", "Developer", BigDecimal.valueOf(40.0), 101L, "Nguyễn Văn A", "EMP01", true, null
                )),
                false,
                List.of()
        );

        when(previewUseCase.previewSuggestion(1L, 200L)).thenReturn(preview);

        mockMvc.perform(post("/api/v1/role-allocation-templates/1/preview-apply/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.suggestions[0].suggestedEmployeeName").value("Nguyễn Văn A"));
    }

    @Test
    @DisplayName("POST /api/v1/role-allocation-templates/{id}/confirm-apply/{targetProjectId} thành công -> 200 OK")
    void confirmApply_Success_ShouldReturn200() throws Exception {
        ApplyRoleAllocationTemplateResult result = new ApplyRoleAllocationTemplateResult(
                1L, 200L, 1, 1, 0, List.of(), "Áp mẫu phân bổ vai trò vào dự án thành công!"
        );

        when(applyUseCase.applyTemplate(any(ApplyRoleAllocationTemplateCommand.class))).thenReturn(result);

        String json = """
                {
                    "assignments": [
                        {
                            "roleId": 1,
                            "employeeId": 101,
                            "hoursPerWeek": 40.0
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/role-allocation-templates/1/confirm-apply/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appliedRolesCount").value(1));
    }

    @Test
    @DisplayName("Không có quyền VT-03 -> Trả về 403 Forbidden")
    void unauthorized_ShouldReturn403() throws Exception {
        when(getTemplatesUseCase.getAllTemplates())
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE));

        mockMvc.perform(get("/api/v1/role-allocation-templates"))
                .andExpect(status().isForbidden());
    }
}

