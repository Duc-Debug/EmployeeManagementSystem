package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hrm.employeemanagement.application.dto.scenario.CompareScenariosCommand;
import com.hrm.employeemanagement.application.dto.scenario.OverloadedEmployeeResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonItemResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioComparisonResult;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioSimulationResult;
import com.hrm.employeemanagement.application.dto.scenario.WeeklySimulationMetricResult;
import com.hrm.employeemanagement.application.port.inbound.scenario.GetScenarioSimulationResultUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.InsufficientScenariosForComparisonException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScenarioComparisonServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadResourceScenarioPort loadScenarioPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private GetScenarioSimulationResultUseCase simulationResultUseCase;
    private SaveAuditLogPort saveAuditLogPort;

    private ScenarioComparisonService service;

    private User executiveUser;
    private User resourceManagerUser;
    private OrgUnit testOrgUnit;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadScenarioPort = mock(LoadResourceScenarioPort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        simulationResultUseCase = mock(GetScenarioSimulationResultUseCase.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new ScenarioComparisonService(
                authorizationService,
                loadUserPort,
                loadScenarioPort,
                loadOrgUnitPort,
                simulationResultUseCase,
                saveAuditLogPort
        );

        executiveUser = new User(
                new UserId(1L),
                "director_user",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Ban giám đốc"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L
        );

        resourceManagerUser = new User(
                new UserId(2L),
                "rm_user",
                "hash",
                new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực"),
                UserStatus.ACTIVE,
                null,
                DataScope.ORGANIZATION_BRANCH,
                10L,
                1L
        );

        testOrgUnit = mock(OrgUnit.class);
        when(testOrgUnit.getUnitName()).thenReturn("Phòng Công nghệ");
    }

    private ResourceScenario createMockScenario(Long id, String code, String name) {
        return new ResourceScenario(
                id,
                code,
                name,
                "Mô tả kịch bản " + id,
                10L,
                ScenarioStatus.DRAFT,
                2026,
                38,
                8,
                LocalDateTime.now(),
                1L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0L
        );
    }

    private ScenarioSimulationResult createMockSimulationResult(Long scenarioId, String code, String name, int overloadedCount, BigDecimal shortfallHours) {
        List<WeeklySimulationMetricResult> weeklyMetrics = List.of(
                new WeeklySimulationMetricResult(
                        2026, 38, "T38 (14/09 - 20/09)",
                        new BigDecimal("120.00"), new BigDecimal("40.00"), new BigDecimal("160.00"),
                        new BigDecimal("150.00"), BigDecimal.ZERO, shortfallHours,
                        new BigDecimal("106.7"), CapacityStatus.OVERLOADED, true
                ),
                new WeeklySimulationMetricResult(
                        2026, 39, "T39 (21/09 - 27/09)",
                        new BigDecimal("100.00"), new BigDecimal("30.00"), new BigDecimal("130.00"),
                        new BigDecimal("150.00"), new BigDecimal("20.00"), BigDecimal.ZERO,
                        new BigDecimal("86.7"), CapacityStatus.OPTIMAL, false
                )
        );

        List<OverloadedEmployeeResult> overloadedEmployees = overloadedCount > 0 ? List.of(
                new OverloadedEmployeeResult(
                        101L, "EMP-101", "Nguyễn Văn A", "Backend Dev",
                        2026, 38, "T38 (14/09 - 20/09)",
                        new BigDecimal("48.00"), new BigDecimal("40.00"), shortfallHours,
                        new BigDecimal("120.0"), CapacityStatus.OVERLOADED
                )
        ) : List.of();

        return new ScenarioSimulationResult(
                scenarioId, code, name, 10L, "Phòng Công nghệ",
                "DRAFT", LocalDateTime.now(),
                weeklyMetrics, overloadedEmployees, List.of(),
                new BigDecimal("100.0"), new BigDecimal("70.0")
        );
    }

    @Test
    @DisplayName("TC-01: So sánh thành công 3 kịch bản đã chạy mô phỏng")
    void shouldCompareThreeScenariosSuccessfully_TC01() {
        // Given: Đã có 3 kịch bản đã chạy
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_COMPARE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(executiveUser));
        when(loadOrgUnitPort.findById(any())).thenReturn(Optional.of(testOrgUnit));

        ResourceScenario s1 = createMockScenario(1L, "SCN-01", "Phương án 1");
        ResourceScenario s2 = createMockScenario(2L, "SCN-02", "Phương án 2");
        ResourceScenario s3 = createMockScenario(3L, "SCN-03", "Phương án 3");

        when(loadScenarioPort.findById(1L)).thenReturn(Optional.of(s1));
        when(loadScenarioPort.findById(2L)).thenReturn(Optional.of(s2));
        when(loadScenarioPort.findById(3L)).thenReturn(Optional.of(s3));

        when(simulationResultUseCase.getSimulationResult(1L)).thenReturn(createMockSimulationResult(1L, "SCN-01", "Phương án 1", 1, new BigDecimal("10.00")));
        when(simulationResultUseCase.getSimulationResult(2L)).thenReturn(createMockSimulationResult(2L, "SCN-02", "Phương án 2", 0, BigDecimal.ZERO));
        when(simulationResultUseCase.getSimulationResult(3L)).thenReturn(createMockSimulationResult(3L, "SCN-03", "Phương án 3", 1, new BigDecimal("25.00")));

        // When: Chọn 3 kịch bản để so sánh
        CompareScenariosCommand command = new CompareScenariosCommand(List.of(1L, 2L, 3L));
        ScenarioComparisonResult result = service.compareScenarios(command);

        // Then: Hệ thống hiện bảng 3 cột với số người quá tải và giờ thiếu của từng kịch bản
        assertNotNull(result);
        assertEquals(3, result.scenarios().size());

        ScenarioComparisonItemResult item1 = result.scenarios().get(0);
        assertEquals(1L, item1.scenarioId());
        assertEquals("SCN-01", item1.scenarioCode());
        assertEquals(1, item1.overloadedEmployeesCount());
        assertEquals(new BigDecimal("10.00"), item1.totalShortfallHours());
        assertEquals(new BigDecimal("10.00"), item1.totalRequiredAdditionalHours());
        assertEquals(new BigDecimal("70.00"), item1.totalDemandHours());
        assertEquals(new BigDecimal("290.00"), item1.totalWorkloadHours());
        assertEquals(new BigDecimal("300.00"), item1.totalAvailableHours());
        assertTrue(result.isTimeframeAligned());
        assertTrue(result.isOrgUnitAligned());
        assertFalse(item1.overloadedEmployees().isEmpty());

        ScenarioComparisonItemResult item2 = result.scenarios().get(1);
        assertEquals(2L, item2.scenarioId());
        assertEquals(0, item2.overloadedEmployeesCount());
        assertEquals(BigDecimal.ZERO, item2.totalShortfallHours());

        ScenarioComparisonItemResult item3 = result.scenarios().get(2);
        assertEquals(3L, item3.scenarioId());
        assertEquals(1, item3.overloadedEmployeesCount());
        assertEquals(new BigDecimal("25.00"), item3.totalShortfallHours());

        // TC-04: Hệ thống ghi lại người thực hiện, nội dung và thời điểm
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort, times(1)).save(auditCaptor.capture());
        AuditLog logged = auditCaptor.getValue();
        assertEquals(1L, logged.getUserId());
        assertEquals("COMPARE_SCENARIOS", logged.getAction());
        assertEquals("resource_scenarios", logged.getTableName());
        assertTrue(logged.getNewValue().contains("scenarioIds=[1, 2, 3]"));
    }

    @Test
    @DisplayName("TC-02: Báo lỗi khi cung cấp ít hơn hai kịch bản để so sánh")
    void shouldThrowExceptionWhenLessThanTwoScenariosProvided_TC02() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_COMPARE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(executiveUser));

        // Case null command
        assertThrows(InsufficientScenariosForComparisonException.class, () -> service.compareScenarios(null));

        // Case null list
        assertThrows(InsufficientScenariosForComparisonException.class, () -> service.compareScenarios(new CompareScenariosCommand(null)));

        // Case empty list
        assertThrows(InsufficientScenariosForComparisonException.class, () -> service.compareScenarios(new CompareScenariosCommand(List.of())));

        // Case 1 scenario
        assertThrows(InsufficientScenariosForComparisonException.class, () -> service.compareScenarios(new CompareScenariosCommand(List.of(1L))));

        // Case duplicate ID resulting in only 1 unique scenario
        assertThrows(InsufficientScenariosForComparisonException.class, () -> service.compareScenarios(new CompareScenariosCommand(List.of(1L, 1L))));
    }

    @Test
    @DisplayName("Báo lỗi IllegalArgumentException khi cung cấp hơn 10 kịch bản để so sánh (DOS protection)")
    void shouldThrowExceptionWhenMoreThanTenScenariosProvided() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_COMPARE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(executiveUser));

        List<Long> elevenScenarios = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
        assertThrows(IllegalArgumentException.class, () -> service.compareScenarios(new CompareScenariosCommand(elevenScenarios)));
    }

    @Test
    @DisplayName("TC-03: Từ chối truy cập và ghi nhật ký khi người dùng không thuộc vai trò Ban giám đốc")
    void shouldDenyAccessAndLogAuditWhenUserIsNotExecutive_TC03() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_COMPARE)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(resourceManagerUser));

        CompareScenariosCommand command = new CompareScenariosCommand(List.of(1L, 2L));

        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class, () -> service.compareScenarios(command));
        assertTrue(ex.getMessage().contains("RESOURCE_SCENARIO_COMPARE"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort, times(1)).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(2L, log.getUserId());
        assertEquals("PERMISSION_DENIED", log.getAction());
        assertTrue(log.getNewValue().contains("ONLY_EXECUTIVE_ROLE_ALLOWED"));
    }

    @Test
    @DisplayName("Báo lỗi ScenarioNotFoundException khi một trong các kịch bản không tồn tại")
    void shouldThrowScenarioNotFoundExceptionWhenScenarioMissing() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_COMPARE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(executiveUser));
        when(loadScenarioPort.findById(1L)).thenReturn(Optional.of(createMockScenario(1L, "SCN-01", "Phương án 1")));
        when(loadScenarioPort.findById(999L)).thenReturn(Optional.empty());

        CompareScenariosCommand command = new CompareScenariosCommand(List.of(1L, 999L));
        assertThrows(ScenarioNotFoundException.class, () -> service.compareScenarios(command));
    }

    @Test
    @DisplayName("QTN-14: Thao tác so sánh hoàn toàn không làm thay đổi dữ liệu phân bổ thật")
    void shouldVerifyQTN14Compliance() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_COMPARE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(executiveUser));
        when(loadOrgUnitPort.findById(any())).thenReturn(Optional.of(testOrgUnit));

        when(loadScenarioPort.findById(1L)).thenReturn(Optional.of(createMockScenario(1L, "SCN-01", "Phương án 1")));
        when(loadScenarioPort.findById(2L)).thenReturn(Optional.of(createMockScenario(2L, "SCN-02", "Phương án 2")));

        when(simulationResultUseCase.getSimulationResult(1L)).thenReturn(createMockSimulationResult(1L, "SCN-01", "Phương án 1", 0, BigDecimal.ZERO));
        when(simulationResultUseCase.getSimulationResult(2L)).thenReturn(createMockSimulationResult(2L, "SCN-02", "Phương án 2", 0, BigDecimal.ZERO));

        CompareScenariosCommand command = new CompareScenariosCommand(List.of(1L, 2L));
        service.compareScenarios(command);

        // Đảm bảo không có bất kỳ tương tác ghi/xóa nào ngoài saveAuditLogPort
        verifyNoInteractions(mock(com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort.class));
    }
}