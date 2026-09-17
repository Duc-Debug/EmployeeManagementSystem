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
import com.hrm.employeemanagement.application.dto.scenario.CompareScenariosCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonItemResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.CompareSimulationScenariosUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.InsufficientScenariosForComparisonException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.CompareScenariosRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScenarioComparisonController Web API Tests (NCL-08-CN-004)")
class ScenarioComparisonControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CompareSimulationScenariosUseCase compareScenariosUseCase;

    @BeforeEach
    void setUp() {
        ScenarioComparisonController controller = new ScenarioComparisonController(compareScenariosUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private ScenarioComparisonResult createMockComparisonResult() {
        ScenarioComparisonItemResult item1 = new ScenarioComparisonItemResult(
                1L, "SCN-01", "Kịch bản A", "Mô tả A",
                10L, "Phòng Công nghệ", "DRAFT",
                2026, 38, 8,
                2, new BigDecimal("20.00"), new BigDecimal("20.00"), new BigDecimal("80.00"),
                new BigDecimal("280.00"), new BigDecimal("300.00"),
                new BigDecimal("93.3"), new BigDecimal("110.0"),
                List.of(), List.of()
        );
        ScenarioComparisonItemResult item2 = new ScenarioComparisonItemResult(
                2L, "SCN-02", "Kịch bản B", "Mô tả B",
                10L, "Phòng Công nghệ", "DRAFT",
                2026, 38, 8,
                0, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("40.00"),
                new BigDecimal("220.00"), new BigDecimal("300.00"),
                new BigDecimal("73.3"), new BigDecimal("85.0"),
                List.of(), List.of()
        );
        return new ScenarioComparisonResult(List.of(item1, item2), true, true, LocalDateTime.now());
    }

    @Test
    @DisplayName("TC-01: So sánh kịch bản thành công trả về HTTP 200 OK với bảng các cột kịch bản")
    void shouldReturn200AndComparisonData_WhenRequestIsValid_TC01() throws Exception {
        when(compareScenariosUseCase.compareScenarios(any())).thenReturn(createMockComparisonResult());

        CompareScenariosRequest request = new CompareScenariosRequest(List.of(1L, 2L));

        mockMvc.perform(post("/api/v1/resource-scenarios/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("So sánh kịch bản mô phỏng thành công"))
                .andExpect(jsonPath("$.data.scenarios.length()").value(2))
                .andExpect(jsonPath("$.data.scenarios[0].scenarioId").value(1))
                .andExpect(jsonPath("$.data.scenarios[0].scenarioCode").value("SCN-01"))
                .andExpect(jsonPath("$.data.scenarios[0].overloadedEmployeesCount").value(2))
                .andExpect(jsonPath("$.data.scenarios[0].totalShortfallHours").value(20.00))
                .andExpect(jsonPath("$.data.scenarios[1].scenarioId").value(2))
                .andExpect(jsonPath("$.data.scenarios[1].overloadedEmployeesCount").value(0));
    }

    @Test
    @DisplayName("TC-02: Báo lỗi 400 Bad Request khi truyền ít hơn 2 ID kịch bản (Bean Validation)")
    void shouldReturn400BadRequest_WhenLessThanTwoIds_BeanValidation_TC02() throws Exception {
        CompareScenariosRequest request = new CompareScenariosRequest(List.of(1L));

        mockMvc.perform(post("/api/v1/resource-scenarios/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("TC-02: Báo lỗi 400 Bad Request khi truyền nhiều hơn 10 ID kịch bản (Bean Validation)")
    void shouldReturn400BadRequest_WhenMoreThanTenIds_BeanValidation_TC02() throws Exception {
        List<Long> elevenIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
        CompareScenariosRequest request = new CompareScenariosRequest(elevenIds);

        mockMvc.perform(post("/api/v1/resource-scenarios/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("TC-02: Báo lỗi 400 Bad Request khi usecase ném InsufficientScenariosForComparisonException")
    void shouldReturn400BadRequest_WhenUseCaseThrowsInsufficientScenarios_TC02() throws Exception {
        when(compareScenariosUseCase.compareScenarios(any()))
                .thenThrow(new InsufficientScenariosForComparisonException("Cần ít nhất hai kịch bản để so sánh"));

        CompareScenariosRequest request = new CompareScenariosRequest(List.of(1L, 1L));

        mockMvc.perform(post("/api/v1/resource-scenarios/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_SCENARIOS_FOR_COMPARISON"))
                .andExpect(jsonPath("$.message").value("Cần ít nhất hai kịch bản để so sánh"));
    }

    @Test
    @DisplayName("TC-03: Trả về HTTP 403 Forbidden khi người dùng không có quyền hoặc không phải Ban giám đốc")
    void shouldReturn403Forbidden_WhenPermissionDenied_TC03() throws Exception {
        when(compareScenariosUseCase.compareScenarios(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_COMPARE));

        CompareScenariosRequest request = new CompareScenariosRequest(List.of(1L, 2L));

        mockMvc.perform(post("/api/v1/resource-scenarios/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Trả về HTTP 404 Not Found khi kịch bản không tồn tại")
    void shouldReturn404NotFound_WhenScenarioNotFound() throws Exception {
        when(compareScenariosUseCase.compareScenarios(any()))
                .thenThrow(new ScenarioNotFoundException(999L));

        CompareScenariosRequest request = new CompareScenariosRequest(List.of(1L, 999L));

        mockMvc.perform(post("/api/v1/resource-scenarios/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCENARIO_NOT_FOUND"));
    }
}