package com.hrm.employeemanagement.application.service.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.report.LoadRecruitmentDemandReportPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.report.RecruitmentSkillDemand;
import com.hrm.employeemanagement.domain.skill.Skill;

class RecruitmentDemandReportServiceTest {

    private AuthorizationService authorizationService;
    private LoadRecruitmentDemandReportPort loadReportPort;
    private SaveAuditLogInNewTransactionPort saveAuditLogPort;
    private RecruitmentDemandReportService service;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadReportPort = mock(LoadRecruitmentDemandReportPort.class);
        saveAuditLogPort = mock(SaveAuditLogInNewTransactionPort.class);

        service = new RecruitmentDemandReportService(
                authorizationService,
                loadReportPort,
                saveAuditLogPort
        );
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-01: Luồng thành công - Dự án thiếu 400 giờ kỹ năng kiểm thử")
    void testTC01_SuccessFlow_TestingSkillShortfall() {
        Long currentUserId = 100L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Skill testingSkill = new Skill(
                1L, "TESTING", "Kiểm thử (Testing / QA)", "Testing", "Mô tả", LocalDateTime.now()
        );
        when(loadReportPort.loadAllActiveSkills()).thenReturn(List.of(testingSkill));

        // Nhu cầu dự án: 400 giờ kỹ năng kiểm thử
        when(loadReportPort.loadProjectDemandHoursGroupedBySkill(any(), any(), any(), any(), any()))
                .thenReturn(Map.of(1L, BigDecimal.valueOf(400.0)));

        // Năng lực hiện có: 0 giờ
        when(loadReportPort.loadAvailableCapacityHoursGroupedBySkill(any(), any(), any(), any(), any()))
                .thenReturn(Map.of(1L, BigDecimal.ZERO));

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

        // [TC-04] Đảm bảo đã ghi vết audit log
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-02: Dữ liệu rỗng - Năng lực hiện có đủ cho mọi nhu cầu")
    void testTC02_EmptyData_SufficientCapacity() {
        Long currentUserId = 100L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Skill devSkill = new Skill(
                2L, "JAVA", "Lập trình Java", "Backend", "Mô tả", LocalDateTime.now()
        );
        when(loadReportPort.loadAllActiveSkills()).thenReturn(List.of(devSkill));

        // Nhu cầu dự án: 100 giờ; Năng lực hiện có: 160 giờ
        when(loadReportPort.loadProjectDemandHoursGroupedBySkill(any(), any(), any(), any(), any()))
                .thenReturn(Map.of(2L, BigDecimal.valueOf(100.0)));
        when(loadReportPort.loadAvailableCapacityHoursGroupedBySkill(any(), any(), any(), any(), any()))
                .thenReturn(Map.of(2L, BigDecimal.valueOf(160.0)));

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
    @DisplayName("NCL-10-CN-005-TC-03: Không có quyền - Người dùng không phải Ban Giám Đốc hoặc HR")
    void testTC03_UnauthorizedAccess_ThrowsException() {
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ))
                .thenThrow(new PermissionDeniedException(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ));

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 4, null);

        assertThrows(PermissionDeniedException.class, () -> service.execute(query));
    }

    @Test
    @DisplayName("NCL-10-CN-005-TC-04: Lưu lịch sử - Ghi nhật ký audit log khi truy cập/xác nhận báo cáo")
    void testTC04_AuditLogSavedOnReportQuery() {
        Long currentUserId = 105L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        Skill skill = new Skill(
                3L, "REACT", "React.js", "Frontend", "Mô tả", LocalDateTime.now()
        );
        when(loadReportPort.loadAllActiveSkills()).thenReturn(List.of(skill));

        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(2026, 1, 2026, 2, 10L);
        service.execute(query);

        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("NCL-10-CN-005-MEDIUM-01: Time Range Validation - Pair & Partial Range Validation")
    void testTimeRangeValidation_PairAndPartialRange() {
        Long currentUserId = 100L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        // Chỉ truyền fromYear mà không có fromWeek -> Ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2026, null, 2026, 4, null))
        );

        // Chỉ truyền fromWeek mà không có fromYear -> Ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(null, 1, 2026, 4, null))
        );

        // Chỉ truyền toYear mà không có toWeek -> Ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2026, 1, 2026, null, null))
        );

        // Chỉ truyền toWeek mà không có toYear -> Ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2026, 1, null, 4, null))
        );

        // Đơn lẻ from range mà không có to range (partial range) -> Ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2026, 1, null, null, null))
        );
    }

    @Test
    @DisplayName("NCL-10-CN-005-MEDIUM-01: Time Range Validation - ISO Week validation & year comparison")
    void testTimeRangeValidation_IsoWeekAndOrder() {
        Long currentUserId = 100L;
        when(authorizationService.require(PermissionCode.RECRUITMENT_DEMAND_REPORT_READ)).thenReturn(currentUserId);

        // Week = 0
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2026, 0, 2026, 4, null))
        );

        // Week < 0
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2026, -1, 2026, 4, null))
        );

        // Week > 53
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2026, 54, 2026, 4, null))
        );

        // Week 53 trong năm 2025 (năm 2025 chỉ có 52 ISO weeks) -> Ném exception
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2025, 53, 2025, 53, null))
        );

        // Week 53 trong năm 2020 (năm 2020 có 53 ISO weeks) -> Hợp lệ!
        when(loadReportPort.loadAllActiveSkills()).thenReturn(List.of());
        assertDoesNotThrow(() ->
                service.execute(new RecruitmentDemandReportQuery(2020, 1, 2020, 53, null))
        );

        // from > to (cùng năm, fromWeek > toWeek)
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2026, 10, 2026, 4, null))
        );

        // from > to (fromYear > toYear)
        assertThrows(IllegalArgumentException.class, () ->
                service.execute(new RecruitmentDemandReportQuery(2027, 1, 2026, 4, null))
        );

        // Hợp lệ trong cùng năm
        assertDoesNotThrow(() ->
                service.execute(new RecruitmentDemandReportQuery(2026, 1, 2026, 4, null))
        );

        // Hợp lệ qua nhiều năm
        assertDoesNotThrow(() ->
                service.execute(new RecruitmentDemandReportQuery(2025, 40, 2026, 10, null))
        );
    }
}
