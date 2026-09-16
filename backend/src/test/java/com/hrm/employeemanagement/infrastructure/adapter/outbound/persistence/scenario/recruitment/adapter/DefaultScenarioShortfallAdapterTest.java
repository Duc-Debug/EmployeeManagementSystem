package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.recruitment.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.scenario.recruitment.RoleShortfallDemand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultScenarioShortfallAdapter Unit Tests")
class DefaultScenarioShortfallAdapterTest {

    @Mock
    private LoadProjectRolePort loadProjectRolePort;

    @Mock
    private LoadScenarioDemandPort loadScenarioDemandPort;

    private DefaultScenarioShortfallAdapter adapter;

    private final ProjectRole devRole = new ProjectRole(new ProjectRoleId(1L), "DEV", "Developer", "Dev role");
    private final ProjectRole testerRole = new ProjectRole(new ProjectRoleId(2L), "TESTER", "Tester", "Tester role");

    @BeforeEach
    void setUp() {
        adapter = new DefaultScenarioShortfallAdapter(loadProjectRolePort, loadScenarioDemandPort);
    }

    @Test
    @DisplayName("Không có active roles -> Trả về danh sách rỗng")
    void loadShortfallDemands_NoRoles_ReturnsEmpty() {
        when(loadProjectRolePort.findAllActive()).thenReturn(List.of());

        List<RoleShortfallDemand> result = adapter.loadShortfallDemands(100L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Có ScenarioDemands cho scenarioId -> Tính đúng số giờ thiếu hụt (headcount * hours * weeks)")
    void loadShortfallDemands_WithDemands_CalculatesHoursCorrectly() {
        when(loadProjectRolePort.findAllActive()).thenReturn(List.of(devRole, testerRole));

        // Demand 1: DEV role, 2 người x 40h/tuần x 2 tuần (tuần 10 đến 11 năm 2026 = 2 tuần) = 160h
        ScenarioDemand devDemand = new ScenarioDemand(
                1L,
                100L,
                "DEV Senior",
                2,
                2026, 10,
                2026, 11,
                new BigDecimal("40.00"),
                "Java",
                LocalDateTime.now(),
                null
        );

        // Demand 2: TESTER role, 1 người x 20h/tuần x 4 tuần (tuần 1 đến 4 năm 2026 = 4 tuần) = 80h
        ScenarioDemand testerDemand = new ScenarioDemand(
                2L,
                100L,
                "TESTER Automation",
                1,
                2026, 1,
                2026, 4,
                new BigDecimal("20.00"),
                "Selenium",
                LocalDateTime.now(),
                null
        );

        when(loadScenarioDemandPort.findByScenarioId(100L)).thenReturn(List.of(devDemand, testerDemand));

        List<RoleShortfallDemand> result = adapter.loadShortfallDemands(100L);

        assertNotNull(result);
        assertEquals(2, result.size());

        RoleShortfallDemand devResult = result.stream().filter(r -> r.roleId().equals(1L)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("160.00").compareTo(devResult.shortfallHours()));

        RoleShortfallDemand testerResult = result.stream().filter(r -> r.roleId().equals(2L)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("80.00").compareTo(testerResult.shortfallHours()));
    }

    @Test
    @DisplayName("Không có ScenarioDemand hoặc loadScenarioDemandPort trả về rỗng -> Trả về shortfall = 0 cho mọi vai trò")
    void loadShortfallDemands_NoDemands_ReturnsZeroShortfall() {
        when(loadProjectRolePort.findAllActive()).thenReturn(List.of(devRole, testerRole));
        when(loadScenarioDemandPort.findByScenarioId(100L)).thenReturn(List.of());

        List<RoleShortfallDemand> result = adapter.loadShortfallDemands(100L);

        assertNotNull(result);
        assertEquals(2, result.size());
        for (RoleShortfallDemand demand : result) {
            assertEquals(0, BigDecimal.ZERO.compareTo(demand.shortfallHours()));
        }
    }

    @Test
    @DisplayName("Adapter với LoadScenarioDemandPort là null -> Hoạt động an toàn và trả về shortfall = 0")
    void loadShortfallDemands_NullDemandPort_HandlesGracefully() {
        DefaultScenarioShortfallAdapter nullPortAdapter = new DefaultScenarioShortfallAdapter(loadProjectRolePort, null);
        when(loadProjectRolePort.findAllActive()).thenReturn(List.of(devRole));

        List<RoleShortfallDemand> result = nullPortAdapter.loadShortfallDemands(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getFirst().shortfallHours()));
    }

    @Test
    @DisplayName("Nhu cầu không khớp với bất kỳ vai trò nào (Unmatched Demand) -> Bỏ qua, không fallback vào vai trò khác và không làm sai lệch số giờ thiếu")
    void loadShortfallDemands_UnmatchedDemand_ShouldNotFallbackToFirstRole() {
        when(loadProjectRolePort.findAllActive()).thenReturn(List.of(devRole, testerRole));

        // Demand 1: DEV role (160h)
        ScenarioDemand devDemand = new ScenarioDemand(
                1L, 100L, "DEV Senior", 2, 2026, 10, 2026, 11,
                new BigDecimal("40.00"), "Java", LocalDateTime.now(), null
        );

        // Demand 2: Business Analyst (không tồn tại trong active roles DEV, TESTER)
        ScenarioDemand baDemand = new ScenarioDemand(
                2L, 100L, "Business Analyst", 1, 2026, 1, 2026, 2,
                new BigDecimal("40.00"), "BPMN", LocalDateTime.now(), null
        );

        when(loadScenarioDemandPort.findByScenarioId(100L)).thenReturn(List.of(devDemand, baDemand));

        List<RoleShortfallDemand> result = adapter.loadShortfallDemands(100L);

        assertNotNull(result);
        assertEquals(2, result.size());

        // DEV vẫn chỉ có 160h, không bị cộng dồn thêm 80h của BA
        RoleShortfallDemand devResult = result.stream().filter(r -> r.roleId().equals(1L)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("160.00").compareTo(devResult.shortfallHours()), "DEV role không được nhận giờ của BA do fallback");

        // TESTER là 0h
        RoleShortfallDemand testerResult = result.stream().filter(r -> r.roleId().equals(2L)).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(testerResult.shortfallHours()));
    }
}
