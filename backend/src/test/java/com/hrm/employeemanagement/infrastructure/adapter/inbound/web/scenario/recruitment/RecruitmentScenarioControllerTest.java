package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.recruitment;

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

import com.hrm.employeemanagement.application.dto.scenario.recruitment.AddSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RemoveSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.SimulatedEmployeeResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.AddSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.GetScenarioSimulatedEmployeesUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.RemoveSimulatedEmployeeUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.recruitment.RerunRecruitmentScenarioUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitmentScenarioController Web API Tests")
class RecruitmentScenarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AddSimulatedEmployeeUseCase addUseCase;
    @Mock
    private RemoveSimulatedEmployeeUseCase removeUseCase;
    @Mock
    private GetScenarioSimulatedEmployeesUseCase getUseCase;
    @Mock
    private RerunRecruitmentScenarioUseCase rerunUseCase;

    @BeforeEach
    void setUp() {
        RecruitmentScenarioController controller = new RecruitmentScenarioController(
                addUseCase, removeUseCase, getUseCase, rerunUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/scenarios/{scenarioId}/simulated-employees - Thêm nhân sự giả định thành công trả về 201 Created")
    void testAddSimulatedEmployee_Success() throws Exception {
        RecruitmentScenarioEvaluationResult evaluation = new RecruitmentScenarioEvaluationResult(
                1L,
                new BigDecimal("160.00"),
                new BigDecimal("160.00"),
                BigDecimal.ZERO,
                false,
                0,
                1,
                0,
                List.of(new RecruitmentScenarioEvaluationResult.RoleEvaluationItemResult(
                        10L, "DEV", "Developer",
                        new BigDecimal("160.00"), new BigDecimal("160.00"), BigDecimal.ZERO, 1, 0
                ))
        );

        when(addUseCase.addSimulatedEmployee(any(AddSimulatedEmployeeCommand.class))).thenReturn(evaluation);

        String json = """
                {
                    "candidateName": "Nguyễn Văn Giả Định",
                    "projectRoleId": 10,
                    "primarySkillId": 1,
                    "standardHoursPerWeek": 40.0,
                    "weeksCount": 4,
                    "notes": "Ứng viên thử nghiệm"
                }
                """;

        mockMvc.perform(post("/api/v1/scenarios/1/simulated-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scenarioId").value(1))
                .andExpect(jsonPath("$.data.totalRemainingShortfallHours").value(0))
                .andExpect(jsonPath("$.data.isPlanBroken").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/scenarios/{scenarioId}/simulated-employees - Không có quyền VT-03 trả về 403 Forbidden")
    void testAddSimulatedEmployee_Forbidden() throws Exception {
        when(addUseCase.addSimulatedEmployee(any())).thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE));

        String json = """
                {
                    "candidateName": "Dev",
                    "projectRoleId": 10,
                    "standardHoursPerWeek": 40.0,
                    "weeksCount": 4
                }
                """;

        mockMvc.perform(post("/api/v1/scenarios/1/simulated-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/scenarios/{scenarioId}/simulated-employees - Lấy danh sách thành công")
    void testGetSimulatedEmployees_Success() throws Exception {
        SimulatedEmployeeResult item = new SimulatedEmployeeResult(
                1L, 100L, "Dev 1", 10L, "DEV", "Developer",
                null, null, new BigDecimal("40.00"), 4, new BigDecimal("160.00"),
                "Ghi chú", 2L, LocalDateTime.now()
        );
        when(getUseCase.getSimulatedEmployees(100L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/scenarios/100/simulated-employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].candidateName").value("Dev 1"))
                .andExpect(jsonPath("$.data[0].projectRoleCode").value("DEV"));
    }

    @Test
    @DisplayName("DELETE /api/v1/scenarios/{scenarioId}/simulated-employees/{employeeId} - Xóa thành công")
    void testRemoveSimulatedEmployee_Success() throws Exception {
        RecruitmentScenarioEvaluationResult evaluation = new RecruitmentScenarioEvaluationResult(
                100L, new BigDecimal("160.00"), BigDecimal.ZERO, new BigDecimal("160.00"),
                true, 1, 0, 1, List.of()
        );
        when(removeUseCase.removeSimulatedEmployee(any(RemoveSimulatedEmployeeCommand.class))).thenReturn(evaluation);

        mockMvc.perform(delete("/api/v1/scenarios/100/simulated-employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPlanBroken").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/scenarios/{scenarioId}/recruitment-evaluation - Chạy lại kịch bản thành công")
    void testRerunEvaluation_Success() throws Exception {
        RecruitmentScenarioEvaluationResult evaluation = new RecruitmentScenarioEvaluationResult(
                100L, new BigDecimal("160.00"), new BigDecimal("160.00"), BigDecimal.ZERO,
                false, 0, 1, 0, List.of()
        );
        when(rerunUseCase.rerunRecruitmentScenario(100L)).thenReturn(evaluation);

        mockMvc.perform(get("/api/v1/scenarios/100/recruitment-evaluation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPlanBroken").value(false));
    }
}