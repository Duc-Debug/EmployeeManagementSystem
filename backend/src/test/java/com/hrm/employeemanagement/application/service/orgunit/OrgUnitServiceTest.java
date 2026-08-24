package com.hrm.employeemanagement.application.service.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.*;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.SaveOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.orgunit.*;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgUnitServiceTest {

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private SaveOrgUnitPort saveOrgUnitPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private CurrentUserPort currentUserPort;

    @InjectMocks
    private OrgUnitService orgUnitService;

    @Test
    @DisplayName("Should create org unit successfully when parameters and manager are valid")
    void shouldCreateOrgUnitSuccessfully() {
        // DEV-CENTER is created as a child of COMPANY_ROOT (id: 1) with manager ID: 10
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, 1L, 10L, "Mô tả"
        );

        OrgUnit rootCompany = new OrgUnit(
                new OrgUnitId(1L), "COMPANY_ROOT", "Công Ty Cổ Phần Software", OrgUnitType.COMPANY,
                null, "/1/", 1, OrgUnitStatus.ACTIVE, "Nút gốc", 1L, LocalDateTime.now(), null
        );

        Employee activeManager = new Employee(
                new EmployeeId(10L), new UserId(10L), 1L, "EMP010", "Manager Name", false, 40, EmployeeStatus.ACTIVE
        );

        when(loadOrgUnitPort.existsByUnitCode("DEV-CENTER")).thenReturn(false);
        when(loadEmployeePort.findById(new EmployeeId(10L))).thenReturn(Optional.of(activeManager));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(rootCompany));

        OrgUnit savedUnit = new OrgUnit(
                new OrgUnitId(2L), "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenReturn(savedUnit);

        OrgUnitResult result = orgUnitService.execute(command);

        // Verify business behavior
        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("DEV-CENTER", result.unitCode());
        assertEquals("Khối Phát Triển", result.unitName());
        assertEquals(OrgUnitType.CENTER, result.unitType());
        assertEquals(1L, result.parentId());
        assertEquals(10L, result.managerId());
        assertEquals("/1/2/", result.treePath());
        assertEquals(2, result.level());
        assertEquals(OrgUnitStatus.ACTIVE, result.status());

        // Verify save() and audit log save() are called
        verify(saveOrgUnitPort).save(any(OrgUnit.class));
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateUnitCodeException when unit code already exists")
    void shouldThrowExceptionWhenUnitCodeExists() {
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, 1L, 10L, "Mô tả"
        );

        when(loadOrgUnitPort.existsByUnitCode("DEV-CENTER")).thenReturn(true);

        assertThrows(DuplicateUnitCodeException.class, () -> orgUnitService.execute(command));
        verify(saveOrgUnitPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when manager does not exist")
    void shouldThrowEmployeeNotFoundExceptionWhenManagerDoesNotExist() {
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, 1L, 9999L, "Mô tả"
        );

        when(loadOrgUnitPort.existsByUnitCode("DEV-CENTER")).thenReturn(false);
        when(loadEmployeePort.findById(new EmployeeId(9999L))).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> orgUnitService.execute(command));
        verify(saveOrgUnitPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidOrgUnitManagerException when manager is inactive")
    void shouldThrowInvalidOrgUnitManagerExceptionWhenManagerIsInactive() {
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, 1L, 10L, "Mô tả"
        );

        Employee inactiveManager = new Employee(
                new EmployeeId(10L), new UserId(10L), 1L, "EMP010", "Manager Name", false, 40, EmployeeStatus.TERMINATED
        );

        when(loadOrgUnitPort.existsByUnitCode("DEV-CENTER")).thenReturn(false);
        when(loadEmployeePort.findById(new EmployeeId(10L))).thenReturn(Optional.of(inactiveManager));

        assertThrows(InvalidOrgUnitManagerException.class, () -> orgUnitService.execute(command));
        verify(saveOrgUnitPort, never()).save(any());
    }

    @Test
    @DisplayName("Should move org unit and invoke bulk updateSubTreePaths")
    void shouldMoveOrgUnitSuccessfully() {
        MoveOrgUnitCommand command = new MoveOrgUnitCommand(2L, 3L);

        OrgUnit unitToMove = new OrgUnit(
                new OrgUnitId(2L), "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        OrgUnit newParent = new OrgUnit(
                new OrgUnitId(3L), "TECH-DEPT", "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/3/", 2, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(2L))).thenReturn(Optional.of(unitToMove));
        when(loadOrgUnitPort.findById(new OrgUnitId(3L))).thenReturn(Optional.of(newParent));
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenReturn(unitToMove);

        OrgUnitResult result = orgUnitService.execute(command);

        assertNotNull(result);
        verify(saveOrgUnitPort).updateSubTreePaths("/1/2/", "/1/3/2/", 1);
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("Should deactivate org unit and invoke bulk deactivateSubTree")
    void shouldDeactivateOrgUnitSuccessfully() {
        DeactivateOrgUnitCommand command = new DeactivateOrgUnitCommand(2L);

        OrgUnit unit = new OrgUnit(
                new OrgUnitId(2L), "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(2L))).thenReturn(Optional.of(unit));
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenReturn(unit);

        OrgUnitResult result = orgUnitService.execute(command);

        assertNotNull(result);
        verify(saveOrgUnitPort).deactivateSubTree("/1/2/");
        verify(saveAuditLogPort).save(any());
    }
}