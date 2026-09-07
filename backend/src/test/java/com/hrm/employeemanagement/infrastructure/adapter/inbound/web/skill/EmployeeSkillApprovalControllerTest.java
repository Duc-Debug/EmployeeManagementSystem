package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.skill.ApproveEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.PendingEmployeeSkillItemResult;
import com.hrm.employeemanagement.application.port.inbound.skill.ApproveEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.GetPendingEmployeeSkillsUseCase;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeSkillApprovalController Web Inbound Adapter Tests")
class EmployeeSkillApprovalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApproveEmployeeSkillUseCase approveEmployeeSkillUseCase;

    @Mock
    private GetPendingEmployeeSkillsUseCase getPendingEmployeeSkillsUseCase;

    @InjectMocks
    private EmployeeSkillApprovalController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SkillExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/employee-skills/pending -> Trả về danh sách kỹ năng chờ duyệt")
    void getPendingSkills_Success() throws Exception {
        PendingEmployeeSkillItemResult item = new PendingEmployeeSkillItemResult(
                10L, 101L, "EMP001", "Nguyễn Văn A", 10L, "Phòng IT",
                1L, "JAVA", "Java", "Backend", 3, new BigDecimal("2.0"),
                "PENDING", LocalDateTime.now()
        );

        when(getPendingEmployeeSkillsUseCase.execute(null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/employee-skills/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.data[0].skillName").value("Java"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("PUT /api/v1/employee-skills/{id}/approve -> Xác nhận thành công trả về 200 OK")
    void approveSkill_Success() throws Exception {
        EmployeeSkillResult result = new EmployeeSkillResult(
                10L, 101L, 1L, "Java", "JAVA", "Backend",
                4, new BigDecimal("2.0"), "APPROVED", 2L,
                LocalDateTime.now(), null, "Đạt mức 4/5",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(approveEmployeeSkillUseCase.execute(any(ApproveEmployeeSkillCommand.class))).thenReturn(result);

        mockMvc.perform(put("/api/v1/employee-skills/10/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adjustedProficiencyLevel\": 4, \"reviewNotes\": \"Đạt mức 4/5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.proficiencyLevel").value(4))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewNotes").value("Đạt mức 4/5"));
    }

    @Test
    @DisplayName("PUT /api/v1/employee-skills/{id}/approve -> Validation thất bại khi mức điểm ngoài khoảng 1-5")
    void approveSkill_ValidationFailure_InvalidProficiency() throws Exception {
        mockMvc.perform(put("/api/v1/employee-skills/10/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adjustedProficiencyLevel\": 6, \"reviewNotes\": \"Ghi chú\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/employee-skills/{id}/approve -> Không tìm thấy bản ghi kỹ năng trả về 404 Not Found")
    void approveSkill_NotFound_Returns404() throws Exception {
        when(approveEmployeeSkillUseCase.execute(any(ApproveEmployeeSkillCommand.class)))
                .thenThrow(new EmployeeSkillNotFoundException("Không tìm thấy bản ghi kỹ năng nhân sự với ID: 999"));

        mockMvc.perform(put("/api/v1/employee-skills/999/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adjustedProficiencyLevel\": 3, \"reviewNotes\": \"Ghi chú hợp lệ\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy bản ghi kỹ năng nhân sự với ID: 999"));
    }
}
