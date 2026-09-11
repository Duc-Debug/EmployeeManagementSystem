package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataLeaveRequestRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.entity.EmployeeLeaveBalanceJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave.repository.SpringDataLeaveBalanceRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-05-CN-005: REST API Integration Test cho Quỹ phép năm")
class LeaveBalanceControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private SpringDataUserRepository userRepository;
    @Autowired
    private SpringDataRoleRepository roleRepository;
    @Autowired
    private SpringDataEmployeeRepository employeeRepository;
    @Autowired
    private SpringDataLeaveBalanceRepository leaveBalanceRepository;
    @Autowired
    private SpringDataLeaveRequestRepository leaveRequestRepository;
    @Autowired
    private SpringDataAuditLogRepository auditLogRepository;

    private UserJpaEntity employeeUser;
    private EmployeeJpaEntity employee;
    private UserJpaEntity managerUser;
    private EmployeeJpaEntity manager;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 1. Tìm hoặc tạo Role VT-04 (Nhân viên) & VT-06 (Quản trị/HR)
        RoleJpaEntity roleEmployee = roleRepository.findByCode("VT-04")
                .orElseGet(() -> roleRepository.save(new RoleJpaEntity(null, "VT-04", "Nhân viên chuyên môn")));
        RoleJpaEntity roleManager = roleRepository.findByCode("VT-06")
                .orElseGet(() -> roleRepository.save(new RoleJpaEntity(null, "VT-06", "Quản trị hệ thống")));

        // 2. Tạo User & Employee cho Nhân viên
        long time = System.nanoTime();
        employeeUser = new UserJpaEntity(
                null,
                "emp_bal_" + time,
                "$2a$10$hashPlaceholderForIntegrationTest12345678901234567890",
                roleEmployee,
                true
        );
        employeeUser.setDataScope(DataScope.SELF.name());
        employeeUser = userRepository.saveAndFlush(employeeUser);

        employee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, employeeUser.getId(), null, "EMP-BAL-" + time,
                "Test Leave Employee", false, 40, "ACTIVE"
        ));

        // 3. Tạo User & Employee cho Quản lý / HR (VT-06 với DataScope COMPANY)
        managerUser = new UserJpaEntity(
                null,
                "mgr_bal_" + time,
                "$2a$10$hashPlaceholderForIntegrationTest12345678901234567890",
                roleManager,
                true
        );
        managerUser.setDataScope(DataScope.COMPANY.name());
        managerUser = userRepository.saveAndFlush(managerUser);

        manager = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, managerUser.getId(), null, "MGR-BAL-" + time,
                "Test Leave Manager", false, 40, "ACTIVE"
        ));

        // 4. Khởi tạo quỹ phép cho nhân viên: 12 ngày tiêu chuẩn, 2 ngày chuyển tiếp
        EmployeeLeaveBalanceJpaEntity balance = new EmployeeLeaveBalanceJpaEntity();
        balance.setEmployeeId(employee.getId());
        balance.setYearNumber(2026);
        balance.setEntitledDays(new BigDecimal("12.00"));
        balance.setCarriedOverDays(new BigDecimal("2.00"));
        leaveBalanceRepository.saveAndFlush(balance);
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        leaveBalanceRepository.deleteAll();
        if (employee != null) {
            leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employee.getId())
                    .forEach(lr -> leaveRequestRepository.deleteById(lr.getId()));
            employeeRepository.deleteById(employee.getId());
        }
        if (manager != null) {
            leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(manager.getId())
                    .forEach(lr -> leaveRequestRepository.deleteById(lr.getId()));
            employeeRepository.deleteById(manager.getId());
        }
        if (employeeUser != null) {
            auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                    .filter(a -> employeeUser.getId().equals(a.getUserId()))
                    .toList());
            userRepository.deleteById(employeeUser.getId());
        }
        if (managerUser != null) {
            auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                    .filter(a -> managerUser.getId().equals(a.getUserId()))
                    .toList());
            userRepository.deleteById(managerUser.getId());
        }
    }

    private UsernamePasswordAuthenticationToken authForEmployee() {
        User principal = new User(
                new UserId(employeeUser.getId()),
                employeeUser.getUsername(),
                employeeUser.getPasswordHash(),
                new Role(new RoleId(employeeUser.getRole().getId()), RoleCode.VT_04, employeeUser.getRole().getName()),
                UserStatus.ACTIVE,
                new EmployeeId(employee.getId()),
                DataScope.SELF,
                null,
                0L
        );
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("VT-04"),
                new SimpleGrantedAuthority("ROLE_VT-04"),
                new SimpleGrantedAuthority("LEAVE_BALANCE_READ")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken authForManager() {
        User principal = new User(
                new UserId(managerUser.getId()),
                managerUser.getUsername(),
                managerUser.getPasswordHash(),
                new Role(new RoleId(managerUser.getRole().getId()), RoleCode.VT_06, managerUser.getRole().getName()),
                UserStatus.ACTIVE,
                new EmployeeId(manager.getId()),
                DataScope.COMPANY,
                null,
                0L
        );
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("VT-06"),
                new SimpleGrantedAuthority("ROLE_VT-06"),
                new SimpleGrantedAuthority("LEAVE_BALANCE_READ"),
                new SimpleGrantedAuthority("LEAVE_BALANCE_MANAGE")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    @DisplayName("GET /api/v1/leaves/balances/me - Nhân viên lấy thành công số ngày phép còn lại (200 OK)")
    void testGetMyLeaveBalance_Success() throws Exception {
        mockMvc.perform(get("/api/v1/leaves/balances/me?year=2026")
                        .with(authentication(authForEmployee()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId", is(employee.getId().intValue())))
                .andExpect(jsonPath("$.data.year", is(2026)))
                .andExpect(jsonPath("$.data.remainingDays").value(14.0));
    }

    @Test
    @DisplayName("GET /api/v1/leaves/balances/employees/{id} - Quản lý xem được quỹ phép cấp dưới (200 OK)")
    void testGetEmployeeLeaveBalance_AsManager_Success() throws Exception {
        mockMvc.perform(get("/api/v1/leaves/balances/employees/{id}?year=2026", employee.getId())
                        .with(authentication(authForManager()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId", is(employee.getId().intValue())))
                .andExpect(jsonPath("$.data.remainingDays").value(14.0));
    }

    @Test
    @DisplayName("GET /api/v1/leaves/balances/employees/{id} - Nhân viên không có quyền xem người khác -> 403 Forbidden")
    void testGetEmployeeLeaveBalance_Unauthorized_Forbidden() throws Exception {
        // Nhân viên cố tình xem của Manager
        mockMvc.perform(get("/api/v1/leaves/balances/employees/{id}?year=2026", manager.getId())
                        .with(authentication(authForEmployee()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
