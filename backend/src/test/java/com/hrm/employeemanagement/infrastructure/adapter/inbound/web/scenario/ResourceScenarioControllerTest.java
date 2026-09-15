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
import com.hrm.employeemanagement.application.dto.scenario.*;
import com.hrm.employeemanagement.application.port.inbound.scenario.*;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.AddScenarioDemandRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.CreateScenarioRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.UpdateScenarioDemandRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceScenarioController Web API Tests (NCL-08-CN-001)")
class ResourceScenarioControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CreateSimulationScenarioUseCase createScenarioUseCase;

    @Mock
    private GetSimulationScenarioUseCase getScenarioUseCase;

    @Mock
    private ListSimulationScenariosUseCase listScenariosUseCase;

    @Mock
    private AddScenarioDemandUseCase addDemandUseCase;

    @Mock
    private UpdateScenarioDemandUseCase updateDemandUseCase;

    @Mock
    private DeleteScenarioDemandUseCase deleteDemandUseCase;

    @Mock
    private GetScenarioSimulationResultUseCase simulationResultUseCase;

    @BeforeEach
    void setUp() {
        ResourceScenarioController controller = new ResourceScenarioController(
                createScenarioUseCase,
                getScenarioUseCase,
                listScenariosUseCase,
                addDemandUseCase,
                updateDemandUseCase,
                deleteDemandUseCase,
                simulationResultUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios: Tạo kịch bản thành công -> 200 OK")
    void testCreateScenario_Success() throws Exception {
        CreateScenarioRequest request = new CreateScenarioRequest(
                "SCN-2026-001", "Kịch bản dự án mới", "Mô tả", 10L, 2026, 38, 8
        );

        ScenarioResult result = new ScenarioResult(
                1L, "SCN-2026-001", "Kịch bản dự án mới", "Mô tả", 10L, "Phòng Kỹ Thuật",
                "draft", 2026, 38, 8, LocalDateTime.now(), 103L, "rm_user",
                LocalDateTime.now(), null, 0, 5
        );

        when(createScenarioUseCase.createScenario(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.code").value("SCN-2026-001"))
                .andExpect(jsonPath("$.data.status").value("draft"));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios: Không đủ quyền / ngoài scope -> 403 Forbidden")
    void testCreateScenario_PermissionDenied() throws Exception {
        CreateScenarioRequest request = new CreateScenarioRequest(
                "SCN-2026-001", "Kịch bản dự án mới", "Mô tả", 10L, 2026, 38, 8
        );

        when(createScenarioUseCase.createScenario(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE));

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/v1/resource-scenarios: Danh sách kịch bản -> 200 OK")
    void testListScenarios_Success() throws Exception {
        ScenarioResult r1 = new ScenarioResult(
                1L, "SCN-01", "Kịch bản 1", "Mô tả", 10L, "Phòng Kỹ Thuật",
                "draft", 2026, 38, 8, LocalDateTime.now(), 103L, "rm_user",
                LocalDateTime.now(), null, 2, 5
        );

        when(listScenariosUseCase.listScenarios(null)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/resource-scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].code").value("SCN-01"));
    }

    @Test
    @DisplayName("GET /api/v1/resource-scenarios/{id}: Không tìm thấy -> 404 NOT_FOUND")
    void testGetScenarioById_NotFound() throws Exception {
        when(getScenarioUseCase.getScenarioById(99L))
                .thenThrow(new ScenarioNotFoundException(99L));

        mockMvc.perform(get("/api/v1/resource-scenarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCENARIO_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/demands: Thêm nhu cầu thành công -> 200 OK")
    void testAddDemand_Success() throws Exception {
        AddScenarioDemandRequest request = new AddScenarioDemandRequest(
                "Java Senior", 2, 1, 6, BigDecimal.valueOf(40), "Spring Boot"
        );

        ScenarioDemandResult result = new ScenarioDemandResult(
                10L, 1L, "Java Senior", 2, 1, 6, BigDecimal.valueOf(40),
                BigDecimal.valueOf(80), "Spring Boot", LocalDateTime.now(), null
        );

        when(addDemandUseCase.addDemand(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/resource-scenarios/1/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.totalHoursPerWeek").value(80));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/demands: Validation lỗi (headcount=0) -> 400 BAD_REQUEST")
    void testAddDemand_ValidationError() throws Exception {
        AddScenarioDemandRequest request = new AddScenarioDemandRequest(
                "Java Senior", 0, 1, 6, BigDecimal.valueOf(40), "Spring Boot"
        );

        mockMvc.perform(post("/api/v1/resource-scenarios/1/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/resource-scenarios/{id}/demands/{demandId}: Xóa nhu cầu thành công -> 200 OK")
    void testDeleteDemand_Success() throws Exception {
        doNothing().when(deleteDemandUseCase).deleteDemand(1L, 10L);

        mockMvc.perform(delete("/api/v1/resource-scenarios/1/demands/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/resource-scenarios/{id}/simulation: Lấy kết quả mô phỏng -> 200 OK")
    void testGetSimulationResult_Success() throws Exception {
        WeeklySimulationMetricResult w1 = new WeeklySimulationMetricResult(
                2026, 38, "T38", BigDecimal.valueOf(32), BigDecimal.valueOf(40),
                BigDecimal.valueOf(72), BigDecimal.valueOf(40), BigDecimal.ZERO,
                BigDecimal.valueOf(32), BigDecimal.valueOf(180), CapacityStatus.OVERLOADED, true
        );

        ScenarioSimulationResult result = new ScenarioSimulationResult(
                1L, "SCN-01", "Kịch bản 1", 10L, "Phòng Kỹ Thuật",
                "draft", LocalDateTime.now(), List.of(w1), List.of(),
                BigDecimal.valueOf(100), BigDecimal.valueOf(50)
        );

        when(simulationResultUseCase.getSimulationResult(1L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/resource-scenarios/1/simulation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scenarioId").value(1L))
                .andExpect(jsonPath("$.data.weeklyMetrics[0].scenarioWorkloadHours").value(72))
                .andExpect(jsonPath("$.data.weeklyMetrics[0].isOverloaded").value(true));
    }
}
