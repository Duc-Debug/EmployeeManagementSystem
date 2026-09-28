package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.task.MyTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyTasksUseCase;
import com.hrm.employeemanagement.domain.task.TaskStatus;

@ExtendWith(MockitoExtension.class)
class MyTaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetMyTasksUseCase getMyTasksUseCase;

    @BeforeEach
    void setUp() {
        MyTaskController controller = new MyTaskController(getMyTasksUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/tasks/me tra ve danh sach cong viec cua toi")
    void testGetMyTasks_Success() throws Exception {
        MyTaskResult task = new MyTaskResult(
                100L,
                200L,
                "PRJ-01",
                "Dự án CRM",
                "PRJ-01-T01",
                "Phát triển module auth",
                TaskStatus.IN_PROGRESS,
                BigDecimal.valueOf(40),
                BigDecimal.valueOf(10),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 30),
                true
        );

        when(getMyTasksUseCase.getMyTasks()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/v1/tasks/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskId").value(100))
                .andExpect(jsonPath("$.data[0].taskCode").value("PRJ-01-T01"))
                .andExpect(jsonPath("$.data[0].taskName").value("Phát triển module auth"))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[0].plannedStartDate").value("2026-09-15"))
                .andExpect(jsonPath("$.data[0].plannedEndDate").value("2026-09-30"));
    }
}

