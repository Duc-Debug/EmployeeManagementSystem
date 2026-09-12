package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportResult;
import com.hrm.employeemanagement.application.port.inbound.report.GetRecruitmentDemandReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.ExportRecruitmentDemandReportUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.report.RecruitmentSkillDemand;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitmentDemandReportController Web Adapter Tests")
class RecruitmentDemandReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetRecruitmentDemandReportUseCase getRecruitmentDemandReportUseCase;

    @Mock
    private ExportRecruitmentDemandReportUseCase exportRecruitmentDemandReportUseCase;

    @InjectMocks
    private RecruitmentDemandReportController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-01: API - Lấy báo cáo nhu cầu tuyển dụng thành công (200 OK)")
    void testGetReport_Success() throws Exception {
        RecruitmentSkillDemand testingSkill = new RecruitmentSkillDemand(
                1L, "TESTING", "Kiểm thử (Testing / QA)", "Testing",
                BigDecimal.valueOf(400), BigDecimal.ZERO
        );

        RecruitmentDemandReportResult mockResult = new RecruitmentDemandReportResult(
                1, BigDecimal.valueOf(400), 1, List.of(testingSkill), "2026-W01 đến 2026-W04", LocalDateTime.now()
        );

        when(getRecruitmentDemandReportUseCase.execute(org.mockito.ArgumentMatchers.any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/reports/recruitment-demand")
                        .param("fromYear", "2026")
                        .param("fromWeek", "1")
                        .param("toYear", "2026")
                        .param("toWeek", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDeficitHours").value(400.0))
                .andExpect(jsonPath("$.data.skills[0].skillCode").value("TESTING"))
                .andExpect(jsonPath("$.data.skills[0].shortfallHours").value(400.0));
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-03: API - Không có quyền truy cập trả về 403 Forbidden")
    void testGetReport_PermissionDenied_Returns403() throws Exception {
        when(getRecruitmentDemandReportUseCase.execute(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ));

        mockMvc.perform(get("/api/v1/reports/recruitment-demand"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
