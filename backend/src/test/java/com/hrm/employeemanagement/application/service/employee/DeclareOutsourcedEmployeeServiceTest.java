package com.hrm.employeemanagement.application.service.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.employee.DeclareOutsourcedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.employee.OutsourcedEmployeeResult;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.employee.InvalidEmployeeDataException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class DeclareOutsourcedEmployeeServiceTest {

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private SaveEmployeePort saveEmployeePort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private SkillCatalogRepository skillCatalogRepository;

    @Mock
    private EmployeeSkillRepository employeeSkillRepository;

    private DeclareOutsourcedEmployeeService service;
    private User hrUser;
    private OrgUnit activeOrgUnit;

    @BeforeEach
    void setUp() {
        service = new DeclareOutsourcedEmployeeService(
                loadEmployeePort,
                saveEmployeePort,
                loadOrgUnitPort,
                loadUserPort,
                authorizationService,
                saveAuditLogPort,
                skillCatalogRepository,
                employeeSkillRepository
        );

        hrUser = new User(
                new UserId(100L),
                "hr_user",
                "hash",
                new Role(new RoleId(5L), RoleCode.VT_05, "Nhân sự"),
                UserStatus.ACTIVE,
                new EmployeeId(10L),
                DataScope.COMPANY,
                null,
                0L
        );

        activeOrgUnit = new OrgUnit(
                new OrgUnitId(1L),
                "DEP-DEV",
                "Phòng Phát Triển",
                OrgUnitType.DEPARTMENT,
                null,
                "/001/",
                1,
                OrgUnitStatus.ACTIVE,
                "Mô tả",
                null,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("NCL-14-CN-001-TC-01: Luồng thành công - Khai báo hồ sơ chuyên gia thuê ngoài 3 tháng kèm kỹ năng")
    void execute_HappyPath_DeclaresOutsourcedEmployeeSuccessfully() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        DeclareOutsourcedEmployeeCommand command = new DeclareOutsourcedEmployeeCommand(
                1L,
                "EXT-001",
                "Nguyễn Văn Chuyên Gia",
                "Công ty Giải Pháp Công Nghệ ABC",
                "Senior Java Specialist",
                startDate,
                endDate,
                40,
                List.of(10L, 20L)
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(hrUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(activeOrgUnit));
        when(loadEmployeePort.existsByEmployeeCode("EXT-001")).thenReturn(false);

        Skill javaSkill = new Skill(10L, "JAVA", "Java", "Backend", "Java programming", null);
        Skill springSkill = new Skill(20L, "SPRING_BOOT", "Spring Boot", "Backend", "Spring Boot", null);
        when(skillCatalogRepository.findAllByIdIn(List.of(10L, 20L))).thenReturn(List.of(javaSkill, springSkill));

        Employee savedMock = new Employee(
                new EmployeeId(50L),
                null,
                1L,
                "EXT-001",
                "Nguyễn Văn Chuyên Gia",
                "Senior Java Specialist",
                startDate,
                endDate,
                true,
                40,
                EmployeeStatus.ACTIVE,
                "Công ty Giải Pháp Công Nghệ ABC",
                0L
        );
        when(saveEmployeePort.save(any(Employee.class))).thenReturn(savedMock);

        // Act
        OutsourcedEmployeeResult result = service.execute(command);

        // Assert
        assertNotNull(result);
        assertEquals(50L, result.id());
        assertEquals("EXT-001", result.employeeCode());
        assertEquals("Nguyễn Văn Chuyên Gia", result.fullName());
        assertEquals("Công ty Giải Pháp Công Nghệ ABC", result.providerName());
        assertTrue(result.isOutsourced());
        assertEquals(40, result.standardHoursPerWeek());
        assertEquals(startDate, result.startDate());
        assertEquals(endDate, result.contractEndDate());
        assertEquals(2, result.skillNames().size());

        // NCL-14-CN-001-TC-04: Kiểm tra lưu lịch sử audit log
        verify(saveAuditLogPort).save(argThat(audit ->
                "DECLARE_OUTSOURCED_EMPLOYEE".equals(audit.getAction())
                        && "employees".equals(audit.getTableName())
                        && Long.valueOf(50L).equals(audit.getRecordId())
                        && audit.getNewValue().contains("providerName=Công ty Giải Pháp Công Nghệ ABC")
        ));
    }

    @Test
    @DisplayName("NCL-14-CN-001-TC-02: Dữ liệu không hợp lệ - Ngày kết thúc sớm hơn ngày bắt đầu -> báo lỗi và không lưu")
    void execute_EndDateBeforeStartDate_ThrowsInvalidEmployeeDataException() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 10, 15);
        LocalDate endDate = LocalDate.of(2026, 10, 10); // Sớm hơn ngày bắt đầu!
        DeclareOutsourcedEmployeeCommand command = new DeclareOutsourcedEmployeeCommand(
                1L,
                "EXT-002",
                "Trần Văn B",
                "Công ty XYZ",
                "Tester",
                startDate,
                endDate,
                40,
                null
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(hrUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(activeOrgUnit));
        when(loadEmployeePort.existsByEmployeeCode("EXT-002")).thenReturn(false);

        // Act & Assert
        InvalidEmployeeDataException ex = assertThrows(InvalidEmployeeDataException.class, () -> service.execute(command));
        assertEquals("Ngày kết thúc hợp đồng thuê không được sớm hơn ngày bắt đầu", ex.getMessage());

        // Đảm bảo KHÔNG lưu hồ sơ và KHÔNG ghi business audit thay đổi
        verify(saveEmployeePort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Boundary: Ngày kết thúc bằng ngày bắt đầu (hợp đồng 1 ngày) -> hợp lệ")
    void execute_EndDateEqualsStartDate_Valid() {
        LocalDate sameDate = LocalDate.of(2026, 11, 1);
        DeclareOutsourcedEmployeeCommand command = new DeclareOutsourcedEmployeeCommand(
                1L,
                "EXT-003",
                "Lê Văn C",
                "Đối tác 123",
                "Consultant",
                sameDate,
                sameDate,
                40,
                null
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(hrUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(activeOrgUnit));
        when(loadEmployeePort.existsByEmployeeCode("EXT-003")).thenReturn(false);

        Employee savedMock = new Employee(
                new EmployeeId(51L),
                null,
                1L,
                "EXT-003",
                "Lê Văn C",
                "Consultant",
                sameDate,
                sameDate,
                true,
                40,
                EmployeeStatus.ACTIVE,
                "Đối tác 123",
                0L
        );
        when(saveEmployeePort.save(any(Employee.class))).thenReturn(savedMock);

        OutsourcedEmployeeResult result = service.execute(command);
        assertNotNull(result);
        assertEquals(sameDate, result.startDate());
        assertEquals(sameDate, result.contractEndDate());
    }

    @Test
    @DisplayName("Validation: Thiếu đơn vị cung cấp -> báo lỗi")
    void execute_MissingProviderName_ThrowsException() {
        DeclareOutsourcedEmployeeCommand command = new DeclareOutsourcedEmployeeCommand(
                1L,
                "EXT-004",
                "Phạm Văn D",
                "", // Blank provider
                "BA",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 1),
                40,
                null
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(hrUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(activeOrgUnit));

        InvalidEmployeeDataException ex = assertThrows(InvalidEmployeeDataException.class, () -> service.execute(command));
        assertEquals("Đơn vị cung cấp nhân sự thuê ngoài không được để trống", ex.getMessage());
    }

    @Test
    @DisplayName("Validation: Trùng mã nhân viên -> báo lỗi")
    void execute_DuplicateEmployeeCode_ThrowsException() {
        DeclareOutsourcedEmployeeCommand command = new DeclareOutsourcedEmployeeCommand(
                1L,
                "EXT-DUP",
                "Hoàng Văn E",
                "Công ty DEF",
                "Dev",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 1),
                40,
                null
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(hrUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(activeOrgUnit));
        when(loadEmployeePort.existsByEmployeeCode("EXT-DUP")).thenReturn(true);

        InvalidEmployeeDataException ex = assertThrows(InvalidEmployeeDataException.class, () -> service.execute(command));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = RoleCode.class, names = {"VT_01", "VT_02", "VT_03", "VT_04", "VT_06"})
    void rejectsOtherRolesEvenWithUpdatePermission(RoleCode roleCode) {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getRole()).thenReturn(new Role(new RoleId(1L), roleCode, roleCode.getName()));
        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(user));
        assertThrows(com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException.class,
                () -> service.execute(validCommand(null)));
        org.mockito.Mockito.verifyNoInteractions(saveEmployeePort, employeeSkillRepository, saveAuditLogPort, loadOrgUnitPort);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"SELF,false", "ORGANIZATION_BRANCH,false", "ORGANIZATION_BRANCH,true"})
    void validatesDataScope(DataScope scope, boolean inBranch) {
        // Mock legacy/configured scope independently of User's current role/scope constructor constraints.
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getRole()).thenReturn(hrUser.getRole());
        when(user.getDataScope()).thenReturn(scope);
        when(user.getScopeOrgUnitId()).thenReturn(scope == DataScope.SELF ? null : 2L);
        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(user));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(activeOrgUnit));
        if (scope == DataScope.ORGANIZATION_BRANCH) {
            when(loadOrgUnitPort.existsInOrgUnitBranch(1L, 2L)).thenReturn(inBranch);
        }
        if (inBranch) {
            when(saveEmployeePort.save(any())).thenAnswer(invocation -> {
                Employee employee = invocation.getArgument(0);
                return new Employee(new EmployeeId(50L), null, 1L, employee.getEmployeeCode(),
                        employee.getFullName(), "Dev", employee.getStartDate(), employee.getContractEndDate(),
                        true, 40, EmployeeStatus.ACTIVE, "Vendor", 0L);
            });
            assertNotNull(service.execute(validCommand(null)));
        } else {
            assertThrows(com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException.class,
                    () -> service.execute(validCommand(null)));
            org.mockito.Mockito.verifyNoInteractions(saveEmployeePort, employeeSkillRepository, saveAuditLogPort);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"MISSING", "INACTIVE", "MERGED"})
    void rejectsInvalidSkillsBeforeSaving(String status) {
        when(authorizationService.require(PermissionCode.EMPLOYEE_UPDATE)).thenReturn(100L);
        when(loadUserPort.findById(new UserId(100L))).thenReturn(Optional.of(hrUser));
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(activeOrgUnit));
        Skill active = new Skill(10L, "JAVA", "Java", "Backend", null, null);
        List<Skill> found = status.equals("MISSING") ? List.of(active) : List.of(active,
                new Skill(20L, "OLD", "Old", "Backend", null, 1L, null,
                        com.hrm.employeemanagement.domain.skill.SkillStatus.valueOf(status)));
        when(skillCatalogRepository.findAllByIdIn(List.of(10L, 20L))).thenReturn(found);
        assertThrows(InvalidEmployeeDataException.class, () -> service.execute(validCommand(List.of(10L, 20L))));
        org.mockito.Mockito.verifyNoInteractions(saveEmployeePort, employeeSkillRepository, saveAuditLogPort);
    }

    @Test
    void requiresSkillRepositories() {
        assertThrows(NullPointerException.class, () -> new DeclareOutsourcedEmployeeService(loadEmployeePort,
                saveEmployeePort, loadOrgUnitPort, loadUserPort, authorizationService, saveAuditLogPort,
                null, employeeSkillRepository));
        assertThrows(NullPointerException.class, () -> new DeclareOutsourcedEmployeeService(loadEmployeePort,
                saveEmployeePort, loadOrgUnitPort, loadUserPort, authorizationService, saveAuditLogPort,
                skillCatalogRepository, null));
    }

    private DeclareOutsourcedEmployeeCommand validCommand(List<Long> skills) {
        return new DeclareOutsourcedEmployeeCommand(1L, "EXT-TEST", "Person", "Vendor", "Dev",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), 40, skills);
    }

}
