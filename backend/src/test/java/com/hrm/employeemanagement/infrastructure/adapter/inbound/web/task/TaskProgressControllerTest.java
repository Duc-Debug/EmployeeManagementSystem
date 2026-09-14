package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.task.TaskProgressResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskProgressCommand;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskProgressUseCase;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotAssignedToUserException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.task.TaskStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskProgressController Web Tests (NCL-04-CN-002)")
class TaskProgressControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UpdateTaskProgressUseCase updateTaskProgressUseCase;

    @BeforeEach
    void setUp() {
        TaskProgressController controller = new TaskProgressController(updateTaskProgressUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TaskExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("NCL-04-CN-002-TC-01: PATCH /api/v1/tasks/{taskId}/progress - Cập nhật tiến độ sang DONE thành công (200 OK)")
    void shouldReturn200WhenUpdateProgressSucceeds() throws Exception {
        TaskProgressResult result = new TaskProgressResult(
                10L,
                100L,
                "TSK-10",
                "Phát triển API",
                TaskStatus.IN_PROGRESS,
                TaskStatus.DONE,
                LocalDateTime.of(2026, 9, 14, 15, 0)
        );

        when(updateTaskProgressUseCase.updateProgress(any(UpdateTaskProgressCommand.class))).thenReturn(result);

        mockMvc.perform(patch("/api/v1/tasks/10/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "status": "DONE"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật tiến độ công việc thành công"))
                .andExpect(jsonPath("$.data.taskId").value(10))
                .andExpect(jsonPath("$.data.previousStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentStatus").value("DONE"));

        verify(updateTaskProgressUseCase).updateProgress(any(UpdateTaskProgressCommand.class));
    }

    @Test
    @DisplayName("NCL-04-CN-002-TC-02: PATCH /api/v1/tasks/{taskId}/progress - Không có quyền trả về 403 FORBIDDEN")
    void shouldReturn403WhenUserNotAssigned() throws Exception {
        when(updateTaskProgressUseCase.updateProgress(any(UpdateTaskProgressCommand.class)))
                .thenThrow(new TaskNotAssignedToUserException(10L, 99L));

        mockMvc.perform(patch("/api/v1/tasks/10/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "status": "DONE"
                        }
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền chuyển trạng thái công việc của người khác"));
    }

    @Test
    @DisplayName("Ngoại lệ: Payload thiếu status trả về 400 BAD REQUEST")
    void shouldReturn400WhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks/10/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ngoại lệ: Task không tồn tại trả về 404 NOT FOUND")
    void shouldReturn404WhenTaskNotFound() throws Exception {
        when(updateTaskProgressUseCase.updateProgress(any(UpdateTaskProgressCommand.class)))
                .thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(patch("/api/v1/tasks/999/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "status": "DONE"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Ngoại lệ: Dự án đã đóng trả về 400 BAD REQUEST")
    void shouldReturn400WhenProjectClosed() throws Exception {
        when(updateTaskProgressUseCase.updateProgress(any(UpdateTaskProgressCommand.class)))
                .thenThrow(new ProjectClosedException(100L));

        mockMvc.perform(patch("/api/v1/tasks/10/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "status": "DONE"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Ngoại lệ: Trạng thái không hợp lệ trả về 400 BAD REQUEST")
    void shouldReturn400WhenInvalidStatus() throws Exception {
        when(updateTaskProgressUseCase.updateProgress(any(UpdateTaskProgressCommand.class)))
                .thenThrow(new InvalidTaskDataException("Nhân viên chuyên môn không được phép hủy công việc (CANCELLED)"));

        mockMvc.perform(patch("/api/v1/tasks/10/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "status": "CANCELLED"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nhân viên chuyên môn không được phép hủy công việc (CANCELLED)"));
    }
}
