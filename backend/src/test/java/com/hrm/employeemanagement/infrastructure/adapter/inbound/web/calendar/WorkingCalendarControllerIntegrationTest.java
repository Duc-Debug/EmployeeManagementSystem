package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataHolidayRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.calendar.repository.SpringDataWorkingCalendarRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-05-CN-001: Khai báo lịch làm việc và ngày lễ Integration Tests (TC-01 -> TC-04)")
class WorkingCalendarControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataHolidayRepository holidayRepository;

    @Autowired
    private SpringDataWorkingCalendarRepository workingCalendarRepository;

    @Autowired
    private SpringDataAuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserJpaEntity createUser(String usernamePrefix, String roleCode, DataScope dataScope, Long scopeOrgUnitId) {
        RoleJpaEntity role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleCode));

        UserJpaEntity user = new UserJpaEntity(
                null,
                usernamePrefix + "-" + System.nanoTime(),
                "$2a$10$dummyHashPlaceholderForTest123456789012345678901234567890",
                role,
                true
        );
        user.setDataScope(dataScope.name());
        user.setScopeOrgUnitId(scopeOrgUnitId);
        return userRepository.saveAndFlush(user);
    }

    private UsernamePasswordAuthenticationToken authenticationFor(UserJpaEntity user, RoleCode roleCode, String... additionalAuthorities) {
        DataScope scope = user.getDataScope() != null ? DataScope.valueOf(user.getDataScope()) : DataScope.SELF;
        User principal = new User(
                new UserId(user.getId()),
                user.getUsername(),
                user.getPasswordHash(),
                new Role(new RoleId(user.getRole().getId()), roleCode, roleCode.getName()),
                UserStatus.ACTIVE,
                new EmployeeId(1L),
                scope,
                user.getScopeOrgUnitId(),
                0L);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(roleCode.getCode()));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.getCode()));
        for (String auth : additionalAuthorities) {
            authorities.add(new SimpleGrantedAuthority(auth));
        }

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken authenticationForEmployee(UserJpaEntity user) {
        return authenticationFor(user, RoleCode.VT_04, "WORKING_CALENDAR_READ", "EMPLOYEE_READ");
    }

    @Test
    @DisplayName("TC-01 & QTN-10: Thêm ngày lễ vào lịch năm -> Hệ thống lưu ngày lễ và giảm giờ khả dụng của tuần chứa ngày đó")
    void testTC01_AddHoliday_ReducesCapacityInTargetWeek() throws Exception {
        // Setup User HR (VT-05) và nhân viên
        UserJpaEntity hrUser = createUser("hr-tc01", "VT-05", DataScope.COMPANY, null);
        UserJpaEntity empUser = createUser("emp-tc01", "VT-04", DataScope.SELF, null);

        OrgUnitJpaEntity orgUnit = orgUnitRepository.findAll().stream().findFirst().orElse(null);
        Long orgUnitId = orgUnit != null ? orgUnit.getId() : null;

        EmployeeJpaEntity employee = employeeRepository.save(new EmployeeJpaEntity(
                null, empUser.getId(), orgUnitId, "EMP-TC01-" + System.nanoTime(),
                "Nguyễn Văn TC01", false, 40, "ACTIVE"
        ));

        // 1. Thêm ngày lễ ngày 2026-09-02 (Thứ Tư, thuộc Tuần 36 năm 2026) với 8 giờ khấu trừ
        LocalDate holidayDate = LocalDate.of(2026, 9, 2);
        // Xóa ngày lễ nếu đã tồn tại từ trước để test độc lập
        holidayRepository.findHolidaysBetween(holidayDate, holidayDate).forEach(h -> holidayRepository.deleteById(h.getId()));

        String createJson = """
                {
                    "holidayDate": "2026-09-02",
                    "name": "Lễ Quốc Khánh 02/09",
                    "workingHoursDeducted": 8
                }
                """;

        mockMvc.perform(post("/api/v1/holidays")
                        .with(authentication(authenticationFor(hrUser, RoleCode.VT_05)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Lễ Quốc Khánh 02/09"))
                .andExpect(jsonPath("$.data.workingHoursDeducted").value(8));

        // 2. Kiểm tra tính năng lực tuần chứa ngày lễ (Tuần 36 năm 2026) theo QTN-10:
        // Giờ chuẩn: 40h, Giờ lễ: 8h, Giờ phép: 0h -> Giờ khả dụng ròng = 32h
        mockMvc.perform(get("/api/v1/employees/{employeeId}/capacity", employee.getId())
                        .param("year", "2026")
                        .param("weekNumber", "36")
                        .with(authentication(authenticationFor(hrUser, RoleCode.VT_05))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standardHours").value(40))
                .andExpect(jsonPath("$.holidayHours").value(8))
                .andExpect(jsonPath("$.netAvailableHours").value(32.0));
    }

    @Test
    @DisplayName("TC-02: Thêm ngày lễ trùng lặp -> Hệ thống báo lỗi trùng 409 Conflict, không thêm lần thứ hai")
    void testTC02_DuplicateHoliday_Returns409Conflict() throws Exception {
        UserJpaEntity hrUser = createUser("hr-tc02", "VT-05", DataScope.COMPANY, null);
        LocalDate holidayDate = LocalDate.of(2026, 12, 25);

        holidayRepository.findHolidaysBetween(holidayDate, holidayDate).forEach(h -> holidayRepository.deleteById(h.getId()));

        String holidayJson = """
                {
                    "holidayDate": "2026-12-25",
                    "name": "Giáng Sinh",
                    "workingHoursDeducted": 8
                }
                """;

        // Thêm lần 1 thành công
        mockMvc.perform(post("/api/v1/holidays")
                        .with(authentication(authenticationFor(hrUser, RoleCode.VT_05)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holidayJson))
                .andExpect(status().isCreated());

        // Thêm lần 2 báo lỗi trùng (TC-02)
        mockMvc.perform(post("/api/v1/holidays")
                        .with(authentication(authenticationFor(hrUser, RoleCode.VT_05)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holidayJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_HOLIDAY"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("2026-12-25")));
    }

    @Test
    @DisplayName("TC-03: Người dùng không phải Nhân sự hoặc Quản trị viên (VT-04) -> Từ chối 403 và ghi nhật ký lần từ chối")
    void testTC03_UnauthorizedUser_Returns403AndPersistsDeniedAudit() throws Exception {
        // Tạo tài khoản Nhân viên chuyên môn (VT-04) không có quyền WORKING_CALENDAR_MANAGE
        UserJpaEntity empUser = createUser("emp-unauth", "VT-04", DataScope.SELF, null);

        String createJson = """
                {
                    "holidayDate": "2026-11-20",
                    "name": "Ngày Nhà giáo Việt Nam",
                    "workingHoursDeducted": 8
                }
                """;

        // Thao tác: Mở/gọi chức năng khai báo ngày lễ
        mockMvc.perform(post("/api/v1/holidays")
                        .with(authentication(authenticationForEmployee(empUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isForbidden());

        // Kết quả mong đợi: Hệ thống ghi nhật ký lần từ chối vào audit_logs
        List<AuditLogJpaEntity> deniedLogs = auditLogRepository.findAll().stream()
                .filter(log -> empUser.getId().equals(log.getUserId())
                        && "PERMISSION_DENIED".equals(log.getAction()))
                .toList();

        assertThat(deniedLogs).isNotEmpty();
        assertThat(deniedLogs.get(deniedLogs.size() - 1).getNewValue())
                .contains("WORKING_CALENDAR_MANAGE");
    }

    @Test
    @DisplayName("TC-04: Lưu lịch sử kiểm toán đầy đủ khi thay đổi lịch làm việc và ngày lễ")
    void testTC04_AuditLogRecordedOnChanges() throws Exception {
        UserJpaEntity adminUser = createUser("admin-tc04", "VT-06", DataScope.COMPANY, null);

        // 1. Thao tác thêm ngày lễ
        LocalDate date = LocalDate.of(2026, 4, 30);
        holidayRepository.findHolidaysBetween(date, date).forEach(h -> holidayRepository.deleteById(h.getId()));

        String holidayJson = """
                {
                    "holidayDate": "2026-04-30",
                    "name": "Giải phóng miền Nam",
                    "workingHoursDeducted": 8
                }
                """;

        mockMvc.perform(post("/api/v1/holidays")
                        .with(authentication(authenticationFor(adminUser, RoleCode.VT_06)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(holidayJson))
                .andExpect(status().isCreated());

        // Verify audit log CREATE_HOLIDAY
        List<AuditLogJpaEntity> createLogs = auditLogRepository.findAll().stream()
                .filter(log -> adminUser.getId().equals(log.getUserId())
                        && "CREATE_HOLIDAY".equals(log.getAction()))
                .toList();

        assertThat(createLogs).isNotEmpty();
        AuditLogJpaEntity lastCreateLog = createLogs.get(createLogs.size() - 1);
        assertThat(lastCreateLog.getTableName()).isEqualTo("holidays");
        assertThat(lastCreateLog.getNewValue()).contains("2026-04-30");

        // 2. Thao tác cập nhật lịch làm việc tuần
        String calendarJson = """
                {
                    "days": [
                        {"dayOfWeek": "MONDAY", "isWorkingDay": true},
                        {"dayOfWeek": "TUESDAY", "isWorkingDay": true},
                        {"dayOfWeek": "WEDNESDAY", "isWorkingDay": true},
                        {"dayOfWeek": "THURSDAY", "isWorkingDay": true},
                        {"dayOfWeek": "FRIDAY", "isWorkingDay": true},
                        {"dayOfWeek": "SATURDAY", "isWorkingDay": true},
                        {"dayOfWeek": "SUNDAY", "isWorkingDay": false}
                    ]
                }
                """;

        mockMvc.perform(put("/api/v1/working-calendar")
                        .with(authentication(authenticationFor(adminUser, RoleCode.VT_06)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(calendarJson))
                .andExpect(status().isOk());

        // Verify audit log UPDATE_WORKING_CALENDAR
        List<AuditLogJpaEntity> calendarLogs = auditLogRepository.findAll().stream()
                .filter(log -> adminUser.getId().equals(log.getUserId())
                        && "UPDATE_WORKING_CALENDAR".equals(log.getAction()))
                .toList();

        assertThat(calendarLogs).isNotEmpty();
        AuditLogJpaEntity lastCalLog = calendarLogs.get(calendarLogs.size() - 1);
        assertThat(lastCalLog.getTableName()).isEqualTo("working_calendar_configs");
        assertThat(lastCalLog.getNewValue()).contains("SATURDAY=true");
    }

    @Test
    @DisplayName("Lấy cấu hình lịch làm việc và danh sách ngày lễ theo năm thành công")
    void testGetCalendarAndHolidaysByYear() throws Exception {
        UserJpaEntity user = createUser("user-read", "VT-01", DataScope.COMPANY, null);

        mockMvc.perform(get("/api/v1/working-calendar")
                        .with(authentication(authenticationFor(user, RoleCode.VT_01))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days").isArray());

        mockMvc.perform(get("/api/v1/holidays")
                        .param("year", "2026")
                        .with(authentication(authenticationFor(user, RoleCode.VT_01))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
