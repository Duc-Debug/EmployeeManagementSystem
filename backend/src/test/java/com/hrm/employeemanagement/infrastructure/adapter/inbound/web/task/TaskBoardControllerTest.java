package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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

import com.hrm.employeemanagement.application.dto.task.MoveTaskBoardStatusCommand;
import com.hrm.employeemanagement.application.dto.task.TaskBoardAssigneeResult;
import com.hrm.employeemanagement.application.dto.task.TaskBoardCardResult;
import com.hrm.employeemanagement.application.dto.task.TaskBoardQuery;
import com.hrm.employeemanagement.application.dto.task.TaskBoardResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskBoardUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.MoveTaskBoardStatusUseCase;
import com.hrm.employeemanagement.domain.exception.task.TaskNotAssignedToUserException;
import com.hrm.employeemanagement.domain.task.TaskStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskBoardController Web Tests (NCL-04-CN-006)")
class TaskBoardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetTaskBoardUseCase getTaskBoardUseCase;

    @Mock
    private MoveTaskBoardStatusUseCase moveTaskBoardStatusUseCase;

    @BeforeEach
    void setUp() {
        TaskBoardController controller = new TaskBoardController(getTaskBoardUseCase, moveTaskBoardStatusUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TaskExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/tasks/board - Lấy dữ liệu bảng công việc thành công (200 OK)")
    void testGetTaskBoard_Success() throws Exception {
        TaskBoardCardResult card = new TaskBoardCardResult(
                10L,
                "TSK-10",
                "Phân tích yêu cầu",
                "Mô tả",
                100L,
                "PRJ-01",
                "Dự án Alpha",
                TaskStatus.IN_PROGRESS,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 25),
                1,
                List.of(new TaskBoardAssigneeResult(50L, "EMP05", "Nguyen Van A", true)),
                true
        );

        TaskBoardResult board = new TaskBoardResult(
                Collections.emptyList(),
                List.of(card),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                1,
                100L,
                null
        );

        when(getTaskBoardUseCase.getTaskBoard(any(TaskBoardQuery.class))).thenReturn(board);

        mockMvc.perform(get("/api/v1/tasks/board")
                        .param("projectId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTasks").value(1))
                .andExpect(jsonPath("$.data.inProgressTasks[0].taskId").value(10))
                .andExpect(jsonPath("$.data.inProgressTasks[0].taskCode").value("TSK-10"))
                .andExpect(jsonPath("$.data.inProgressTasks[0].canMove").value(true))
                .andExpect(jsonPath("$.data.inProgressTasks[0].assignees[0].fullName").value("Nguyen Van A"));
    }

    @Test
    @DisplayName("NCL-04-CN-006-TC-01: PATCH /api/v1/tasks/{taskId}/board-status - Chuyển sang IN_REVIEW thành công (200 OK)")
    void testMoveTaskStatus_Success_TC01() throws Exception {
        TaskBoardCardResult updatedCard = new TaskBoardCardResult(
                10L,
                "TSK-10",
                "Phân tích yêu cầu",
                "Mô tả",
                100L,
                "PRJ-01",
                "Dự án Alpha",
                TaskStatus.IN_REVIEW,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 25),
                1,
                Collections.emptyList(),
                true
        );

        when(moveTaskBoardStatusUseCase.moveTaskStatus(any(MoveTaskBoardStatusCommand.class))).thenReturn(updatedCard);

        mockMvc.perform(patch("/api/v1/tasks/10/board-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"IN_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(10))
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"));
    }

    @Test
    @DisplayName("NCL-04-CN-006-TC-02: PATCH /api/v1/tasks/{taskId}/board-status - Không có quyền trả về 403 FORBIDDEN")
    void testMoveTaskStatus_Forbidden_TC02() throws Exception {
        when(moveTaskBoardStatusUseCase.moveTaskStatus(any(MoveTaskBoardStatusCommand.class)))
                .thenThrow(new TaskNotAssignedToUserException(10L, 99L));

        mockMvc.perform(patch("/api/v1/tasks/10/board-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"IN_REVIEW\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền chuyển trạng thái công việc của người khác"));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{taskId}/board-status - Payload thiếu newStatus trả về 400 BAD REQUEST")
    void testMoveTaskStatus_MissingStatus_BadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks/10/board-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
