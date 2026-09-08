package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixCellResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixRowResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixSkillHeaderResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.skill.GetDepartmentSkillMatrixUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentSkillMatrixController Inbound Web Adapter Tests")
class DepartmentSkillMatrixControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetDepartmentSkillMatrixUseCase getDepartmentSkillMatrixUseCase;

    @InjectMocks
    private DepartmentSkillMatrixController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/skills/matrix?orgUnitId=10 -> Trả về ma trận kỹ năng bộ phận 200 OK")
    void getMatrix_WithQueryParam_Returns200() throws Exception {
        DepartmentSkillMatrixResult result = new DepartmentSkillMatrixResult(
                10L,
                "DEV-TEAM",
                "Đội ngũ Phát triển",
                List.of(
                        new SkillMatrixSkillHeaderResult(1L, "JAVA", "Java", "Backend", 2, false),
                        new SkillMatrixSkillHeaderResult(2L, "DOCKER", "Docker", "DevOps", 1, true)
                ),
                List.of(
                        new SkillMatrixRowResult(101L, "EMP001", "Nguyễn Văn A", "Backend Dev",
                                Map.of(1L, new SkillMatrixCellResult(1L, 4, new BigDecimal("3.0"), "Tốt")))
                ),
                new SkillMatrixSummaryResult(1, 2, 1, 0)
        );

        when(getDepartmentSkillMatrixUseCase.execute(10L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/skills/matrix?orgUnitId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgUnitId").value(10))
                .andExpect(jsonPath("$.data.orgUnitName").value("Đội ngũ Phát triển"))
                .andExpect(jsonPath("$.data.skills[0].code").value("JAVA"))
                .andExpect(jsonPath("$.data.skills[0].employeeCount").value(2))
                .andExpect(jsonPath("$.data.skills[1].code").value("DOCKER"))
                .andExpect(jsonPath("$.data.skills[1].singlePersonRisk").value(true))
                .andExpect(jsonPath("$.data.rows[0].employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.data.rows[0].skills.1.proficiencyLevel").value(4))
                .andExpect(jsonPath("$.data.summary.totalEmployees").value(1))
                .andExpect(jsonPath("$.data.summary.singlePersonRiskSkillCount").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/skills/matrix/10 -> Gọi qua Path Variable trả về 200 OK")
    void getMatrix_WithPathVariable_Returns200() throws Exception {
        DepartmentSkillMatrixResult result = new DepartmentSkillMatrixResult(
                10L, "DEV-TEAM", "Đội ngũ Phát triển", List.of(), List.of(),
                new SkillMatrixSummaryResult(0, 0, 0, 0)
        );

        when(getDepartmentSkillMatrixUseCase.execute(10L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/skills/matrix/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgUnitId").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/skills/matrix?orgUnitId=999 -> Không tìm thấy đơn vị trả về 404 Not Found")
    void getMatrix_OrgUnitNotFound_Returns404() throws Exception {
        when(getDepartmentSkillMatrixUseCase.execute(999L))
                .thenThrow(new OrgUnitNotFoundException("Không tìm thấy đơn vị/bộ phận với ID: 999"));

        mockMvc.perform(get("/api/v1/skills/matrix?orgUnitId=999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORG_UNIT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/skills/matrix?orgUnitId=10 -> Không có quyền truy cập trả về 403 Forbidden")
    void getMatrix_PermissionDenied_Returns403() throws Exception {
        when(getDepartmentSkillMatrixUseCase.execute(10L))
                .thenThrow(new PermissionDeniedException(PermissionCode.EMPLOYEE_SKILL_READ));

        mockMvc.perform(get("/api/v1/skills/matrix?orgUnitId=10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
