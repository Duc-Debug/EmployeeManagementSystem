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
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.*;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
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

    // =========================================================================
    // TC-01 đến TC-05: Hierarchical Manager Assignment (NCL-01)
    // =========================================================================

    @Test
    @DisplayName("TC-01: Gán nhân viên thuộc chính phòng ban làm Trưởng phòng -> Thành công")
    void shouldAssignManagerFromSameOrgUnitSuccessfully() {
        UpdateOrgUnitCommand command = new UpdateOrgUnitCommand(
                2L, "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT, 20L, "Mô tả cập nhật"
        );

        OrgUnit targetUnit = new OrgUnit(
                new OrgUnitId(2L), "TECH-DEPT", "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả cũ", null, LocalDateTime.now(), null
        );

        Employee manager = new Employee(
                new EmployeeId(20L), new UserId(20L), 2L, "EMP020", "Trưởng phòng nội bộ", false, 40, EmployeeStatus.ACTIVE
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(2L))).thenReturn(Optional.of(targetUnit));
        when(loadEmployeePort.findById(new EmployeeId(20L))).thenReturn(Optional.of(manager));
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrgUnitResult result = orgUnitService.execute(command);

        assertNotNull(result);
        assertEquals(20L, result.managerId());
        verify(saveOrgUnitPort).save(any(OrgUnit.class));
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("TC-02: Giám đốc ở Khối (cấp cha) kiêm nhiệm Trưởng phòng (cấp con) -> Thành công")
    void shouldAllowParentOrgUnitManagerToLeadChildUnit() {
        UpdateOrgUnitCommand command = new UpdateOrgUnitCommand(
                3L, "Nhóm AI", OrgUnitType.DEPARTMENT, 10L, "Mô tả"
        );

        OrgUnit childUnit = new OrgUnit(
                new OrgUnitId(3L), "AI-TEAM", "Nhóm AI", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/3/", 2, OrgUnitStatus.ACTIVE, "Mô tả cũ", null, LocalDateTime.now(), null
        );

        Employee directorManager = new Employee(
                new EmployeeId(10L), new UserId(10L), 1L, "EMP010", "Giám Đốc Khối", false, 40, EmployeeStatus.ACTIVE
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(3L))).thenReturn(Optional.of(childUnit));
        when(loadEmployeePort.findById(new EmployeeId(10L))).thenReturn(Optional.of(directorManager));
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrgUnitResult result = orgUnitService.execute(command);

        assertNotNull(result);
        assertEquals(10L, result.managerId());
        verify(saveOrgUnitPort).save(any(OrgUnit.class));
    }

    @Test
    @DisplayName("TC-03: Gán nhân viên phòng Kế toán làm Trưởng phòng IT (nhánh khác) -> Ném InvalidOrgUnitManagerException")
    void shouldThrowInvalidOrgUnitManagerExceptionWhenManagerFromUnrelatedBranch() {
        UpdateOrgUnitCommand command = new UpdateOrgUnitCommand(
                3L, "Phòng IT", OrgUnitType.DEPARTMENT, 99L, "Mô tả"
        );

        OrgUnit itUnit = new OrgUnit(
                new OrgUnitId(3L), "IT-DEPT", "Phòng IT", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/3/", 2, OrgUnitStatus.ACTIVE, "Mô tả cũ", null, LocalDateTime.now(), null
        );

        Employee accountant = new Employee(
                new EmployeeId(99L), new UserId(99L), 50L, "EMP099", "Kế toán viên", false, 40, EmployeeStatus.ACTIVE
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(3L))).thenReturn(Optional.of(itUnit));
        when(loadEmployeePort.findById(new EmployeeId(99L))).thenReturn(Optional.of(accountant));

        InvalidOrgUnitManagerException ex = assertThrows(
                InvalidOrgUnitManagerException.class, () -> orgUnitService.execute(command)
        );
        assertTrue(ex.getMessage().contains("phải thuộc chính đơn vị này hoặc thuộc đơn vị cấp trên"));
        verify(saveOrgUnitPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Gán nhân viên đã bị khóa (TERMINATED) -> Ném InvalidOrgUnitManagerException")
    void shouldThrowInvalidOrgUnitManagerExceptionWhenUpdatingWithInactiveManager() {
        UpdateOrgUnitCommand command = new UpdateOrgUnitCommand(
                2L, "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT, 20L, "Mô tả"
        );

        OrgUnit targetUnit = new OrgUnit(
                new OrgUnitId(2L), "TECH-DEPT", "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả cũ", null, LocalDateTime.now(), null
        );

        Employee terminatedEmployee = new Employee(
                new EmployeeId(20L), new UserId(20L), 2L, "EMP020", "Nhân viên cũ", false, 40, EmployeeStatus.TERMINATED
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(2L))).thenReturn(Optional.of(targetUnit));
        when(loadEmployeePort.findById(new EmployeeId(20L))).thenReturn(Optional.of(terminatedEmployee));

        assertThrows(InvalidOrgUnitManagerException.class, () -> orgUnitService.execute(command));
        verify(saveOrgUnitPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-05: Gán managerId = null (gỡ trưởng phòng hoặc chưa bổ nhiệm) -> Thành công")
    void shouldAllowNullManagerIdWhenUpdatingOrgUnit() {
        UpdateOrgUnitCommand command = new UpdateOrgUnitCommand(
                2L, "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT, null, "Không có trưởng phòng"
        );

        OrgUnit targetUnit = new OrgUnit(
                new OrgUnitId(2L), "TECH-DEPT", "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả cũ", 10L, LocalDateTime.now(), null
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(2L))).thenReturn(Optional.of(targetUnit));
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrgUnitResult result = orgUnitService.execute(command);

        assertNotNull(result);
        assertNull(result.managerId());
        verify(saveOrgUnitPort).save(any(OrgUnit.class));
    }

    @Test
    @DisplayName("TC-06: Chặn gán nhân viên ACTIVE nhưng chưa thuộc đơn vị nào (orgUnitId == null) làm Trưởng phòng")
    void shouldThrowInvalidOrgUnitManagerExceptionWhenActiveManagerHasNoOrgUnit() {
        UpdateOrgUnitCommand command = new UpdateOrgUnitCommand(
                2L, "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT, 25L, "Mô tả"
        );

        OrgUnit targetUnit = new OrgUnit(
                new OrgUnitId(2L), "TECH-DEPT", "Phòng Kỹ Thuật", OrgUnitType.DEPARTMENT,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả cũ", null, LocalDateTime.now(), null
        );

        // Nhân viên ACTIVE nhưng orgUnitId = null (chưa biên chế vào phòng ban nào)
        Employee unassignedEmployee = new Employee(
                new EmployeeId(25L), new UserId(25L), null, "EMP025", "Nhân viên tự do", false, 40, EmployeeStatus.ACTIVE
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(2L))).thenReturn(Optional.of(targetUnit));
        when(loadEmployeePort.findById(new EmployeeId(25L))).thenReturn(Optional.of(unassignedEmployee));

        InvalidOrgUnitManagerException ex = assertThrows(
                InvalidOrgUnitManagerException.class, () -> orgUnitService.execute(command)
        );
        assertTrue(ex.getMessage().contains("phải thuộc chính đơn vị này hoặc thuộc đơn vị cấp trên"));
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

    @Test
    @DisplayName("Should activate inactive org unit successfully when parent is active and log audit change")
    void shouldActivateOrgUnitSuccessfully() {
        ActivateOrgUnitCommand command = new ActivateOrgUnitCommand(2L);

        OrgUnit parent = new OrgUnit(
                new OrgUnitId(1L), "COMPANY_ROOT", "Công ty Software", OrgUnitType.COMPANY,
                null, "/1/", 1, OrgUnitStatus.ACTIVE, "Gốc", 1L, LocalDateTime.now(), null
        );

        OrgUnit inactiveChild = new OrgUnit(
                new OrgUnitId(2L), "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.INACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(2L))).thenReturn(Optional.of(inactiveChild));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(parent));
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrgUnitResult result = orgUnitService.execute(command);

        assertNotNull(result);
        assertEquals(OrgUnitStatus.ACTIVE, result.status());
        verify(saveOrgUnitPort).save(any(OrgUnit.class));
        verify(saveAuditLogPort).save(argThat(auditLog ->
                "ACTIVATE_ORG_UNIT".equals(auditLog.getAction())
        ));
    }

    @Test
    @DisplayName("Should throw OrgUnitNotFoundException when activating non-existing unit")
    void shouldThrowOrgUnitNotFoundExceptionWhenActivatingNonExistingUnit() {
        ActivateOrgUnitCommand command = new ActivateOrgUnitCommand(999L);

        when(loadOrgUnitPort.findById(new OrgUnitId(999L))).thenReturn(Optional.empty());

        assertThrows(OrgUnitNotFoundException.class, () -> orgUnitService.execute(command));
        verify(saveOrgUnitPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InactiveParentException when activating child while its parent is inactive")
    void shouldThrowInactiveParentExceptionWhenActivatingChildWithInactiveParent() {
        ActivateOrgUnitCommand command = new ActivateOrgUnitCommand(2L);

        OrgUnit inactiveParent = new OrgUnit(
                new OrgUnitId(1L), "COMPANY_ROOT", "Công ty Software", OrgUnitType.COMPANY,
                null, "/1/", 1, OrgUnitStatus.INACTIVE, "Gốc", 1L, LocalDateTime.now(), null
        );

        OrgUnit inactiveChild = new OrgUnit(
                new OrgUnitId(2L), "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.INACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(2L))).thenReturn(Optional.of(inactiveChild));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(inactiveParent));

        assertThrows(InactiveParentException.class, () -> orgUnitService.execute(command));
        verify(saveOrgUnitPort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Should build org tree in deterministic order matching tree path and unit code")
    void shouldBuildOrgTreeInDeterministicOrder() {
        OrgUnit root = new OrgUnit(
                new OrgUnitId(1L), "COMPANY_ROOT", "Công ty Software", OrgUnitType.COMPANY,
                null, "/1/", 1, OrgUnitStatus.ACTIVE, "Gốc", 1L, LocalDateTime.now(), null
        );
        OrgUnit childA = new OrgUnit(
                new OrgUnitId(2L), "ADMIN-CENTER", "Khối Hành Chính", OrgUnitType.CENTER,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );
        OrgUnit childB = new OrgUnit(
                new OrgUnitId(3L), "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                new OrgUnitId(1L), "/1/3/", 2, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        when(loadOrgUnitPort.findAll()).thenReturn(List.of(root, childA, childB));
        when(loadEmployeePort.findAllActive()).thenReturn(List.of());

        List<OrgUnitNodeResult> tree = orgUnitService.execute();

        assertNotNull(tree);
        assertEquals(1, tree.size());
        OrgUnitNodeResult rootNode = tree.get(0);
        assertEquals("COMPANY_ROOT", rootNode.unitCode());
        assertEquals(2, rootNode.children().size());
        assertEquals("ADMIN-CENTER", rootNode.children().get(0).unitCode());
        assertEquals("DEV-CENTER", rootNode.children().get(1).unitCode());
    }
}