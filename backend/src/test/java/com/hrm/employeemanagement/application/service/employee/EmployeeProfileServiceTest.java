package com.hrm.employeemanagement.application.service.employee;

import com.hrm.employeemanagement.application.dto.employee.CreateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;
import com.hrm.employeemanagement.application.dto.employee.UpdateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.employee.InvalidEmployeeDataException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeProfileServiceTest {

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private SaveEmployeePort saveEmployeePort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private User currentUser;

    @Mock
    private OrgUnit activeOrgUnit;

    private EmployeeProfileService employeeProfileService;

    @BeforeEach
    void setUp() {
        employeeProfileService = new EmployeeProfileService(loadEmployeePort, saveEmployeePort,
                loadUserPort, loadOrgUnitPort, authorizationService);
        lenient().when(authorizationService.require(any())).thenReturn(1L);
        lenient().when(loadUserPort.findById(any(UserId.class))).thenReturn(Optional.of(currentUser));
        lenient().when(currentUser.getIdValue()).thenReturn(1L);
        lenient().when(currentUser.getDataScope()).thenReturn(DataScope.COMPANY);
        lenient().when(loadOrgUnitPort.findById(any())).thenReturn(Optional.of(activeOrgUnit));
        lenient().when(activeOrgUnit.getStatus()).thenReturn(OrgUnitStatus.ACTIVE);
    }

    @Test
    @DisplayName("Tạo mới hồ sơ nhân sự thành công")
    void createProfile_Success() {
        CreateEmployeeProfileCommand command = new CreateEmployeeProfileCommand(
                10L, 1L, "EMP001", "Nguyễn Văn A", "Backend Developer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 40
        );

        when(loadEmployeePort.findByUserId(any(UserId.class))).thenReturn(Optional.empty());
        when(saveEmployeePort.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            return new Employee(
                    new EmployeeId(100L), emp.getUserId(), emp.getOrgUnitId(),
                    emp.getEmployeeCode(), emp.getFullName(), emp.getProfessionalRole(),
                    emp.getStartDate(), emp.getContractEndDate(), emp.getIsOutsourced(),
                    emp.getStandardHoursPerWeek(), emp.getStatus()
            );
        });

        EmployeeProfileResult result = employeeProfileService.execute(command);

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals("EMP001", result.employeeCode());
        assertEquals(40, result.standardHoursPerWeek());
        verify(saveEmployeePort, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("Tạo mới hồ sơ thất bại khi user_id đã được liên kết với nhân sự khác")
    void createProfile_DuplicateUser_ThrowsException() {
        CreateEmployeeProfileCommand command = new CreateEmployeeProfileCommand(
                10L, 1L, "EMP001", "Nguyễn Văn A", "Backend Developer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 40
        );

        Employee existing = Employee.createNew(new UserId(10L), 1L, "EMP999", "Đã Tồn Tại");
        when(loadEmployeePort.findByUserId(any(UserId.class))).thenReturn(Optional.of(existing));

        assertThrows(InvalidEmployeeDataException.class, () -> employeeProfileService.execute(command));
        verify(saveEmployeePort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật hồ sơ nhân sự thành công")
    void updateProfile_Success() {
        UpdateEmployeeProfileCommand command = new UpdateEmployeeProfileCommand(
                100L, 2L, "Nguyễn Văn B", "Senior Developer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1), 44
        );

        Employee existing = Employee.createNew(new UserId(10L), 1L, "EMP001", "Nguyễn Văn A");
        when(loadEmployeePort.findById(new EmployeeId(100L))).thenReturn(Optional.of(existing));
        when(saveEmployeePort.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeProfileResult result = employeeProfileService.execute(command);

        assertNotNull(result);
        assertEquals("Nguyễn Văn B", result.fullName());
        assertEquals("Senior Developer", result.professionalRole());
        assertEquals(44, result.standardHoursPerWeek());
    }

    @Test
    @DisplayName("Cập nhật hồ sơ thất bại khi không tìm thấy nhân viên")
    void updateProfile_NotFound_ThrowsException() {
        UpdateEmployeeProfileCommand command = new UpdateEmployeeProfileCommand(
                999L, 2L, "Nguyễn Văn B", "Senior Developer", null, null, 40
        );

        when(loadEmployeePort.findById(any())).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeProfileService.execute(command));
    }

    @Test
    @DisplayName("Từ chối đọc hồ sơ nằm ngoài phạm vi chi nhánh")
    void getById_OutsideOrganizationBranch_ThrowsPermissionDenied() {
        Employee employee = new Employee(
                new EmployeeId(100L), new UserId(10L), 20L, "EMP001", "Nguyễn Văn A",
                false, 40, com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE);
        when(currentUser.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(currentUser.getScopeOrgUnitId()).thenReturn(5L);
        when(loadEmployeePort.findById(new EmployeeId(100L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(20L, 5L)).thenReturn(false);

        assertThrows(PermissionDeniedException.class,
                () -> employeeProfileService.getById(100L));
        verify(saveEmployeePort, never()).save(any());
    }
}
