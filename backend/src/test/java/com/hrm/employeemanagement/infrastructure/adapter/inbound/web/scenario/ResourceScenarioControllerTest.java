package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioDemandResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioShareResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioSimulationResult;
import com.hrm.employeemanagement.application.dto.scenario.ShareCandidateResult;
import com.hrm.employeemanagement.application.dto.scenario.WeeklySimulationMetricResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.AddScenarioDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.CreateSimulationScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.DeleteScenarioDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetScenarioSharesUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetScenarioSimulationResultUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetShareCandidatesUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetSimulationScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.ListSimulationScenariosUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.PatchSimulationScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.SaveSimulationScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.ShareSimulationScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.UnshareSimulationScenarioUseCase;
import com.hrm.employeemanagement.application.port.inbound.scenario.UpdateScenarioDemandUseCase;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioCodeException;
import com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioShareException;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidShareRecipientException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotSavedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.AddScenarioDemandRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto.CreateScenarioRequest;

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

    @Mock
    private SaveSimulationScenarioUseCase saveScenarioUseCase;

    @Mock
    private PatchSimulationScenarioUseCase patchScenarioUseCase;

    @Mock
    private ShareSimulationScenarioUseCase shareScenarioUseCase;

    @Mock
    private UnshareSimulationScenarioUseCase unshareScenarioUseCase;

    @Mock
    private GetShareCandidatesUseCase getShareCandidatesUseCase;

    @Mock
    private GetScenarioSharesUseCase getScenarioSharesUseCase;

    @BeforeEach
    void setUp() {
        ResourceScenarioController controller = new ResourceScenarioController(
                createScenarioUseCase,
                getScenarioUseCase,
                listScenariosUseCase,
                addDemandUseCase,
                updateDemandUseCase,
                deleteDemandUseCase,
                simulationResultUseCase,
                saveScenarioUseCase,
                patchScenarioUseCase,
                shareScenarioUseCase,
                unshareScenarioUseCase,
                getShareCandidatesUseCase,
                getScenarioSharesUseCase
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
    @DisplayName("POST /api/v1/resource-scenarios: Mã kịch bản bị trùng -> 409 Conflict")
    void testCreateScenario_DuplicateCode_Returns409Conflict() throws Exception {
        CreateScenarioRequest request = new CreateScenarioRequest(
                "SCN-2026-001", "Kịch bản trùng", "Mô tả", 10L, 2026, 38, 8
        );

        when(createScenarioUseCase.createScenario(any()))
                .thenThrow(new DuplicateScenarioCodeException("SCN-2026-001"));

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_SCENARIO_CODE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SCN-2026-001")));
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
                "Java Senior", 2, 2026, 1, 2026, 6, BigDecimal.valueOf(40), "Spring Boot"
        );

        ScenarioDemandResult result = new ScenarioDemandResult(
                10L, 1L, "Java Senior", 2, 2026, 1, 2026, 6, BigDecimal.valueOf(40),
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
                "Java Senior", 0, 2026, 1, 2026, 6, BigDecimal.valueOf(40), "Spring Boot"
        );

        mockMvc.perform(post("/api/v1/resource-scenarios/1/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/demands: hoursPerWeekPerPerson = 0 -> 400 BAD_REQUEST")
    void testAddDemand_ZeroHours_Returns400() throws Exception {
        AddScenarioDemandRequest request = new AddScenarioDemandRequest(
                "Java Senior", 2, 2026, 1, 2026, 6, BigDecimal.ZERO, "Spring Boot"
        );

        mockMvc.perform(post("/api/v1/resource-scenarios/1/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Số giờ/tuần/người phải lớn hơn 0")));
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
                "draft", LocalDateTime.now(), List.of(w1), List.of(), List.of(),
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

    @Test
    @DisplayName("POST /api/v1/resource-scenarios: fromYear và fromWeek được chuyển giao chính xác cho UseCase")
    void testCreateScenario_WithFromYearAndFromWeek_ForwardedCorrectly() throws Exception {
        CreateScenarioRequest request = new CreateScenarioRequest(
                "SCN-2027-X", "Kịch bản năm 2027", "Mô tả", 10L, 2027, 12, 10
        );

        ScenarioResult result = new ScenarioResult(
                2L, "SCN-2027-X", "Kịch bản năm 2027", "Mô tả", 10L, "Phòng Kỹ Thuật",
                "draft", 2027, 12, 10, LocalDateTime.now(), 103L, "rm_user",
                LocalDateTime.now(), null, 0, 5
        );

        when(createScenarioUseCase.createScenario(argThat(cmd ->
                Integer.valueOf(2027).equals(cmd.fromYear()) &&
                Integer.valueOf(12).equals(cmd.fromWeek()) &&
                Integer.valueOf(10).equals(cmd.durationWeeks()) &&
                Long.valueOf(10L).equals(cmd.orgUnitId())
        ))).thenReturn(result);

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromYear").value(2027))
                .andExpect(jsonPath("$.data.fromWeek").value(12))
                .andExpect(jsonPath("$.data.durationWeeks").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios: durationWeeks > 16 -> 400 Bad Request (Validation Error)")
    void testCreateScenario_DurationWeeksTooLarge_Returns400() throws Exception {
        String jsonPayload = """
                {
                    "code": "SCN-01",
                    "name": "Kịch bản vượt ngưỡng tuần",
                    "fromYear": 2026,
                    "fromWeek": 38,
                    "durationWeeks": 100
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Số tuần mô phỏng tối đa là 16 tuần")));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios: durationWeeks < 1 -> 400 Bad Request")
    void testCreateScenario_DurationWeeksZero_Returns400() throws Exception {
        String jsonPayload = """
                {
                    "code": "SCN-01",
                    "name": "Kịch bản 0 tuần",
                    "fromYear": 2026,
                    "fromWeek": 38,
                    "durationWeeks": 0
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Số tuần mô phỏng tối thiểu là 1 tuần")));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios: durationWeeks null -> 400 Bad Request")
    void testCreateScenario_DurationWeeksNull_Returns400() throws Exception {
        String jsonPayload = """
                {
                    "code": "SCN-01",
                    "name": "Kịch bản thiếu durationWeeks",
                    "fromYear": 2026,
                    "fromWeek": 38,
                    "durationWeeks": null
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Số tuần mô phỏng không được để trống")));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios: Chỉ truyền fromYear mà thiếu fromWeek -> 400 Bad Request")
    void testCreateScenario_OnlyFromYearProvided_Returns400() throws Exception {
        String jsonPayload = """
                {
                    "code": "SCN-01",
                    "name": "Kịch bản chỉ có fromYear",
                    "fromYear": 2026,
                    "durationWeeks": 8
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("fromYear và fromWeek phải cùng được cung cấp hoặc cùng để trống")));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios: Chỉ truyền fromWeek mà thiếu fromYear -> 400 Bad Request")
    void testCreateScenario_OnlyFromWeekProvided_Returns400() throws Exception {
        String jsonPayload = """
                {
                    "code": "SCN-01",
                    "name": "Kịch bản chỉ có fromWeek",
                    "fromWeek": 38,
                    "durationWeeks": 8
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("fromYear và fromWeek phải cùng được cung cấp hoặc cùng để trống")));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/save: Lưu snapshot kịch bản thành công -> 200 OK")
    void testSaveScenario_Success_Returns200() throws Exception {
        ScenarioResult savedResult = new ScenarioResult(
                1L, "SCN-001", "Kịch bản đã lưu", "Mô tả", "Ghi chú lưu kịch bản",
                10L, "Phòng IT", "saved", 2026, 38, 8, LocalDateTime.now(),
                100L, "User VT-03", LocalDateTime.now(), LocalDateTime.now(),
                2, 5, "EDIT"
        );
        when(saveScenarioUseCase.saveScenario(1L)).thenReturn(savedResult);

        mockMvc.perform(post("/api/v1/resource-scenarios/1/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("saved"))
                .andExpect(jsonPath("$.data.note").value("Ghi chú lưu kịch bản"));
    }

    @Test
    @DisplayName("PATCH /api/v1/resource-scenarios/{id}: Cập nhật tên và ghi chú thành công -> 200 OK")
    void testPatchScenario_Success_Returns200() throws Exception {
        ScenarioResult patchedResult = new ScenarioResult(
                1L, "SCN-001", "Tên mới", "Mô tả", "Ghi chú mới",
                10L, "Phòng IT", "draft", 2026, 38, 8, LocalDateTime.now(),
                100L, "User VT-03", LocalDateTime.now(), LocalDateTime.now(),
                2, 5, "EDIT"
        );
        when(patchScenarioUseCase.patchScenario(any())).thenReturn(patchedResult);

        String json = """
                {
                    "name": "Tên mới",
                    "note": "Ghi chú mới"
                }
                """;

        mockMvc.perform(patch("/api/v1/resource-scenarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Tên mới"))
                .andExpect(jsonPath("$.data.note").value("Ghi chú mới"));
    }

    @Test
    @DisplayName("GET /api/v1/resource-scenarios/{id}/share-candidates: Lấy danh sách candidate hợp lệ -> 200 OK")
    void testGetShareCandidates_Success_Returns200() throws Exception {
        ShareCandidateResult candidate = new ShareCandidateResult(
                200L, "vt01_user", "Giám Đốc", "EMP-001", "vt01@company.com",
                "VT-01", "Lãnh đạo", 10L, "Phòng IT", List.of()
        );
        when(getShareCandidatesUseCase.getShareCandidates(1L, "vt01")).thenReturn(List.of(candidate));

        mockMvc.perform(get("/api/v1/resource-scenarios/1/share-candidates?query=vt01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(200))
                .andExpect(jsonPath("$.data[0].username").value("vt01_user"));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/shares: Chia sẻ kịch bản thành công -> 200 OK")
    void testShareScenario_Success_Returns200() throws Exception {
        ScenarioShareResult shareResult = new ScenarioShareResult(
                10L, 1L, 200L, "vt01_user", "Giám Đốc", "VT-01", "Lãnh đạo",
                100L, "VIEW_ONLY", LocalDateTime.now(), null, true
        );
        when(shareScenarioUseCase.shareScenario(any())).thenReturn(List.of(shareResult));

        String json = """
                {
                    "userIds": [200]
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios/1/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sharedWithUserId").value(200));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/shares: Kịch bản chưa lưu (DRAFT) -> 400 Bad Request")
    void testShareScenario_NotSaved_Returns400() throws Exception {
        when(shareScenarioUseCase.shareScenario(any()))
                .thenThrow(new ScenarioNotSavedException("Chỉ có thể chia sẻ kịch bản đã lưu"));

        String json = """
                {
                    "userIds": [200]
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios/1/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SCENARIO_NOT_SAVED"));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/shares: Recipient không hợp lệ -> 400 Bad Request")
    void testShareScenario_InvalidRecipient_Returns400() throws Exception {
        when(shareScenarioUseCase.shareScenario(any()))
                .thenThrow(new InvalidShareRecipientException("Người nhận không hợp lệ"));

        String json = """
                {
                    "userIds": [300]
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios/1/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SHARE_RECIPIENT"));
    }

    @Test
    @DisplayName("POST /api/v1/resource-scenarios/{id}/shares: Trùng lặp chia sẻ -> 409 Conflict")
    void testShareScenario_DuplicateShare_Returns409() throws Exception {
        when(shareScenarioUseCase.shareScenario(any()))
                .thenThrow(new DuplicateScenarioShareException("Kịch bản đã được chia sẻ cho người dùng này"));

        String json = """
                {
                    "userIds": [200]
                }
                """;

        mockMvc.perform(post("/api/v1/resource-scenarios/1/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SCENARIO_SHARE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/resource-scenarios/{id}/shares/{userId}: Thu hồi chia sẻ thành công -> 200 OK")
    void testUnshareScenario_Success_Returns200() throws Exception {
        doNothing().when(unshareScenarioUseCase).unshareScenario(1L, 200L);

        mockMvc.perform(delete("/api/v1/resource-scenarios/1/shares/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/resource-scenarios/{id}/shares: Lấy danh sách chia sẻ đang active -> 200 OK")
    void testGetScenarioShares_Success_Returns200() throws Exception {
        ScenarioShareResult shareResult = new ScenarioShareResult(
                10L, 1L, 200L, "vt01_user", "Giám Đốc", "VT-01", "Lãnh đạo",
                100L, "VIEW_ONLY", LocalDateTime.now(), null, true
        );
        when(getScenarioSharesUseCase.getActiveShares(1L)).thenReturn(List.of(shareResult));

        mockMvc.perform(get("/api/v1/resource-scenarios/1/shares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }
}
