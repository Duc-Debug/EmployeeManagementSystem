package com.hrm.employeemanagement.application.service.skill;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.skill.DeclareEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeclareEmployeeSkillService Unit Tests")
class DeclareEmployeeSkillServiceTest {

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

    private DeclareEmployeeSkillService service;

    @BeforeEach
    void setUp() {
        service = new DeclareEmployeeSkillService(
                employeeSkillRepository,
                skillCatalogRepository,
                auditLogRepository,
                loadEmployeePort,
                authorizationService
        );
    }

    @Test
    @DisplayName("Khai báo kỹ năng thành công khi employeeId là null (tự động lấy ID của người dùng đang đăng nhập)")
    void execute_NullEmployeeId_AutoResolvesCurrentEmployeeId() {
        Long currentUserId = 100L;
        Long currentEmployeeId = 50L;
        Long skillId = 10L;

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_DECLARE)).thenReturn(currentUserId);

        Employee mockEmployee = mock(Employee.class);
        when(mockEmployee.getIdValue()).thenReturn(currentEmployeeId);
        when(loadEmployeePort.findByUserId(new UserId(currentUserId))).thenReturn(Optional.of(mockEmployee));

        Skill mockSkill = mock(Skill.class);
        when(mockSkill.getName()).thenReturn("Java");
        when(skillCatalogRepository.findById(skillId)).thenReturn(Optional.of(mockSkill));

        when(employeeSkillRepository.existsByEmployeeIdAndSkillId(currentEmployeeId, skillId)).thenReturn(false);

        EmployeeSkill realSavedSkill = EmployeeSkill.declare(currentEmployeeId, skillId, 3, new BigDecimal("3.0"));
        when(employeeSkillRepository.save(any(EmployeeSkill.class))).thenReturn(realSavedSkill);

        DeclareEmployeeSkillCommand command = new DeclareEmployeeSkillCommand(
                null, // null employeeId
                skillId,
                3,
                new BigDecimal("3.0")
        );

        EmployeeSkillResult result = service.execute(command);

        assertNotNull(result);
        assertEquals(currentEmployeeId, result.employeeId());
        assertEquals(skillId, result.skillId());

        ArgumentCaptor<EmployeeSkill> captor = ArgumentCaptor.forClass(EmployeeSkill.class);
        verify(employeeSkillRepository).save(captor.capture());
        assertEquals(currentEmployeeId, captor.getValue().getEmployeeId());
    }
}
