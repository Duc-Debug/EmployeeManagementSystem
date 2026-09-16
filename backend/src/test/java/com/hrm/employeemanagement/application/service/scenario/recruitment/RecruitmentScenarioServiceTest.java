package com.hrm.employeemanagement.application.service.scenario.recruitment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.scenario.recruitment.AddSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RecruitmentScenarioEvaluationResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.RemoveSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.SimulatedEmployeeResult;
import com.hrm.employeemanagement.application.dto.scenario.recruitment.UpdateSimulatedEmployeeCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.DeleteSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadScenarioShortfallPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.LoadSimulationScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.recruitment.SaveSimulatedEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.InvalidSimulatedEmployeeException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.recruitment.SimulatedEmployeeNotFoundException;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.scenario.recruitment.RoleShortfallDemand;
import com.hrm.employeemanagement.domain.scenario.recruitment.ScenarioSimulatedEmployee;
import com.hrm.employeemanagement.domain.scenario.recruitment.SimulatedEmployeeId;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitmentScenarioService Application Unit Tests (All 4 Scenarios for NCL-08-CN-005)")
class RecruitmentScenarioServiceTest {

    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private LoadSimulationScenarioPort loadScenarioPort;
    @Mock
    private LoadScenarioShortfallPort loadShortfallPort;
    @Mock
    private SaveSimulatedEmployeePort saveEmployeePort;
    @Mock
    private LoadSimulatedEmployeePort loadEmployeePort;
    @Mock
    private DeleteSimulatedEmployeePort deleteEmployeePort;
    @Mock
    private LoadProjectRolePort loadRolePort;
    @Mock
    private LoadSkillPort loadSkillPort;
    @Mock
    private SaveAuditLogInNewTransactionPort saveAuditLogPort;

    private RecruitmentScenarioService service;

    private final Long scenarioId = 100L;
    private final Long rmUserId = 2L; // VT-03 Quản lý nguồn lực
    private final Long devRoleId = 10L;
    private final Long testerRoleId = 20L;

    @BeforeEach
    void setUp() {
        service = new RecruitmentScenarioService(
                authorizationService,
                loadScenarioPort,
                loadShortfallPort,
                saveEmployeePort,
                loadEmployeePort,
                deleteEmployeePort,
                loadRolePort,
                loadSkillPort,
                saveAuditLogPort
        );

        lenient().when(loadScenarioPort.findById(anyLong())).thenReturn(
                Optional.of(new LoadSimulationScenarioPort.SimulationScenarioInfo(scenarioId, "SCN-01", "Kịch bản tuyển dụng", "DRAFT"))
        );
    }

    @Test
    @DisplayName("NCL-08-CN-005-TC-01: Luồng thành công - Kịch bản đang thiếu 160h vai trò lập trình, thêm 1 nhân sự toàn thời gian, hệ thống chạy lại và báo còn thiếu 0h")
    void scenario1_Success_Shortfall160h_AddFullTimeCandidate_ShouldReduceShortfallToZero() {
        // Given: Kịch bản đang thiếu 160 giờ vai trò lập trình (DEVELOPER)
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadRolePort.findById(new ProjectRoleId(devRoleId))).thenReturn(
                Optional.of(new ProjectRole(new ProjectRoleId(devRoleId), "DEV", "Developer", "Mô tả"))
        );

        RoleShortfallDemand devDemand = new RoleShortfallDemand(devRoleId, "DEV", "Developer", new BigDecimal("160.00"));
        when(loadShortfallPort.loadShortfallDemands(scenarioId)).thenReturn(List.of(devDemand));

        // When: Thêm một nhân sự giả định toàn thời gian (40h/tuần x 4 tuần = 160h)
        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                scenarioId,
                "Lập trình viên Giả định 1",
                devRoleId,
                null,
                new BigDecimal("40.00"),
                4,
                "Tuyển full-time cho dự án"
        );

        ScenarioSimulatedEmployee savedCandidate = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(1L),
                scenarioId,
                "Lập trình viên Giả định 1",
                devRoleId,
                null,
                new BigDecimal("40.00"),
                4,
                "Tuyển full-time cho dự án",
                rmUserId,
                null,
                null,
                0L
        );

        when(saveEmployeePort.save(any(ScenarioSimulatedEmployee.class))).thenReturn(savedCandidate);
        when(loadEmployeePort.findByScenarioId(scenarioId)).thenReturn(List.of(savedCandidate));

        // Act
        RecruitmentScenarioEvaluationResult result = service.addSimulatedEmployee(command);

        // Then: Hệ thống chạy lại và cho biết số giờ còn thiếu giảm về 0h, không còn vỡ kế hoạch
        assertNotNull(result);
        assertEquals(scenarioId, result.scenarioId());
        assertEquals(0, new BigDecimal("160.00").compareTo(result.totalOriginalShortfallHours()));
        assertEquals(0, new BigDecimal("160.00").compareTo(result.totalSimulatedCapacityHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalRemainingShortfallHours()));
        assertFalse(result.isPlanBroken(), "Kịch bản phải không còn vỡ kế hoạch khi đã bù đủ giờ");
        assertEquals(0, result.overloadedRoleCount());
        assertEquals(1, result.totalSimulatedEmployeesCount());
        assertEquals(0, result.totalSuggestedRecruitsNeeded());

        verify(saveEmployeePort, times(1)).save(any(ScenarioSimulatedEmployee.class));
        verify(saveAuditLogPort, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-08-CN-005-TC-02: Luồng thành công - Đã thêm đủ nhân sự giả định, chạy lại kịch bản báo không còn ai vỡ kế hoạch và nêu tổng số người cần tuyển")
    void scenario2_Success_AllCandidatesAdded_RerunScenario_ShouldReportNoBrokenPlanAndSummary() {
        // Given: Kịch bản có 2 vai trò thiếu giờ: DEV (160h) và TESTER (80h)
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);

        RoleShortfallDemand devDemand = new RoleShortfallDemand(devRoleId, "DEV", "Developer", new BigDecimal("160.00"));
        RoleShortfallDemand testerDemand = new RoleShortfallDemand(testerRoleId, "TESTER", "Tester", new BigDecimal("80.00"));
        when(loadShortfallPort.loadShortfallDemands(scenarioId)).thenReturn(List.of(devDemand, testerDemand));

        // Đã thêm đủ nhân sự giả định: 1 Dev full-time (160h) và 1 Tester part-time (80h)
        ScenarioSimulatedEmployee devCandidate = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(1L), scenarioId, "Dev 1", devRoleId, null, new BigDecimal("40.00"), 4, null, rmUserId, null, null, 0L
        );
        ScenarioSimulatedEmployee testerCandidate = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(2L), scenarioId, "Tester 1", testerRoleId, null, new BigDecimal("20.00"), 4, null, rmUserId, null, null, 0L
        );
        when(loadEmployeePort.findByScenarioId(scenarioId)).thenReturn(List.of(devCandidate, testerCandidate));

        // When: Chạy lại kịch bản
        RecruitmentScenarioEvaluationResult result = service.rerunRecruitmentScenario(scenarioId);

        // Then: Hệ thống báo không còn ai vỡ kế hoạch và nêu tổng số người cần tuyển
        assertNotNull(result);
        assertFalse(result.isPlanBroken());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalRemainingShortfallHours()));
        assertEquals(0, result.overloadedRoleCount());
        assertEquals(2, result.totalSimulatedEmployeesCount());
        assertEquals(0, result.totalSuggestedRecruitsNeeded(), "Khi đã bù đủ thì số người cần tuyển bổ sung là 0");

        // Verify audit log ghi nhận RERUN
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        assertEquals("RERUN_RECRUITMENT_SCENARIO", auditCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("NCL-08-CN-005-TC-03: Không có quyền - Người dùng không phải Quản lý nguồn lực (không có quyền VT-03) -> Từ chối truy cập và ghi nhật ký lần từ chối")
    void scenario3_Unauthorized_NonVT03User_ShouldThrow403AndRecordAccessDeniedAuditLog() {
        // Given: Người dùng không phải Quản lý nguồn lực (AuthorizationService ném PermissionDeniedException)
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE));

        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                scenarioId, "Dev Giả Định", devRoleId, null, new BigDecimal("40.00"), 4, null
        );

        // When & Then: Hệ thống từ chối truy cập
        assertThrows(PermissionDeniedException.class, () -> service.addSimulatedEmployee(command));

        // And: Tự động ghi nhật ký lần từ chối (ACCESS_DENIED)
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        assertEquals("ACCESS_DENIED", auditCaptor.getValue().getAction());
        assertEquals("SCENARIO_SIMULATED_EMPLOYEE", auditCaptor.getValue().getTableName());

        // Đảm bảo không lưu dữ liệu gì
        verify(saveEmployeePort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-08-CN-005-TC-04: Lưu lịch sử - Có thay đổi liên quan tới kịch bản tuyển thêm nhân sự -> Ghi lại người thực hiện, nội dung và thời điểm")
    void scenario4_AuditLog_Modification_ShouldRecordFullAuditTrail() {
        // Given
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadRolePort.findById(new ProjectRoleId(devRoleId))).thenReturn(
                Optional.of(new ProjectRole(new ProjectRoleId(devRoleId), "DEV", "Developer", "Mô tả"))
        );

        ScenarioSimulatedEmployee savedCandidate = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(101L), scenarioId, "Dev Cần Tuyển", devRoleId, null,
                new BigDecimal("40.00"), 4, "Ghi chú tuyển dụng", rmUserId, null, null, 0L
        );
        when(saveEmployeePort.save(any())).thenReturn(savedCandidate);
        when(loadEmployeePort.findByScenarioId(scenarioId)).thenReturn(List.of(savedCandidate));
        when(loadShortfallPort.loadShortfallDemands(scenarioId)).thenReturn(List.of());

        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                scenarioId, "Dev Cần Tuyển", devRoleId, null, new BigDecimal("40.00"), 4, "Ghi chú tuyển dụng"
        );

        // When: Xác nhận thao tác thêm nhân sự giả định
        service.addSimulatedEmployee(command);

        // Then: Hệ thống ghi lại người thực hiện, nội dung và thời điểm vào audit_logs
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());

        AuditLog log = auditCaptor.getValue();
        assertEquals(rmUserId, log.getUserId());
        assertEquals("ADD_SIMULATED_EMPLOYEE", log.getAction());
        assertEquals("scenario_simulated_employees", log.getTableName());
        assertEquals(101L, log.getRecordId());
        assertNotNull(log.getNewValue());
        assertTrue(log.getNewValue().contains("Dev Cần Tuyển"));
        assertNotNull(log.getCreatedAt());
    }

    @Test
    @DisplayName("QTN-14 Verification: Mọi thao tác mô phỏng chỉ lưu ở bản nháp kịch bản, không gọi bất kỳ port phân bổ hoặc nhân viên thật nào")
    void qtn14_SimulationDataOnly_EnsureNoRealEmployeeOrAllocationModified() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadRolePort.findById(new ProjectRoleId(devRoleId))).thenReturn(
                Optional.of(new ProjectRole(new ProjectRoleId(devRoleId), "DEV", "Developer", "Mô tả"))
        );

        ScenarioSimulatedEmployee savedCandidate = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(55L), scenarioId, "Candidate nháp", devRoleId, null,
                new BigDecimal("40.00"), 4, null, rmUserId, null, null, 0L
        );
        when(saveEmployeePort.save(any())).thenReturn(savedCandidate);
        when(loadEmployeePort.findByScenarioId(scenarioId)).thenReturn(List.of(savedCandidate));
        when(loadShortfallPort.loadShortfallDemands(scenarioId)).thenReturn(List.of());

        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                scenarioId, "Candidate nháp", devRoleId, null, new BigDecimal("40.00"), 4, null
        );

        service.addSimulatedEmployee(command);

        // Xác nhận chỉ gọi save trên bản nháp kịch bản
        verify(saveEmployeePort, times(1)).save(any(ScenarioSimulatedEmployee.class));
    }

    @Test
    @DisplayName("Kịch bản không tồn tại -> Ném ScenarioNotFoundException")
    void scenarioNotFound_ShouldThrow() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(999L)).thenReturn(false);

        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                999L, "Dev", devRoleId, null, new BigDecimal("40.00"), 4, null
        );

        assertThrows(ScenarioNotFoundException.class, () -> service.addSimulatedEmployee(command));
    }

    @Test
    @DisplayName("Xóa nhân sự giả định thành công và chạy lại kịch bản cập nhật số giờ thiếu")
    void removeSimulatedEmployee_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);

        ScenarioSimulatedEmployee existing = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(1L), scenarioId, "Dev 1", devRoleId, null, new BigDecimal("40.00"), 4, null, rmUserId, null, null, 0L
        );
        when(loadEmployeePort.findById(new SimulatedEmployeeId(1L))).thenReturn(Optional.of(existing));
        when(loadShortfallPort.loadShortfallDemands(scenarioId)).thenReturn(
                List.of(new RoleShortfallDemand(devRoleId, "DEV", "Developer", new BigDecimal("160.00")))
        );
        when(loadEmployeePort.findByScenarioId(scenarioId)).thenReturn(List.of()); // Sau khi xóa không còn ai

        RecruitmentScenarioEvaluationResult result = service.removeSimulatedEmployee(new RemoveSimulatedEmployeeCommand(scenarioId, 1L));

        assertNotNull(result);
        assertTrue(result.isPlanBroken());
        assertEquals(0, new BigDecimal("160.00").compareTo(result.totalRemainingShortfallHours()));
        verify(deleteEmployeePort).deleteById(new SimulatedEmployeeId(1L));
    }

    @Test
    @DisplayName("Lấy danh sách nhân sự giả định của kịch bản và làm giàu thông tin vai trò")
    void getSimulatedEmployees_Success() {
        when(authorizationService.requireAny(any(), any())).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);

        ScenarioSimulatedEmployee candidate = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(1L), scenarioId, "Dev 1", devRoleId, null, new BigDecimal("40.00"), 4, "Notes", rmUserId, null, null, 0L
        );
        when(loadEmployeePort.findByScenarioId(scenarioId)).thenReturn(List.of(candidate));
        when(loadRolePort.findById(new ProjectRoleId(devRoleId))).thenReturn(
                Optional.of(new ProjectRole(new ProjectRoleId(devRoleId), "DEV", "Developer", "Mô tả"))
        );

        List<SimulatedEmployeeResult> list = service.getSimulatedEmployees(scenarioId);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("DEV", list.getFirst().projectRoleCode());
        assertEquals("Developer", list.getFirst().projectRoleName());
    }

    @Test
    @DisplayName("Cập nhật nhân sự giả định thành công và ghi nhận nhật ký kiểm toán UPDATE_SIMULATED_EMPLOYEE")
    void updateSimulatedEmployee_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadRolePort.findById(new ProjectRoleId(devRoleId))).thenReturn(
                Optional.of(new ProjectRole(new ProjectRoleId(devRoleId), "DEV", "Developer", "Mô tả"))
        );

        ScenarioSimulatedEmployee existing = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(10L), scenarioId, "Cũ", devRoleId, null, new BigDecimal("20.00"), 4, null, rmUserId, null, null, 0L
        );
        when(loadEmployeePort.findById(new SimulatedEmployeeId(10L))).thenReturn(Optional.of(existing));
        when(saveEmployeePort.save(any(ScenarioSimulatedEmployee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loadShortfallPort.loadShortfallDemands(scenarioId)).thenReturn(
                List.of(new RoleShortfallDemand(devRoleId, "DEV", "Developer", new BigDecimal("160.00")))
        );
        when(loadEmployeePort.findByScenarioId(scenarioId)).thenReturn(List.of(existing));

        UpdateSimulatedEmployeeCommand command = new UpdateSimulatedEmployeeCommand(
                scenarioId, 10L, "Mới", devRoleId, null, new BigDecimal("40.00"), 4, "Ghi chú mới"
        );

        RecruitmentScenarioEvaluationResult result = service.updateSimulatedEmployee(command);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalRemainingShortfallHours()));
        assertFalse(result.isPlanBroken());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        assertEquals("UPDATE_SIMULATED_EMPLOYEE", auditCaptor.getValue().getAction());
        assertEquals(10L, auditCaptor.getValue().getRecordId());
    }

    @Test
    @DisplayName("Cập nhật nhân sự giả định không tồn tại -> Ném SimulatedEmployeeNotFoundException")
    void updateSimulatedEmployee_NotFound_ShouldThrow() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadEmployeePort.findById(new SimulatedEmployeeId(999L))).thenReturn(Optional.empty());

        UpdateSimulatedEmployeeCommand command = new UpdateSimulatedEmployeeCommand(
                scenarioId, 999L, "Mới", devRoleId, null, new BigDecimal("40.00"), 4, null
        );

        assertThrows(SimulatedEmployeeNotFoundException.class, () -> service.updateSimulatedEmployee(command));
    }

    @Test
    @DisplayName("Cập nhật nhân sự giả định thuộc kịch bản khác -> Ném SimulatedEmployeeNotFoundException")
    void updateSimulatedEmployee_WrongScenario_ShouldThrow() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);

        ScenarioSimulatedEmployee otherScenarioEmp = new ScenarioSimulatedEmployee(
                new SimulatedEmployeeId(10L), 999L, "Khác", devRoleId, null, new BigDecimal("40.00"), 4, null, rmUserId, null, null, 0L
        );
        when(loadEmployeePort.findById(new SimulatedEmployeeId(10L))).thenReturn(Optional.of(otherScenarioEmp));

        UpdateSimulatedEmployeeCommand command = new UpdateSimulatedEmployeeCommand(
                scenarioId, 10L, "Mới", devRoleId, null, new BigDecimal("40.00"), 4, null
        );

        assertThrows(SimulatedEmployeeNotFoundException.class, () -> service.updateSimulatedEmployee(command));
    }

    @Test
    @DisplayName("Thêm nhân sự giả định với vai trò không tồn tại -> Ném InvalidSimulatedEmployeeException")
    void addSimulatedEmployee_NonExistentRole_ShouldThrow() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadRolePort.findById(new ProjectRoleId(888L))).thenReturn(Optional.empty());

        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                scenarioId, "Dev", 888L, null, new BigDecimal("40.00"), 4, null
        );

        assertThrows(InvalidSimulatedEmployeeException.class, () -> service.addSimulatedEmployee(command));
    }

    @Test
    @DisplayName("Thêm nhân sự giả định với kỹ năng không tồn tại -> Ném InvalidSimulatedEmployeeException")
    void addSimulatedEmployee_NonExistentSkill_ShouldThrow() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadRolePort.findById(new ProjectRoleId(devRoleId))).thenReturn(
                Optional.of(new ProjectRole(new ProjectRoleId(devRoleId), "DEV", "Developer", "Mô tả"))
        );
        when(loadSkillPort.findById(new SkillId(777L))).thenReturn(Optional.empty());

        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                scenarioId, "Dev", devRoleId, 777L, new BigDecimal("40.00"), 4, null
        );

        assertThrows(InvalidSimulatedEmployeeException.class, () -> service.addSimulatedEmployee(command));
    }

    @Test
    @DisplayName("Thêm nhân sự giả định khi kịch bản đã APPLIED -> Từ chối ném InvalidSimulatedEmployeeException")
    void addSimulatedEmployee_AppliedScenario_ShouldThrow() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(
                Optional.of(new LoadSimulationScenarioPort.SimulationScenarioInfo(scenarioId, "SCN-01", "Kịch bản đã áp dụng", "APPLIED"))
        );

        AddSimulatedEmployeeCommand command = new AddSimulatedEmployeeCommand(
                scenarioId, "Dev", devRoleId, null, new BigDecimal("40.00"), 4, null
        );

        InvalidSimulatedEmployeeException ex = assertThrows(
                InvalidSimulatedEmployeeException.class,
                () -> service.addSimulatedEmployee(command)
        );
        assertTrue(ex.getMessage().contains("DRAFT"));
    }

    @Test
    @DisplayName("Cập nhật nhân sự giả định khi kịch bản đã DISCARDED -> Từ chối ném InvalidSimulatedEmployeeException")
    void updateSimulatedEmployee_DiscardedScenario_ShouldThrow() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(
                Optional.of(new LoadSimulationScenarioPort.SimulationScenarioInfo(scenarioId, "SCN-01", "Kịch bản đã hủy", "DISCARDED"))
        );

        UpdateSimulatedEmployeeCommand command = new UpdateSimulatedEmployeeCommand(
                scenarioId, 1L, "Dev", devRoleId, null, new BigDecimal("40.00"), 4, null
        );

        InvalidSimulatedEmployeeException ex = assertThrows(
                InvalidSimulatedEmployeeException.class,
                () -> service.updateSimulatedEmployee(command)
        );
        assertTrue(ex.getMessage().contains("DRAFT"));
    }

    @Test
    @DisplayName("Xóa nhân sự giả định khi kịch bản đã APPLIED -> Từ chối ném InvalidSimulatedEmployeeException")
    void removeSimulatedEmployee_AppliedScenario_ShouldThrow() {
        when(authorizationService.require(PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE)).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(
                Optional.of(new LoadSimulationScenarioPort.SimulationScenarioInfo(scenarioId, "SCN-01", "Kịch bản đã áp dụng", "APPLIED"))
        );

        RemoveSimulatedEmployeeCommand command = new RemoveSimulatedEmployeeCommand(scenarioId, 1L);

        InvalidSimulatedEmployeeException ex = assertThrows(
                InvalidSimulatedEmployeeException.class,
                () -> service.removeSimulatedEmployee(command)
        );
        assertTrue(ex.getMessage().contains("DRAFT"));
    }

    @Test
    @DisplayName("Lấy đánh giá tuyển dụng (getRecruitmentEvaluation) chỉ yêu cầu quyền READ/MANAGE và không ghi audit log rerun")
    void getRecruitmentEvaluation_RequiresReadPermission() {
        when(authorizationService.requireAny(
                PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_READ,
                PermissionCode.RESOURCE_RECRUITMENT_SCENARIO_MANAGE
        )).thenReturn(rmUserId);
        when(loadScenarioPort.existsById(scenarioId)).thenReturn(true);
        when(loadShortfallPort.loadShortfallDemands(scenarioId)).thenReturn(List.of());
        when(loadEmployeePort.findByScenarioId(scenarioId)).thenReturn(List.of());

        RecruitmentScenarioEvaluationResult result = service.getRecruitmentEvaluation(scenarioId);

        assertNotNull(result);
        assertEquals(scenarioId, result.scenarioId());
        verify(saveAuditLogPort, never()).save(any());
    }
}