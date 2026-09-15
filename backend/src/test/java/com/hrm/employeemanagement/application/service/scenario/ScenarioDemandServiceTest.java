package com.hrm.employeemanagement.application.service.scenario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.dto.scenario.AddScenarioDemandCommand;
import com.hrm.employeemanagement.application.dto.scenario.ScenarioDemandResult;
import com.hrm.employeemanagement.application.dto.scenario.UpdateScenarioDemandCommand;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.DeleteScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioDemandNotFoundException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioDemand;
import com.hrm.employeemanagement.domain.scenario.ScenarioStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ScenarioDemandServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadResourceScenarioPort loadScenarioPort;
    private SaveScenarioDemandPort saveDemandPort;
    private LoadScenarioDemandPort loadDemandPort;
    private DeleteScenarioDemandPort deleteDemandPort;
    private SaveAuditLogPort saveAuditLogPort;

    private ScenarioDemandService service;

    private User vt03User;
    private User vt01User;
    private ResourceScenario draftScenario;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        loadScenarioPort = mock(LoadResourceScenarioPort.class);
        saveDemandPort = mock(SaveScenarioDemandPort.class);
        loadDemandPort = mock(LoadScenarioDemandPort.class);
        deleteDemandPort = mock(DeleteScenarioDemandPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new ScenarioDemandService(
                authorizationService,
                loadUserPort,
                loadOrgUnitPort,
                loadScenarioPort,
                saveDemandPort,
                loadDemandPort,
                deleteDemandPort,
                saveAuditLogPort
        );

        vt03User = new User(
                new UserId(103L),
                "vt03_user",
                "hash",
                new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực"),
                UserStatus.ACTIVE,
                null,
                DataScope.ORGANIZATION_BRANCH,
                10L,
                1L
        );

        vt01User = new User(
                new UserId(101L),
                "vt01_user",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Ban giám đốc"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L
        );

        draftScenario = ResourceScenario.createNew(
                "SCN-01", "Kịch bản 1", "Mô tả", 10L, 2026, 38, 8, 103L
        );
        draftScenario.setId(1L);

        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(103L);
        when(loadUserPort.findById(new UserId(103L))).thenReturn(Optional.of(vt03User));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(loadScenarioPort.findById(1L)).thenReturn(Optional.of(draftScenario));
    }

    @Test
    @DisplayName("AC-01 & AC-04: VT-03 thêm nhu cầu giả định thành công, ghi audit log")
    void testAddDemand_Success() {
        when(saveDemandPort.save(any(ScenarioDemand.class))).thenAnswer(invocation -> {
            ScenarioDemand d = invocation.getArgument(0);
            d.setId(10L);
            return d;
        });

        AddScenarioDemandCommand command = new AddScenarioDemandCommand(
                1L, "Java Dev Senior", 2, 2026, 38, 2026, 42, BigDecimal.valueOf(40), "Java Spring"
        );

        ScenarioDemandResult result = service.addDemand(command);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Java Dev Senior", result.demandName());
        assertEquals(2, result.headcount());
        assertEquals(2026, result.startYear());
        assertEquals(38, result.startWeek());
        assertEquals(2026, result.endYear());
        assertEquals(42, result.endWeek());
        assertEquals(BigDecimal.valueOf(80), result.totalHoursPerWeek());

        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "ADD_SCENARIO_DEMAND".equals(log.getAction()) &&
                "scenario_demands".equals(log.getTableName()) &&
                Long.valueOf(10L).equals(log.getRecordId())
        ));
    }

    @Test
    @DisplayName("AC-04: VT-03 sửa nhu cầu giả định thành công, ghi audit log với old và new value")
    void testUpdateDemand_Success() {
        ScenarioDemand existing = ScenarioDemand.create(
                1L, "Cũ", 1, 2026, 38, 2026, 40, BigDecimal.valueOf(20), "Java"
        );
        existing.setId(10L);
        when(loadDemandPort.findById(10L)).thenReturn(Optional.of(existing));
        when(saveDemandPort.save(any(ScenarioDemand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateScenarioDemandCommand command = new UpdateScenarioDemandCommand(
                1L, 10L, "Mới", 2, 2026, 38, 2026, 42, BigDecimal.valueOf(40), "Java"
        );

        ScenarioDemandResult result = service.updateDemand(command);

        assertNotNull(result);
        assertEquals("Mới", result.demandName());
        assertEquals(2, result.headcount());
        assertEquals(BigDecimal.valueOf(80), result.totalHoursPerWeek());

        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "UPDATE_SCENARIO_DEMAND".equals(log.getAction()) &&
                log.getOldValue().contains("demandName=Cũ") &&
                log.getNewValue().contains("demandName=Mới")
        ));
    }

    @Test
    @DisplayName("AC-04: VT-03 xóa nhu cầu giả định thành công, ghi audit log")
    void testDeleteDemand_Success() {
        ScenarioDemand existing = ScenarioDemand.create(
                1L, "Cần xóa", 2, 2026, 38, 2026, 40, BigDecimal.valueOf(40), "Python"
        );
        existing.setId(10L);
        when(loadDemandPort.findById(10L)).thenReturn(Optional.of(existing));

        service.deleteDemand(1L, 10L);

        verify(deleteDemandPort, times(1)).deleteById(10L);
        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "DELETE_SCENARIO_DEMAND".equals(log.getAction()) &&
                log.getOldValue().contains("demandName=Cần xóa")
        ));
    }

    @Test
    @DisplayName("VT-01 cố thêm nhu cầu giả định -> Bị từ chối PermissionDeniedException")
    void testAddDemand_VT01_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(101L);
        when(loadUserPort.findById(new UserId(101L))).thenReturn(Optional.of(vt01User));

        AddScenarioDemandCommand command = new AddScenarioDemandCommand(
                1L, "Java Dev", 1, 2026, 38, 2026, 40, BigDecimal.valueOf(40), "Java"
        );

        assertThrows(PermissionDeniedException.class, () -> service.addDemand(command));

        verify(saveAuditLogPort, times(1)).save(argThat(log ->
                "ACCESS_DENIED_DEMAND_ADD".equals(log.getAction())
        ));
    }

    @Test
    @DisplayName("Không cho thêm nhu cầu nếu kịch bản đã applied / discarded")
    void testAddDemand_ScenarioNotDraft_ThrowsException() {
        draftScenario.setStatus(ScenarioStatus.APPLIED);

        AddScenarioDemandCommand command = new AddScenarioDemandCommand(
                1L, "Java Dev", 1, 2026, 38, 2026, 40, BigDecimal.valueOf(40), "Java"
        );

        assertThrows(ScenarioNotModifiableException.class, () -> service.addDemand(command));
    }

    @Test
    @DisplayName("Nhu cầu nằm ngoài phạm vi kịch bản ném InvalidScenarioDemandException")
    void testAddDemand_OutsideScenarioBounds_ThrowsException() {
        AddScenarioDemandCommand command = new AddScenarioDemandCommand(
                1L, "Java Dev", 1, 2026, 30, 2026, 35, BigDecimal.valueOf(40), "Java"
        );

        assertThrows(com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException.class,
                () -> service.addDemand(command));
    }
}
