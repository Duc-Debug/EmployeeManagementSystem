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

import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateEmployeeSkillCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateEmployeeSkillService Unit Tests")
class UpdateEmployeeSkillServiceTest {

    @Mock
    private EmployeeSkillRepository employeeSkillRepository;

    @Mock
    private SkillCatalogRepository skillCatalogRepository;

    @Mock
    private SaveAuditLogInNewTransactionPort auditLogRepository;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private AuthorizationService authorizationService;

    private UpdateEmployeeSkillService service;

    @BeforeEach
    void setUp() {
        service = new UpdateEmployeeSkillService(
                employeeSkillRepository,
                skillCatalogRepository,
                auditLogRepository,
                loadEmployeePort,
                authorizationService
        );
    }

    @Test
    @DisplayName("Cập nhật kỹ năng cá nhân thành công và tự động chuyển về PENDING chờ duyệt lại")
    void shouldUpdateEmployeeSkillSuccessfully() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE)).thenReturn(99L);

        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(10L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.of(employee));

        // Ban đầu skill ở trạng thái APPROVED
        EmployeeSkill existingSkill = new EmployeeSkill(
                1L, 10L, 100L, ProficiencyLevel.fromValue(2), new BigDecimal("1.0"),
                SkillStatus.APPROVED, 88L, LocalDateTime.now(), null, "Notes", LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeIdAndSkillId(10L, 100L)).thenReturn(Optional.of(existingSkill));
        when(employeeSkillRepository.save(any(EmployeeSkill.class))).thenAnswer(i -> i.getArgument(0));

        Skill skill = new Skill(100L, "JAVA", "Java", "Backend", "Desc", 3L, LocalDateTime.now());
        when(skillCatalogRepository.findById(100L)).thenReturn(Optional.of(skill));

        UpdateEmployeeSkillCommand command = new UpdateEmployeeSkillCommand(10L, 100L, 4, new BigDecimal("3.0"));

        EmployeeSkillResult result = service.execute(command);

        assertNotNull(result);
        assertEquals(4, result.proficiencyLevel());
        assertEquals("PENDING", result.status()); // Chuyển từ APPROVED -> PENDING theo quy tắc nghiệp vụ

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertEquals("UPDATE_SKILL", auditCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("Ném EmployeeSkillNotFoundException khi kỹ năng không có trong hồ sơ của nhân viên")
    void shouldThrowEmployeeSkillNotFoundExceptionWhenSkillNotInProfile() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE)).thenReturn(99L);

        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(10L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.of(employee));

        when(employeeSkillRepository.findByEmployeeIdAndSkillId(10L, 999L)).thenReturn(Optional.empty());

        UpdateEmployeeSkillCommand command = new UpdateEmployeeSkillCommand(10L, 999L, 4, new BigDecimal("3.0"));

        assertThrows(EmployeeSkillNotFoundException.class, () -> service.execute(command));
        verify(employeeSkillRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném PermissionDeniedException khi cập nhật kỹ năng của nhân viên khác")
    void shouldThrowPermissionDeniedWhenUpdatingOtherEmployeeSkill() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE)).thenReturn(99L);

        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(10L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.of(employee));

        // Command yêu cầu sửa cho employeeId = 20L khác với current employee 10L
        UpdateEmployeeSkillCommand command = new UpdateEmployeeSkillCommand(20L, 100L, 4, new BigDecimal("3.0"));

        assertThrows(PermissionDeniedException.class, () -> service.execute(command));
        verify(employeeSkillRepository, never()).save(any());
    }
}
