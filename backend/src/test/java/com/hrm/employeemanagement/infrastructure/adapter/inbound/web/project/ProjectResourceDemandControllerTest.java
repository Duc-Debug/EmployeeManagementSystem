package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;
import com.hrm.employeemanagement.application.dto.project.demand.RoleResourceDemandResult;
import com.hrm.employeemanagement.application.dto.project.demand.WeeklyDemandItemResult;
import com.hrm.employeemanagement.application.port.inbound.project.DeleteProjectResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.EstimateResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectResourceDemandUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectResourceDemandController Web API Tests")
class ProjectResourceDemandControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EstimateResourceDemandUseCase estimateResourceDemandUseCase;

    @Mock
    private GetProjectResourceDemandUseCase getProjectResourceDemandUseCase;

    @Mock
    private DeleteProjectResourceDemandUseCase deleteProjectResourceDemandUseCase;

    @BeforeEach
    void setUp() {
        ProjectResourceDemandController controller = new ProjectResourceDemandController(
                estimateResourceDemandUseCase,
                getProjectResourceDemandUseCase,
                deleteProjectResourceDemandUseCase);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new ProjectExceptionHandler(),
                        new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/resource-demands thành công (TC-01)")
    void testEstimateDemand_Success() throws Exception {
        ProjectResourceDemandSummaryResult summary = createSampleSummary(false, null);

        when(estimateResourceDemandUseCase.estimateDemand(any(EstimateResourceDemandCommand.class)))
                .thenReturn(summary);

        String json = """
                {
                    "roleId": 4,
                    "hoursPerWeek": 20.00
                }
                """;

        mockMvc.perform(post("/api/v1/projects/1/resource-demands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ước lượng nhu cầu nhân sự thành công"))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.totalDemandHours").value(160.0))
                .andExpect(jsonPath("$.data.exceedsEstimatedHours").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/resource-demands có cảnh báo vượt quy mô (TC-02)")
    void testEstimateDemand_ExceedsBudget_Warning() throws Exception {
        ProjectResourceDemandSummaryResult summary = createSampleSummary(true, "Nhu cầu nhân sự vượt quá quy mô dự án");

        when(estimateResourceDemandUseCase.estimateDemand(any(EstimateResourceDemandCommand.class)))
                .thenReturn(summary);

        String json = """
                {
                    "roleId": 4,
                    "hoursPerWeek": 20.00
                }
                """;

        mockMvc.perform(post("/api/v1/projects/1/resource-demands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ước lượng nhu cầu nhân sự thành công (Có cảnh báo)"))
                .andExpect(jsonPath("$.data.exceedsEstimatedHours").value(true))
                .andExpect(jsonPath("$.data.warningMessage").value(containsString("vượt quá quy mô")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/resource-demands trả về 403 khi không có quyền (TC-03)")
    void testEstimateDemand_Forbidden_Returns403() throws Exception {
        when(estimateResourceDemandUseCase.estimateDemand(any(EstimateResourceDemandCommand.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE));

        String json = """
                {
                    "roleId": 4,
                    "hoursPerWeek": 20.00
                }
                """;

        mockMvc.perform(post("/api/v1/projects/1/resource-demands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/resource-demands trả về 404 khi dự án không tồn tại")
    void testEstimateDemand_ProjectNotFound_Returns404() throws Exception {
        when(estimateResourceDemandUseCase.estimateDemand(any(EstimateResourceDemandCommand.class)))
                .thenThrow(new ProjectNotFoundException("Không tìm thấy dự án với ID: 999"));

        String json = """
                {
                    "roleId": 4,
                    "hoursPerWeek": 20.00
                }
                """;

        mockMvc.perform(post("/api/v1/projects/999/resource-demands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("999")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/resource-demands trả về 400 khi số giờ âm hoặc vượt 168")
    void testEstimateDemand_InvalidHours_Returns400() throws Exception {
        String json = """
                {
                    "roleId": 4,
                    "hoursPerWeek": -5.00
                }
                """;

        mockMvc.perform(post("/api/v1/projects/1/resource-demands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/resource-demands lấy dữ liệu thành công")
    void testGetDemands_Success() throws Exception {
        ProjectResourceDemandSummaryResult summary = createSampleSummary(false, null);

        when(getProjectResourceDemandUseCase.getProjectResourceDemands(1L))
                .thenReturn(summary);

        mockMvc.perform(get("/api/v1/projects/1/resource-demands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.totalDemandHours").value(160.0));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/resource-demands trả về 409 khi gặp DuplicateResourceDemandException")
    void testEstimateDemand_DuplicateConflict_Returns409() throws Exception {
        when(estimateResourceDemandUseCase.estimateDemand(any(EstimateResourceDemandCommand.class)))
                .thenThrow(new com.hrm.employeemanagement.domain.exception.project.DuplicateResourceDemandException(
                        "Xung đột dữ liệu nhu cầu nhân sự sau nhiều lần thử lại"));

        String json = """
                {
                    "roleId": 4,
                    "hoursPerWeek": 20.00
                }
                """;

        mockMvc.perform(post("/api/v1/projects/1/resource-demands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Xung đột dữ liệu")));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/resource-demands/{roleId} thành công")
    void testDeleteDemand_Success() throws Exception {
        ProjectResourceDemandSummaryResult summary = createSampleSummary(false, null);

        when(deleteProjectResourceDemandUseCase.deleteDemand(1L, 4L)).thenReturn(summary);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/projects/1/resource-demands/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa ước lượng nhu cầu nhân sự của vai trò thành công"))
                .andExpect(jsonPath("$.data.projectId").value(1));
    }

    private ProjectResourceDemandSummaryResult createSampleSummary(boolean exceeds, String warningMsg) {
        WeeklyDemandItemResult weekItem = new WeeklyDemandItemResult(
                2026, 41, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11), new BigDecimal("20.00"));
        RoleResourceDemandResult roleResult = new RoleResourceDemandResult(
                4L, "VT-04", "Lập trình viên", new BigDecimal("160.00"), List.of(weekItem));

        return new ProjectResourceDemandSummaryResult(
                1L,
                "PRJ-01",
                "Dự án Mẫu",
                new BigDecimal("200.00"),
                new BigDecimal("160.00"),
                exceeds,
                warningMsg,
                List.of(roleResult));
    }
}