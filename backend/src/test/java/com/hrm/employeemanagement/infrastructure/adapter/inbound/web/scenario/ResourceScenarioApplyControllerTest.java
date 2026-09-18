package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioCommand;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioPreviewResult;
import com.hrm.employeemanagement.application.dto.scenario.ApplyScenarioResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.ApplyScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.PreviewApplyScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.RefreshScenarioBaselineUseCase;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioBaselineStaleException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.ApplyScenarioRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceScenarioApplyController Web API Tests (NCL-08-CN-003)")
class ResourceScenarioApplyControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private PreviewApplyScenarioUseCase previewApplyUseCase;
    @Mock private ApplyScenarioUseCase applyScenarioUseCase;
    @Mock private RefreshScenarioBaselineUseCase refreshBaselineUseCase;

    @BeforeEach
    void setUp() {
        ResourceScenarioApplyController controller = new ResourceScenarioApplyController(
                previewApplyUseCase,
                applyScenarioUseCase,
                refreshBaselineUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/resource-scenarios/{id}/apply-preview - Lấy bảng so sánh thành công -> 200 OK")
    void previewApply_Success() throws Exception {
        ApplyScenarioPreviewResult mockResult = new ApplyScenarioPreviewResult(
                1L,
                "SCN-01",
                "Kịch bản test",
                200L,
                "Dự án Alpha",
                false,
                List.of(),
                List.of(new ApplyScenarioPreviewResult.WeeklyHeaderResult(2026, 38, "T38")),
                List.of(),
                1,
                BigDecimal.valueOf(20)
        );

        when(previewApplyUseCase.previewApplyScenario(1L, 200L)).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/resource-scenarios/1/apply-preview")
                        .param("targetProjectId", "200")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scenarioCode").value("SCN-01"))
                .andExpect(jsonPath("$.data.isBaselineStale").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/apply - Áp dụng kịch bản thành công -> 200 OK")
    void applyScenario_Success() throws Exception {
        ApplyScenarioResult mockResult = new ApplyScenarioResult(
                1L,
                "SCN-01",
                200L,
                "Dự án Alpha",
                "applied",
                5,
                2,
                LocalDateTime.now(),
                "Thành công"
        );

        when(applyScenarioUseCase.applyScenario(any(ApplyScenarioCommand.class))).thenReturn(mockResult);

        ApplyScenarioRequest request = new ApplyScenarioRequest(200L, "Áp dụng sau ký HĐ");

        mockMvc.perform(post("/api/v1/resource-scenarios/1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("applied"))
                .andExpect(jsonPath("$.data.appliedAllocationsCount").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/apply - Dữ liệu gốc đã đổi -> 409 Conflict")
    void applyScenario_BaselineStale_ReturnsConflict() throws Exception {
        when(applyScenarioUseCase.applyScenario(any(ApplyScenarioCommand.class)))
                .thenThrow(new ScenarioBaselineStaleException("Dữ liệu phân bổ thật đã thay đổi sau khi kịch bản được tạo"));

        ApplyScenarioRequest request = new ApplyScenarioRequest(200L, "Ghi chú");

        mockMvc.perform(post("/api/v1/resource-scenarios/1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCENARIO_BASELINE_STALE"))
                .andExpect(jsonPath("$.message").value("Dữ liệu phân bổ thật đã thay đổi sau khi kịch bản được tạo"));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/refresh-baseline - Làm mới snapshot thành công -> 200 OK")
    void refreshBaseline_Success() throws Exception {
        ScenarioResult mockResult = new ScenarioResult(
                1L,
                "SCN-01",
                "Kịch bản test",
                "Mô tả",
                10L,
                "Phòng Kỹ Thuật",
                "draft",
                2026,
                38,
                8,
                LocalDateTime.now(),
                100L,
                "rm",
                LocalDateTime.now(),
                LocalDateTime.now(),
                2,
                10,
                null,
                null,
                null
        );

        when(refreshBaselineUseCase.refreshScenarioBaseline(1L)).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/resource-scenarios/1/refresh-baseline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("SCN-01"));
    }
}
