package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.hrm.employeemanagement.application.dto.milestone.CreateMilestoneCommand;
import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.dto.milestone.UpdateMilestoneCommand;
import com.hrm.employeemanagement.application.port.inbound.milestone.CreateMilestoneUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.DeleteMilestoneUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.GetProjectMilestonesUseCase;
import com.hrm.employeemanagement.application.port.inbound.milestone.UpdateMilestoneUseCase;
import com.hrm.employeemanagement.domain.exception.milestone.DuplicateMilestoneNameException;
import com.hrm.employeemanagement.domain.exception.milestone.MilestoneNotFoundException;
import com.hrm.employeemanagement.domain.exception.milestone.ProjectHasNoWbsException;
import com.hrm.employeemanagement.domain.milestone.MilestoneStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class MilestoneControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateMilestoneUseCase createMilestoneUseCase;
    @Mock
    private UpdateMilestoneUseCase updateMilestoneUseCase;
    @Mock
    private GetProjectMilestonesUseCase getProjectMilestonesUseCase;
    @Mock
    private DeleteMilestoneUseCase deleteMilestoneUseCase;

    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        MilestoneController controller = new MilestoneController(
                createMilestoneUseCase,
                updateMilestoneUseCase,
                getProjectMilestonesUseCase,
                deleteMilestoneUseCase);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new MilestoneExceptionHandler(),
                        new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/milestones thành công trả về 201 CREATED")
    void shouldCreateMilestoneSuccessfully() throws Exception {
        MilestoneResult mockResult = new MilestoneResult(
                10L,
                PROJECT_ID,
                "Bàn giao giai đoạn 1",
                "Mô tả giai đoạn",
                LocalDate.of(2026, 9, 30),
                null,
                MilestoneStatus.ON_TRACK,
                0,
                2,
                0,
                List.of(101L, 102L),
                1L,
                LocalDateTime.now(),
                null,
                0L);

        when(createMilestoneUseCase.createMilestone(any(CreateMilestoneCommand.class))).thenReturn(mockResult);

        String requestJson = """
                {
                    "name": "Bàn giao giai đoạn 1",
                    "description": "Mô tả giai đoạn",
                    "plannedDate": "2026-09-30",
                    "linkedTaskIds": [101, 102]
                }
                """;

        mockMvc.perform(post("/api/v1/projects/{projectId}/milestones", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Bàn giao giai đoạn 1"))
                .andExpect(jsonPath("$.data.status").value("ON_TRACK"))
                .andExpect(jsonPath("$.data.delayDays").value(0))
                .andExpect(jsonPath("$.data.totalLinkedTasks").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/milestones trả về 400 khi tên rỗng hoặc ngày kế hoạch null")
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        String invalidJson = """
                {
                    "name": "   ",
                    "plannedDate": null
                }
                """;

        mockMvc.perform(post("/api/v1/projects/{projectId}/milestones", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/milestones trả về 400 khi dự án chưa có WBS")
    void shouldReturnBadRequestWhenProjectHasNoWbs() throws Exception {
        when(createMilestoneUseCase.createMilestone(any(CreateMilestoneCommand.class)))
                .thenThrow(new ProjectHasNoWbsException(PROJECT_ID));

        String requestJson = """
                {
                    "name": "Mốc mới",
                    "plannedDate": "2026-09-30"
                }
                """;

        mockMvc.perform(post("/api/v1/projects/{projectId}/milestones", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("chưa có cây công việc")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/milestones trả về 409 khi tên mốc bị trùng")
    void shouldReturnConflictWhenMilestoneNameDuplicate() throws Exception {
        when(createMilestoneUseCase.createMilestone(any(CreateMilestoneCommand.class)))
                .thenThrow(new DuplicateMilestoneNameException("Mốc trùng"));

        String requestJson = """
                {
                    "name": "Mốc trùng",
                    "plannedDate": "2026-09-30"
                }
                """;

        mockMvc.perform(post("/api/v1/projects/{projectId}/milestones", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Tên mốc tiến độ đã tồn tại")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/milestones trả về danh sách mốc tiến độ và số ngày trễ (TC-02)")
    void shouldGetProjectMilestonesSuccessfully() throws Exception {
        MilestoneResult m1 = new MilestoneResult(
                1L,
                PROJECT_ID,
                "Mốc trễ hạn",
                null,
                LocalDate.of(2026, 9, 1),
                null,
                MilestoneStatus.DELAYED,
                7, // 7 ngày trễ
                2,
                1,
                List.of(101L, 102L),
                1L,
                LocalDateTime.now(),
                null,
                0L);

        when(getProjectMilestonesUseCase.getProjectMilestones(PROJECT_ID)).thenReturn(List.of(m1));

        mockMvc.perform(get("/api/v1/projects/{projectId}/milestones", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("DELAYED"))
                .andExpect(jsonPath("$.data[0].delayDays").value(7));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId}/milestones/{milestoneId} cập nhật mốc thành công")
    void shouldUpdateMilestoneSuccessfully() throws Exception {
        MilestoneResult updatedResult = new MilestoneResult(
                10L,
                PROJECT_ID,
                "Tên mới",
                "Mô tả mới",
                LocalDate.of(2026, 10, 15),
                null,
                MilestoneStatus.ON_TRACK,
                0,
                0,
                0,
                List.of(),
                1L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L);

        when(updateMilestoneUseCase.updateMilestone(any(UpdateMilestoneCommand.class))).thenReturn(updatedResult);

        String requestJson = """
                {
                    "name": "Tên mới",
                    "description": "Mô tả mới",
                    "plannedDate": "2026-10-15"
                }
                """;

        mockMvc.perform(put("/api/v1/projects/{projectId}/milestones/{milestoneId}", PROJECT_ID, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tên mới"))
                .andExpect(jsonPath("$.data.description").value("Mô tả mới"));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/milestones/{milestoneId} xóa mốc thành công")
    void shouldDeleteMilestoneSuccessfully() throws Exception {
        doNothing().when(deleteMilestoneUseCase).deleteMilestone(PROJECT_ID, 10L);

        mockMvc.perform(delete("/api/v1/projects/{projectId}/milestones/{milestoneId}", PROJECT_ID, 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/milestones trả về 400 khi linkedTaskIds chứa phần tử null hoặc <= 0")
    void shouldReturnBadRequestWhenCreateLinkedTaskIdContainsNullOrNonPositive() throws Exception {
        String invalidJson = """
                {
                    "name": "Mốc đợt 1",
                    "plannedDate": "2026-09-30",
                    "linkedTaskIds": [1, null, -5]
                }
                """;

        mockMvc.perform(post("/api/v1/projects/{projectId}/milestones", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId}/milestones/{milestoneId} trả về 400 khi linkedTaskIds chứa phần tử null hoặc <= 0")
    void shouldReturnBadRequestWhenUpdateLinkedTaskIdContainsNullOrNonPositive() throws Exception {
        String invalidJson = """
                {
                    "name": "Mốc đợt 1 cập nhật",
                    "linkedTaskIds": [null]
                }
                """;

        mockMvc.perform(put("/api/v1/projects/{projectId}/milestones/{milestoneId}", PROJECT_ID, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
