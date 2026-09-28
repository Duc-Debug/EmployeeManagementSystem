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
    @DisplayName("Lấy danh sách kỹ năng cá nhân của nhân viên đang đăng nhập thành công với batch query (tránh N+1)")
    void shouldGetMySkillsSuccessfullyWithBatchQuery() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(99L);

        Employee employee = mock(Employee.class);
        when(employee.getIdValue()).thenReturn(10L);
        when(loadEmployeePort.findByUserId(new UserId(99L))).thenReturn(Optional.of(employee));

        EmployeeSkill es1 = new EmployeeSkill(
                1L, 10L, 100L, ProficiencyLevel.fromValue(3), new BigDecimal("2.5"),
                SkillStatus.APPROVED, 99L, LocalDateTime.now(), null, "Notes", LocalDateTime.now(), LocalDateTime.now()
        );
        EmployeeSkill es2 = new EmployeeSkill(
                2L, 10L, 101L, ProficiencyLevel.fromValue(4), new BigDecimal("4.0"),
                SkillStatus.APPROVED, 99L, LocalDateTime.now(), null, "Notes", LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeId(10L)).thenReturn(List.of(es1, es2));

        Skill skill1 = new Skill(100L, "JAVA", "Java", "Backend", "Desc", 3L, LocalDateTime.now());
        Skill skill2 = new Skill(101L, "REACT", "React", "Frontend", "Desc", 4L, LocalDateTime.now());
        when(skillCatalogRepository.findAllByIdIn(List.of(100L, 101L))).thenReturn(List.of(skill1, skill2));

        List<EmployeeSkillResult> results = service.execute();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Java", results.get(0).skillName());
        assertEquals("React", results.get(1).skillName());

        // Đảm bảo không gọi findById đơn lẻ (Tránh N+1)
        verify(skillCatalogRepository, never()).findById(any());
        verify(skillCatalogRepository, times(1)).findAllByIdIn(List.of(100L, 101L));
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
        verify(skillCatalogRepository, never()).findAllByIdIn(any());
    }
}
