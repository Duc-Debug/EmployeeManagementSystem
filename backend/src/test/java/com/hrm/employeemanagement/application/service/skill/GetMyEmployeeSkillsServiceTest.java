package com.hrm.employeemanagement.application.service.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyEmployeeSkillsService Unit Tests")
class GetMyEmployeeSkillsServiceTest {

    @Mock
    private EmployeeSkillRepository employeeSkillRepository;

    @Mock
    private SkillCatalogRepository skillCatalogRepository;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private AuthorizationService authorizationService;

    private GetMyEmployeeSkillsService service;

    @BeforeEach
    void setUp() {
        service = new GetMyEmployeeSkillsService(
                employeeSkillRepository,
                skillCatalogRepository,
                loadEmployeePort,
                authorizationService
        );
    }

    @Test
    @DisplayName("Lấy danh sách kỹ năng cá nhân của nhân viên đang đăng nhập thành công")
    void shouldGetMySkillsSuccessfully() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(99L);

        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(10L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.of(employee));

        EmployeeSkill es = new EmployeeSkill(
                1L, 10L, 100L, ProficiencyLevel.fromValue(3), new BigDecimal("2.5"),
                SkillStatus.APPROVED, 99L, LocalDateTime.now(), null, "Notes", LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeId(10L)).thenReturn(List.of(es));

        Skill skill = new Skill(100L, "JAVA", "Java", "Backend", "Desc", 3L, LocalDateTime.now());
        when(skillCatalogRepository.findById(100L)).thenReturn(Optional.of(skill));

        List<EmployeeSkillResult> results = service.execute();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Java", results.get(0).skillName());
        assertEquals(3, results.get(0).proficiencyLevel());
    }

    @Test
    @DisplayName("Trả về danh sách rỗng nếu nhân viên chưa khởi tạo hồ sơ")
    void shouldReturnEmptyListWhenNoEmployeeProfile() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(99L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.empty());

        List<EmployeeSkillResult> results = service.execute();

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(employeeSkillRepository, never()).findByEmployeeId(any());
    }
}
