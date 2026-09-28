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

import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixSkillHeaderResult;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentSkillMatrixService Application Tests (NCL-02-CN-007)")
class DepartmentSkillMatrixServiceTest {

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private SkillCatalogRepository skillCatalogRepository;

    @Mock
    private EmployeeSkillRepository employeeSkillRepository;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private AuthorizationService authorizationService;

    private DepartmentSkillMatrixService service;
    private User rmUser;
    private User companyUser;
    private User devUser;
    private OrgUnit department;

    @BeforeEach
    void setUp() {
        service = new DepartmentSkillMatrixService(
                loadOrgUnitPort,
                loadEmployeePort,
                skillCatalogRepository,
                employeeSkillRepository,
                loadUserPort,
                authorizationService
        );

        Role rmRole = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        rmUser = new User(new UserId(2L), "rm_user", "hash", rmRole, UserStatus.ACTIVE, new EmployeeId(2L), DataScope.ORGANIZATION_BRANCH, 10L, 0L);

        Role directorRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám Đốc");
        companyUser = new User(new UserId(1L), "director_user", "hash", directorRole, UserStatus.ACTIVE, new EmployeeId(1L), DataScope.COMPANY, null, 0L);

        Role devRole = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
        devUser = new User(new UserId(4L), "dev_user", "hash", devRole, UserStatus.ACTIVE, new EmployeeId(4L), DataScope.SELF, null, 0L);

        department = new OrgUnit(
                new OrgUnitId(10L), "DEV-TEAM", "Đội ngũ Phát triển", OrgUnitType.TEAM,
                new OrgUnitId(1L), "/1/10/", 2, OrgUnitStatus.ACTIVE,
                "Bộ phận kỹ thuật phần mềm", 2L, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("TC-01: Ma trận 5 nhân sự × 8 kỹ năng kèm dòng tổng đếm người theo kỹ năng thành công")
    void getMatrix_FiveEmployeesEightSkills_Success_TC01() {
        Long orgUnitId = 10L;

        // 5 nhân viên
        List<Employee> fiveEmployees = List.of(
                new Employee(new EmployeeId(101L), new UserId(11L), orgUnitId, "EMP001", "Nguyễn Văn A", "Backend Dev", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(102L), new UserId(12L), orgUnitId, "EMP002", "Trần Thị B", "Frontend Dev", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(103L), new UserId(13L), orgUnitId, "EMP003", "Lê Văn C", "Fullstack Dev", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(104L), new UserId(14L), orgUnitId, "EMP004", "Phạm Thị D", "QA Engineer", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(105L), new UserId(15L), orgUnitId, "EMP005", "Hoàng Văn E", "DevOps Engineer", null, null, false, 40, EmployeeStatus.ACTIVE)
        );

        // 8 kỹ năng
        List<Skill> eightSkills = List.of(
                new Skill(1L, "JAVA", "Java", "Backend", "Ngôn ngữ Java", LocalDateTime.now()),
                new Skill(2L, "SPRING", "Spring Boot", "Backend", "Framework Spring", LocalDateTime.now()),
                new Skill(3L, "REACT", "React.js", "Frontend", "Thư viện React", LocalDateTime.now()),
                new Skill(4L, "TYPESCRIPT", "TypeScript", "Frontend", "TypeScript", LocalDateTime.now()),
                new Skill(5L, "MYSQL", "MySQL", "Database", "Database MySQL", LocalDateTime.now()),
                new Skill(6L, "DOCKER", "Docker", "DevOps", "Container Docker", LocalDateTime.now()),
                new Skill(7L, "K8S", "Kubernetes", "DevOps", "Container Orchestration", LocalDateTime.now()),
                new Skill(8L, "GIT", "Git", "Tool", "Version Control", LocalDateTime.now())
        );

        // Các kỹ năng đã được duyệt (APPROVED)
        List<EmployeeSkill> approvedSkills = List.of(
                new EmployeeSkill(1L, 101L, 1L, ProficiencyLevel.EXPERT, new BigDecimal("4.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Tốt", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(2L, 101L, 2L, ProficiencyLevel.ADVANCED, new BigDecimal("3.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Tốt", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(3L, 102L, 3L, ProficiencyLevel.PROFICIENT, new BigDecimal("3.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Tốt", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(4L, 103L, 1L, ProficiencyLevel.INTERMEDIATE, new BigDecimal("2.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Khá", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(5L, 103L, 3L, ProficiencyLevel.ADVANCED, new BigDecimal("2.5"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Khá", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(6L, 105L, 6L, ProficiencyLevel.EXPERT, new BigDecimal("5.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Chuyên gia", LocalDateTime.now(), LocalDateTime.now())
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))).thenReturn(Optional.of(department));
        when(loadEmployeePort.findActiveByOrgUnitId(orgUnitId)).thenReturn(fiveEmployees);
        when(skillCatalogRepository.findAll()).thenReturn(eightSkills);
        when(employeeSkillRepository.findByStatusAndEmployeeIdIn(SkillStatus.APPROVED, List.of(101L, 102L, 103L, 104L, 105L)))
                .thenReturn(approvedSkills);

        DepartmentSkillMatrixResult result = service.execute(orgUnitId);

        assertNotNull(result);
        assertEquals(orgUnitId, result.orgUnitId());
        assertEquals("DEV-TEAM", result.orgUnitCode());
        assertEquals("Đội ngũ Phát triển", result.orgUnitName());

        // Kiểm tra đúng 5 hàng và 8 cột
        assertEquals(5, result.rows().size());
        assertEquals(8, result.skills().size());

        // Kiểm tra dòng tổng đếm theo kỹ năng:
        // Java (ID 1): 2 người (101, 103)
        SkillMatrixSkillHeaderResult javaHeader = result.skills().stream().filter(s -> s.id().equals(1L)).findFirst().orElseThrow();
        assertEquals(2, javaHeader.employeeCount());
        assertFalse(javaHeader.singlePersonRisk());

        // Spring Boot (ID 2): 1 người (101) -> rủi ro phụ thuộc 1 người (TC-02)
        SkillMatrixSkillHeaderResult springHeader = result.skills().stream().filter(s -> s.id().equals(2L)).findFirst().orElseThrow();
        assertEquals(1, springHeader.employeeCount());
        assertTrue(springHeader.singlePersonRisk());

        // Kubernetes (ID 7): 0 người
        SkillMatrixSkillHeaderResult k8sHeader = result.skills().stream().filter(s -> s.id().equals(7L)).findFirst().orElseThrow();
        assertEquals(0, k8sHeader.employeeCount());
        assertFalse(k8sHeader.singlePersonRisk());

        // Kiểm tra tổng kết (Summary)
        assertEquals(5, result.summary().totalEmployees());
        assertEquals(8, result.summary().totalSkills());
    }

    @Test
    @DisplayName("TC-02: Kỹ năng chỉ có duy nhất 1 người nắm -> Đánh dấu singlePersonRisk = true")
    void getMatrix_SinglePersonSkill_FlagsRisk_TC02() {
        Long orgUnitId = 10L;

        List<Employee> employees = List.of(
                new Employee(new EmployeeId(101L), new UserId(11L), orgUnitId, "EMP001", "Nguyễn Văn A", "DevOps", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(102L), new UserId(12L), orgUnitId, "EMP002", "Trần Thị B", "Frontend", null, null, false, 40, EmployeeStatus.ACTIVE)
        );

        List<Skill> skills = List.of(
                new Skill(1L, "DOCKER", "Docker", "DevOps", "Docker", LocalDateTime.now()),
                new Skill(2L, "JAVA", "Java", "Backend", "Java", LocalDateTime.now())
        );

        // Docker chỉ có duy nhất 101 có, Java có cả 101 và 102
        List<EmployeeSkill> approvedSkills = List.of(
                new EmployeeSkill(1L, 101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("3.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Duyệt", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(2L, 101L, 2L, ProficiencyLevel.BASIC, new BigDecimal("1.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Duyệt", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(3L, 102L, 2L, ProficiencyLevel.EXPERT, new BigDecimal("5.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Duyệt", LocalDateTime.now(), LocalDateTime.now())
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))).thenReturn(Optional.of(department));
        when(loadEmployeePort.findActiveByOrgUnitId(orgUnitId)).thenReturn(employees);
        when(skillCatalogRepository.findAll()).thenReturn(skills);
        when(employeeSkillRepository.findByStatusAndEmployeeIdIn(SkillStatus.APPROVED, List.of(101L, 102L)))
                .thenReturn(approvedSkills);

        DepartmentSkillMatrixResult result = service.execute(orgUnitId);

        SkillMatrixSkillHeaderResult dockerHeader = result.skills().stream().filter(s -> s.id().equals(1L)).findFirst().orElseThrow();
        assertEquals(1, dockerHeader.employeeCount());
        assertTrue(dockerHeader.singlePersonRisk(), "Kỹ năng Docker chỉ có 1 người phải được đánh dấu rủi ro phụ thuộc 1 người (TC-02)");

        SkillMatrixSkillHeaderResult javaHeader = result.skills().stream().filter(s -> s.id().equals(2L)).findFirst().orElseThrow();
        assertEquals(2, javaHeader.employeeCount());
        assertFalse(javaHeader.singlePersonRisk(), "Kỹ năng Java có 2 người không bị cờ rủi ro");

        assertEquals(1, result.summary().singlePersonRiskSkillCount());
    }

    @Test
    @DisplayName("TC-03: Người dùng không có quyền EMPLOYEE_SKILL_READ -> Ném PermissionDeniedException")
    void getMatrix_UnauthorizedUser_ThrowsPermissionDenied_TC03() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ))
                .thenThrow(new PermissionDeniedException(PermissionCode.EMPLOYEE_SKILL_READ));

        assertThrows(PermissionDeniedException.class, () -> service.execute(10L));
        verify(loadEmployeePort, never()).findActiveByOrgUnitId(any());
    }

    @Test
    @DisplayName("TC-03: RM xem bộ phận ngoài phạm vi Data Scope chi nhánh -> Ném PermissionDeniedException")
    void getMatrix_OrgUnitOutOfBranchScope_ThrowsPermissionDenied_TC03() {
        Long outOfScopeUnitId = 99L;
        OrgUnit outOfScopeUnit = new OrgUnit(
                new OrgUnitId(outOfScopeUnitId), "OTHER", "Đơn vị khác", OrgUnitType.DEPARTMENT,
                null, "/99/", 1, OrgUnitStatus.ACTIVE, "Đơn vị khác", null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.existsInOrgUnitBranch(outOfScopeUnitId, 10L)).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> service.execute(outOfScopeUnitId));
        verify(loadEmployeePort, never()).findActiveByOrgUnitId(any());
    }

    @Test
    @DisplayName("TC-04: Chỉ các kỹ năng đã APPROVED mới xuất hiện trên ma trận")
    void getMatrix_OnlyApprovedSkillsAreCounted_TC04() {
        Long orgUnitId = 10L;

        List<Employee> employees = List.of(
                new Employee(new EmployeeId(101L), new UserId(11L), orgUnitId, "EMP001", "Nguyễn Văn A", "Dev", null, null, false, 40, EmployeeStatus.ACTIVE)
        );

        List<Skill> skills = List.of(
                new Skill(1L, "JAVA", "Java", "Backend", "Java", LocalDateTime.now())
        );

        // Chỉ trả về bản ghi APPROVED
        List<EmployeeSkill> approvedSkills = List.of(
                new EmployeeSkill(1L, 101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("3.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Đã duyệt", LocalDateTime.now(), LocalDateTime.now())
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))).thenReturn(Optional.of(department));
        when(loadEmployeePort.findActiveByOrgUnitId(orgUnitId)).thenReturn(employees);
        when(skillCatalogRepository.findAll()).thenReturn(skills);
        when(employeeSkillRepository.findByStatusAndEmployeeIdIn(SkillStatus.APPROVED, List.of(101L)))
                .thenReturn(approvedSkills);

        DepartmentSkillMatrixResult result = service.execute(orgUnitId);

        assertEquals(1, result.rows().get(0).skills().size());
        assertEquals(3, result.rows().get(0).skills().get(1L).proficiencyLevel());
    }

    @Test
    @DisplayName("Edge Case: Bộ phận chưa có nhân viên nào -> Trả về ma trận với rows rỗng không lỗi")
    void getMatrix_EmptyDepartment_ReturnsEmptyRows() {
        Long orgUnitId = 10L;

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))).thenReturn(Optional.of(department));
        when(loadEmployeePort.findActiveByOrgUnitId(orgUnitId)).thenReturn(List.of());
        when(skillCatalogRepository.findAll()).thenReturn(List.of(new Skill(1L, "JAVA", "Java", "Backend", "Java", LocalDateTime.now())));

        DepartmentSkillMatrixResult result = service.execute(orgUnitId);

        assertNotNull(result);
        assertTrue(result.rows().isEmpty());
        assertEquals(0, result.summary().totalEmployees());
        assertEquals(1, result.summary().unstaffedSkillCount());
    }

    @Test
    @DisplayName("Edge Case: Không tìm thấy đơn vị -> Ném OrgUnitNotFoundException")
    void getMatrix_OrgUnitNotFound_ThrowsException() {
        Long nonExistentId = 999L;

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(companyUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(nonExistentId))).thenReturn(Optional.empty());

        assertThrows(OrgUnitNotFoundException.class, () -> service.execute(nonExistentId));
    }

    @Test
    @DisplayName("Test 1: COMPANY + null -> execute(null) trả toàn bộ nhân sự toàn công ty")
    void getMatrix_CompanyScope_NullOrgUnitId_ReturnsAllCompanyEmployees_Test1() {
        List<Employee> allEmployees = List.of(
                new Employee(new EmployeeId(101L), new UserId(11L), 10L, "EMP001", "Nguyễn Văn A", "Backend Dev", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(102L), new UserId(12L), 20L, "EMP002", "Trần Thị B", "Frontend Dev", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(103L), new UserId(13L), 30L, "EMP003", "Lê Văn C", "QA", null, null, false, 40, EmployeeStatus.ACTIVE)
        );

        List<Skill> skills = List.of(
                new Skill(1L, "JAVA", "Java", "Backend", "Java", LocalDateTime.now()),
                new Skill(2L, "REACT", "React.js", "Frontend", "React", LocalDateTime.now())
        );

        List<EmployeeSkill> approvedSkills = List.of(
                new EmployeeSkill(1L, 101L, 1L, ProficiencyLevel.EXPERT, new BigDecimal("4.0"), SkillStatus.APPROVED, 1L, LocalDateTime.now(), null, "Tốt", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(2L, 102L, 2L, ProficiencyLevel.ADVANCED, new BigDecimal("3.0"), SkillStatus.APPROVED, 1L, LocalDateTime.now(), null, "Tốt", LocalDateTime.now(), LocalDateTime.now())
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(companyUser));
        when(loadEmployeePort.findAllActive()).thenReturn(allEmployees);
        when(skillCatalogRepository.findAll()).thenReturn(skills);
        when(employeeSkillRepository.findByStatusAndEmployeeIdIn(SkillStatus.APPROVED, List.of(101L, 102L, 103L)))
                .thenReturn(approvedSkills);

        DepartmentSkillMatrixResult result = service.execute(null);

        assertNotNull(result);
        assertNull(result.orgUnitId());
        assertEquals("ALL", result.orgUnitCode());
        assertEquals("Toàn công ty", result.orgUnitName());
        assertEquals(3, result.rows().size());
        assertEquals(3, result.summary().totalEmployees());

        verify(loadOrgUnitPort, never()).findById(any());
        verify(loadEmployeePort).findAllActive();
        verify(loadEmployeePort, never()).findActiveByOrgUnitId(any());
        verify(loadEmployeePort, never()).findActiveByOrgUnitIds(any());
    }

    @Test
    @DisplayName("Test 2: VT-03 + null -> execute(null) chỉ trả nhân sự thuộc branch của VT-03")
    void getMatrix_BranchScope_NullOrgUnitId_ReturnsBranchEmployees_Test2() {
        Long branchId = 10L;

        List<Employee> branchEmployees = List.of(
                new Employee(new EmployeeId(101L), new UserId(11L), branchId, "EMP001", "Nguyễn Văn A", "Backend Dev", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(102L), new UserId(12L), branchId, "EMP002", "Trần Thị B", "Frontend Dev", null, null, false, 40, EmployeeStatus.ACTIVE)
        );

        List<Skill> skills = List.of(
                new Skill(1L, "JAVA", "Java", "Backend", "Java", LocalDateTime.now())
        );

        List<EmployeeSkill> approvedSkills = List.of(
                new EmployeeSkill(1L, 101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("3.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Tốt", LocalDateTime.now(), LocalDateTime.now())
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(branchId))).thenReturn(Optional.of(department));
        when(loadEmployeePort.findActiveByOrgUnitId(branchId)).thenReturn(branchEmployees);
        when(skillCatalogRepository.findAll()).thenReturn(skills);
        when(employeeSkillRepository.findByStatusAndEmployeeIdIn(SkillStatus.APPROVED, List.of(101L, 102L)))
                .thenReturn(approvedSkills);

        DepartmentSkillMatrixResult result = service.execute(null);

        assertNotNull(result);
        assertEquals(branchId, result.orgUnitId());
        assertEquals("DEV-TEAM", result.orgUnitCode());
        assertEquals("Đội ngũ Phát triển", result.orgUnitName());
        assertEquals(2, result.rows().size());

        verify(loadEmployeePort, never()).findAllActive();
        verify(loadOrgUnitPort).findById(new OrgUnitId(branchId));
    }

    @Test
    @DisplayName("Test 3: VT-03 chọn phòng ban trong branch -> execute(childOrgUnitId) thành công")
    void getMatrix_BranchScope_ChildOrgUnitInScope_Success_Test3() {
        Long childOrgUnitId = 20L;
        OrgUnit childUnit = new OrgUnit(
                new OrgUnitId(childOrgUnitId), "DEV-SUBTEAM", "Đội ngũ Backend Con", OrgUnitType.TEAM,
                new OrgUnitId(10L), "/1/10/20/", 3, OrgUnitStatus.ACTIVE,
                "Nhóm Backend con", 2L, LocalDateTime.now(), LocalDateTime.now()
        );

        List<Employee> childEmployees = List.of(
                new Employee(new EmployeeId(101L), new UserId(11L), childOrgUnitId, "EMP001", "Nguyễn Văn A", "Backend", null, null, false, 40, EmployeeStatus.ACTIVE)
        );

        List<Skill> skills = List.of(
                new Skill(1L, "JAVA", "Java", "Backend", "Java", LocalDateTime.now())
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(childOrgUnitId))).thenReturn(Optional.of(childUnit));
        when(loadOrgUnitPort.existsInOrgUnitBranch(childOrgUnitId, 10L)).thenReturn(true);
        when(loadEmployeePort.findActiveByOrgUnitId(childOrgUnitId)).thenReturn(childEmployees);
        when(skillCatalogRepository.findAll()).thenReturn(skills);
        when(employeeSkillRepository.findByStatusAndEmployeeIdIn(SkillStatus.APPROVED, List.of(101L)))
                .thenReturn(List.of());

        DepartmentSkillMatrixResult result = service.execute(childOrgUnitId);

        assertNotNull(result);
        assertEquals(childOrgUnitId, result.orgUnitId());
        assertEquals("DEV-SUBTEAM", result.orgUnitCode());
        assertEquals("Đội ngũ Backend Con", result.orgUnitName());
        assertEquals(1, result.rows().size());
    }

    @Test
    @DisplayName("Test 5: Branch có nhiều cấp con (Branch A -> Team A1, Team A2 -> Team A2.1) -> Chọn Branch A lấy đúng toàn bộ cây con")
    void getMatrix_BranchWithMultipleDescendantLevels_ResolvesFullSubtree_Test5() {
        Long branchAId = 10L;
        Long teamA1Id = 11L;
        Long teamA2Id = 12L;
        Long teamA21Id = 13L;

        OrgUnit branchA = new OrgUnit(
                new OrgUnitId(branchAId), "BRANCH-A", "Khối Phát triển A", OrgUnitType.DEPARTMENT,
                null, "/10/", 1, OrgUnitStatus.ACTIVE, "Branch A", 2L, LocalDateTime.now(), LocalDateTime.now()
        );
        OrgUnit teamA1 = new OrgUnit(
                new OrgUnitId(teamA1Id), "TEAM-A1", "Nhóm A1", OrgUnitType.TEAM,
                new OrgUnitId(branchAId), "/10/11/", 2, OrgUnitStatus.ACTIVE, "Team A1", 2L, LocalDateTime.now(), LocalDateTime.now()
        );
        OrgUnit teamA2 = new OrgUnit(
                new OrgUnitId(teamA2Id), "TEAM-A2", "Nhóm A2", OrgUnitType.TEAM,
                new OrgUnitId(branchAId), "/10/12/", 2, OrgUnitStatus.ACTIVE, "Team A2", 2L, LocalDateTime.now(), LocalDateTime.now()
        );
        OrgUnit teamA21 = new OrgUnit(
                new OrgUnitId(teamA21Id), "TEAM-A2.1", "Tổ kỹ thuật A2.1", OrgUnitType.TEAM,
                new OrgUnitId(teamA2Id), "/10/12/13/", 3, OrgUnitStatus.ACTIVE, "Team A2.1", 2L, LocalDateTime.now(), LocalDateTime.now()
        );

        List<OrgUnit> allUnits = List.of(branchA, teamA1, teamA2, teamA21);

        List<Employee> treeEmployees = List.of(
                new Employee(new EmployeeId(101L), new UserId(11L), branchAId, "EMP001", "Trưởng khối A", "Manager", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(102L), new UserId(12L), teamA1Id, "EMP002", "Dev A1", "Developer", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(103L), new UserId(13L), teamA2Id, "EMP003", "Dev A2", "Developer", null, null, false, 40, EmployeeStatus.ACTIVE),
                new Employee(new EmployeeId(104L), new UserId(14L), teamA21Id, "EMP004", "Dev A2.1", "Developer", null, null, false, 40, EmployeeStatus.ACTIVE)
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(branchAId))).thenReturn(Optional.of(branchA));
        when(loadOrgUnitPort.findAll()).thenReturn(allUnits);
        when(loadEmployeePort.findActiveByOrgUnitIds(argThat(ids -> ids != null
                && ids.size() == 4
                && ids.containsAll(List.of(branchAId, teamA1Id, teamA2Id, teamA21Id)))))
                .thenReturn(treeEmployees);
        when(skillCatalogRepository.findAll()).thenReturn(List.of());

        DepartmentSkillMatrixResult result = service.execute(null);

        assertNotNull(result);
        assertEquals(branchAId, result.orgUnitId());
        assertEquals("BRANCH-A", result.orgUnitCode());
        assertEquals(4, result.rows().size());
        assertEquals(4, result.summary().totalEmployees());

        verify(loadEmployeePort).findActiveByOrgUnitIds(argThat(ids -> ids.containsAll(List.of(10L, 11L, 12L, 13L))));
    }

    @Test
    @DisplayName("DataScope SELF -> Ném PermissionDeniedException")
    void getMatrix_SelfScope_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(4L);
        when(loadUserPort.findById(new UserId(4L))).thenReturn(Optional.of(devUser));

        assertThrows(PermissionDeniedException.class, () -> service.execute(null));
        assertThrows(PermissionDeniedException.class, () -> service.execute(10L));
    }

    @Test
    @DisplayName("DataScope ORGANIZATION_BRANCH nhưng scopeOrgUnitId là null -> Ném PermissionDeniedException")
    void getMatrix_BranchScopeWithNullScopeOrgUnitId_ThrowsPermissionDenied() {
        User userWithoutScope = mock(User.class);
        when(userWithoutScope.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(userWithoutScope.getScopeOrgUnitId()).thenReturn(null);

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(99L);
        when(loadUserPort.findById(new UserId(99L))).thenReturn(Optional.of(userWithoutScope));

        assertThrows(PermissionDeniedException.class, () -> service.execute(null));
    }

    @Test
    @DisplayName("Edge Case: Có duplicate bản ghi EmployeeSkill cho cùng 1 nhân sự -> Chỉ đếm 1 nhân sự duy nhất và phát hiện đúng rủi ro phụ thuộc 1 người")
    void getMatrix_DuplicateEmployeeSkillRecords_CountsDistinctEmployeesOnly() {
        Long orgUnitId = 10L;

        List<Employee> employees = List.of(
                new Employee(new EmployeeId(101L), new UserId(11L), orgUnitId, "EMP001", "Nguyễn Văn A", "Dev", null, null, false, 40, EmployeeStatus.ACTIVE)
        );

        List<Skill> skills = List.of(
                new Skill(1L, "JAVA", "Java", "Backend", "Java", LocalDateTime.now())
        );

        // 2 bản ghi APPROVED cho cùng 1 nhân viên 101 với skill 1
        List<EmployeeSkill> approvedSkills = List.of(
                new EmployeeSkill(1L, 101L, 1L, ProficiencyLevel.INTERMEDIATE, new BigDecimal("2.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Đã duyệt 1", LocalDateTime.now(), LocalDateTime.now()),
                new EmployeeSkill(2L, 101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("3.0"), SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Đã duyệt 2", LocalDateTime.now(), LocalDateTime.now())
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))).thenReturn(Optional.of(department));
        when(loadEmployeePort.findActiveByOrgUnitId(orgUnitId)).thenReturn(employees);
        when(skillCatalogRepository.findAll()).thenReturn(skills);
        when(employeeSkillRepository.findByStatusAndEmployeeIdIn(SkillStatus.APPROVED, List.of(101L)))
                .thenReturn(approvedSkills);

        DepartmentSkillMatrixResult result = service.execute(orgUnitId);

        SkillMatrixSkillHeaderResult javaHeader = result.skills().stream().filter(s -> s.id().equals(1L)).findFirst().orElseThrow();
        assertEquals(1, javaHeader.employeeCount(), "Số lượng nhân sự phải là 1 (distinct) dù có 2 bản ghi kỹ năng");
        assertTrue(javaHeader.singlePersonRisk(), "Vẫn phải đánh dấu rủi ro phụ thuộc 1 người vì chỉ có 1 nhân sự nắm giữ");
    }
}
