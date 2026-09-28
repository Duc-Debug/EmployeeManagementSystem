package com.hrm.employeemanagement.application.service.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.skill.DeleteEmployeeSkillCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteEmployeeSkillService Unit Tests")
class DeleteEmployeeSkillServiceTest {

    @Mock
    private EmployeeSkillRepository employeeSkillRepository;

    @Mock
    private SaveAuditLogInNewTransactionPort auditLogRepository;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private AuthorizationService authorizationService;

    private DeleteEmployeeSkillService service;

    @BeforeEach
    void setUp() {
        service = new DeleteEmployeeSkillService(
                employeeSkillRepository,
                auditLogRepository,
                loadEmployeePort,
                authorizationService
        );
    }

    @Test
    @DisplayName("Xóa kỹ năng cá nhân thành công khi tồn tại và đúng quyền sở hữu")
    void shouldDeleteEmployeeSkillSuccessfully() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE)).thenReturn(99L);

        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(10L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.of(employee));

        EmployeeSkill existingSkill = new EmployeeSkill(
                1L, 10L, 100L, ProficiencyLevel.fromValue(3), new BigDecimal("2.0"),
                SkillStatus.APPROVED, 88L, LocalDateTime.now(), null, "Notes", LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeIdAndSkillId(10L, 100L)).thenReturn(Optional.of(existingSkill));

        DeleteEmployeeSkillCommand command = new DeleteEmployeeSkillCommand(10L, 100L);

        service.execute(command);

        verify(employeeSkillRepository).deleteByEmployeeIdAndSkillId(10L, 100L);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals("DELETE_SKILL", auditCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("Ném EmployeeSkillNotFoundException và không thực hiện xóa khi kỹ năng không tồn tại trong hồ sơ")
    void shouldThrowEmployeeSkillNotFoundExceptionWhenSkillNotExist() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE)).thenReturn(99L);

        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(10L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.of(employee));

        when(employeeSkillRepository.findByEmployeeIdAndSkillId(10L, 999L)).thenReturn(Optional.empty());

        DeleteEmployeeSkillCommand command = new DeleteEmployeeSkillCommand(10L, 999L);

        assertThrows(EmployeeSkillNotFoundException.class, () -> service.execute(command));
        verify(employeeSkillRepository, never()).deleteByEmployeeIdAndSkillId(any(), any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném PermissionDeniedException khi xóa kỹ năng của nhân viên khác")
    void shouldThrowPermissionDeniedWhenDeletingOtherEmployeeSkill() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE)).thenReturn(99L);

        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(10L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.of(employee));

        DeleteEmployeeSkillCommand command = new DeleteEmployeeSkillCommand(20L, 100L);

        assertThrows(PermissionDeniedException.class, () -> service.execute(command));
        verify(employeeSkillRepository, never()).deleteByEmployeeIdAndSkillId(any(), any());
    }
}
