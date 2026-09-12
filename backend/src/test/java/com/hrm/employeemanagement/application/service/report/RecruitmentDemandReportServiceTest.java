package com.hrm.employeemanagement.application.service.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportQuery;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandMetrics;
import com.hrm.employeemanagement.application.dto.report.RecruitmentCapacityMetrics;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.report.LoadRecruitmentDemandReportPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.report.RecruitmentSkillDemand;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class RecruitmentDemandReportServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadRecruitmentDemandReportPort loadReportPort;
    private SaveAuditLogInNewTransactionPort saveAuditLogPort;
    private RecruitmentDemandReportService service;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        loadReportPort = mock(LoadRecruitmentDemandReportPort.class);
        saveAuditLogPort = mock(SaveAuditLogInNewTransactionPort.class);

        service = new RecruitmentDemandReportService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadReportPort,
                saveAuditLogPort
        );
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-01: Luồng thành công - Dự án thiếu 400 giờ kỹ năng kiểm thử")
    void testTC01_SuccessFlow_TestingSkillShortfall() {
        Long currentUserId = 100L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Role role = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám đốc");
        User user = new User(new UserId(100L), "director", "hash", role, UserStatus.ACTIVE, null, DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(user));

        Skill testingSkill = new Skill(
                1L, "TESTING", "Kiểm thử (Testing / QA)", "Testing", "Mô tả", LocalDateTime.now()
        );
        when(loadReportPort.loadAllActiveSkills()).thenReturn(List.of(testingSkill));

        when(loadReportPort.loadProjectDemandMetrics(any(), any(), any(), any(), any()))
                .thenReturn(new RecruitmentDemandMetrics(Map.of(1L, BigDecimal.valueOf(400.0)), BigDecimal.ZERO, 0));
        when(loadReportPort.loadAvailableCapacityMetrics(any(), any(), any(), any(), any()))
                .thenReturn(new RecruitmentCapacityMetrics(Map.of(1L, BigDecimal.ZERO), BigDecimal.ZERO));

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 4, null);
        RecruitmentDemandReportResult result = service.execute(query);

        assertNotNull(result);
        assertEquals(1, result.totalSkillsEvaluated());
        assertEquals(1, result.skillsWithDeficitCount());
        assertEquals(0, BigDecimal.valueOf(400).compareTo(result.totalDeficitHours()));

        RecruitmentSkillDemand demand = result.skills().get(0);
        assertEquals("TESTING", demand.getSkillCode());
        assertEquals("Kiểm thử (Testing / QA)", demand.getSkillName());
        assertEquals(0, BigDecimal.valueOf(400).compareTo(demand.getRequiredDemandHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(demand.getAvailableCapacityHours()));
        assertEquals(0, BigDecimal.valueOf(400).compareTo(demand.getShortfallHours()));
        assertTrue(demand.isDeficit());
        assertEquals("DEFICIT", demand.getStatus());

        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-02: Dữ liệu rỗng - Năng lực hiện có đủ cho mọi nhu cầu")
    void testTC02_EmptyData_SufficientCapacity() {
        Long currentUserId = 100L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Role role = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám đốc");
        User user = new User(new UserId(100L), "director", "hash", role, UserStatus.ACTIVE, null, DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(user));

        Skill devSkill = new Skill(
                2L, "JAVA", "Lập trình Java", "Backend", "Mô tả", LocalDateTime.now()
        );
        when(loadReportPort.loadAllActiveSkills()).thenReturn(List.of(devSkill));

        when(loadReportPort.loadProjectDemandMetrics(any(), any(), any(), any(), any()))
                .thenReturn(new RecruitmentDemandMetrics(Map.of(2L, BigDecimal.valueOf(100.0)), BigDecimal.ZERO, 0));
        when(loadReportPort.loadAvailableCapacityMetrics(any(), any(), any(), any(), any()))
                .thenReturn(new RecruitmentCapacityMetrics(Map.of(2L, BigDecimal.valueOf(160.0)), BigDecimal.ZERO));

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 4, null);
        RecruitmentDemandReportResult result = service.execute(query);

        assertNotNull(result);
        assertEquals(1, result.totalSkillsEvaluated());
        assertEquals(0, result.skillsWithDeficitCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalDeficitHours()));

        RecruitmentSkillDemand demand = result.skills().get(0);
        assertEquals("JAVA", demand.getSkillCode());
        assertEquals(0, BigDecimal.ZERO.compareTo(demand.getShortfallHours()));
        assertFalse(demand.isDeficit());
        assertEquals("SUFFICIENT", demand.getStatus());
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-03: Không có quyền - Người dùng không có quyền RECRUITMENT_DEMAND_REPORT_READ")
    void testTC03_UnauthorizedAccess_ThrowsException() {
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ))
                .thenThrow(new PermissionDeniedException(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ));

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 4, null);

        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
    }

    @Test
    @DisplayName("DataScope ORGANIZATION_BRANCH - In-scope child orgUnitId allowed")
    void testDataScope_OrganizationBranch_InScope_Allowed() {
        Long currentUserId = 101L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Role role = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        User user = new User(new UserId(101L), "manager", "hash", role, UserStatus.ACTIVE, null, DataScope.ORGANIZATION_BRANCH, 10L, 0L);
        when(loadUserPort.findById(new UserId(101L))).thenReturn(Optional.of(user));

        when(loadOrgUnitPort.existsInOrgUnitBranch(15L, 10L)).thenReturn(true);
        when(loadReportPort.loadAllActiveSkills()).thenReturn(List.of());

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 4, 15L);
        RecruitmentDemandReportResult result = service.execute(query);

        assertNotNull(result);
    }

    @Test
    @DisplayName("DataScope ORGANIZATION_BRANCH - Out of scope orgUnitId throws PermissionDeniedException")
    void testDataScope_OrganizationBranch_OutsideScope_ThrowsException() {
        Long currentUserId = 101L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Role role = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        User user = new User(new UserId(101L), "manager", "hash", role, UserStatus.ACTIVE, null, DataScope.ORGANIZATION_BRANCH, 10L, 0L);
        when(loadUserPort.findById(new UserId(101L))).thenReturn(Optional.of(user));

        when(loadOrgUnitPort.existsInOrgUnitBranch(20L, 10L)).thenReturn(false);

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 4, 20L);

        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
    }

    @Test
    @DisplayName("DataScope SELF - Denies access to Recruitment Demand Report")
    void testDataScope_Self_ThrowsException() {
        Long currentUserId = 102L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Role role = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
        User user = new User(new UserId(102L), "staff", "hash", role, UserStatus.ACTIVE, null, DataScope.SELF, null, 0L);
        when(loadUserPort.findById(new UserId(102L))).thenReturn(Optional.of(user));

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 4, null);

        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-04: Lưu lịch sử - Ghi nhật ký audit log khi truy cập/xác nhận báo cáo")
    void testTC04_AuditLogSavedOnReportQuery() {
        Long currentUserId = 105L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Role role = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám đốc");
        User user = new User(new UserId(105L), "director", "hash", role, UserStatus.ACTIVE, null, DataScope.COMPANY, null, 0L);
        when(loadUserPort.findById(new UserId(105L))).thenReturn(Optional.of(user));

        Skill skill = new Skill(
                3L, "REACT", "React.js", "Frontend", "Mô tả", LocalDateTime.now()
        );
        when(loadReportPort.loadAllActiveSkills()).thenReturn(List.of(skill));

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 2, 10L);
        service.execute(query);

        verify(saveAuditLogPort).save(any());
    }
}
