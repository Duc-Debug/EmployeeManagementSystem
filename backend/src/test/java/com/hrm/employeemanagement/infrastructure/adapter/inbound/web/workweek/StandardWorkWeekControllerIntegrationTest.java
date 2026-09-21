package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto.CapacityConversionRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto.StandardWorkWeekDayRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workweek.dto.UpdateStandardWorkWeekRequest;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-05-CN-008: Cấu hình đơn vị và tuần làm việc chuẩn Integration Tests")
class StandardWorkWeekControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

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

    private UserJpaEntity createUser(String usernamePrefix, String roleCode, DataScope dataScope) {
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
        return userRepository.saveAndFlush(user);
    }

    private UsernamePasswordAuthenticationToken authFor(UserJpaEntity user, RoleCode roleCode, DataScope dataScope, String... additionalAuthorities) {
        User principal = new User(
                new UserId(user.getId()),
                user.getUsername(),
                user.getPasswordHash(),
                new Role(new RoleId(user.getRole().getId()), roleCode, roleCode.getName()),
                UserStatus.ACTIVE,
                new EmployeeId(1L),
                dataScope,
                null,
                0L);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(roleCode.getCode()));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.getCode()));
        for (String auth : additionalAuthorities) {
            authorities.add(new SimpleGrantedAuthority(auth));
        }

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private List<StandardWorkWeekDayRequest> createStandardDayRequests(BigDecimal weekdayHours, BigDecimal saturdayHours) {
        return List.of(
                new StandardWorkWeekDayRequest(DayOfWeek.MONDAY, true, weekdayHours),
                new StandardWorkWeekDayRequest(DayOfWeek.TUESDAY, true, weekdayHours),
                new StandardWorkWeekDayRequest(DayOfWeek.WEDNESDAY, true, weekdayHours),
                new StandardWorkWeekDayRequest(DayOfWeek.THURSDAY, true, weekdayHours),
                new StandardWorkWeekDayRequest(DayOfWeek.FRIDAY, true, weekdayHours),
                new StandardWorkWeekDayRequest(DayOfWeek.SATURDAY, saturdayHours.compareTo(BigDecimal.ZERO) > 0, saturdayHours),
                new StandardWorkWeekDayRequest(DayOfWeek.SUNDAY, false, BigDecimal.ZERO)
        );
    }

    @Test
    @DisplayName("TC-01: Lấy cấu hình tuần làm việc chuẩn cấp công ty trả về 200 OK")
    void getConfig_company_success() throws Exception {
        UserJpaEntity hrUser = createUser("test-hr", "VT-05", DataScope.COMPANY);
        UsernamePasswordAuthenticationToken auth = authFor(hrUser, RoleCode.VT_05, DataScope.COMPANY, "STANDARD_WORK_WEEK_READ", "WORKING_CALENDAR_READ");

        mockMvc.perform(get("/api/v1/work-week-configs")
                        .with(authentication(auth))
                        .param("scopeType", "COMPANY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scopeType").value("COMPANY"))
                .andExpect(jsonPath("$.data.capacityUnit").isNotEmpty())
                .andExpect(jsonPath("$.data.days").isArray());
    }

    @Test
    @DisplayName("TC-02: HR cập nhật cấu hình tuần chuẩn thành công (44h/tuần)")
    void updateConfig_success() throws Exception {
        UserJpaEntity hrUser = createUser("test-hr-update", "VT-05", DataScope.COMPANY);
        UsernamePasswordAuthenticationToken auth = authFor(hrUser, RoleCode.VT_05, DataScope.COMPANY, "STANDARD_WORK_WEEK_MANAGE", "WORKING_CALENDAR_MANAGE");

        List<StandardWorkWeekDayRequest> days = createStandardDayRequests(BigDecimal.valueOf(8), BigDecimal.valueOf(4));
        UpdateStandardWorkWeekRequest request = new UpdateStandardWorkWeekRequest(
                "COMPANY",
                null,
                "HOURS",
                "MONDAY",
                BigDecimal.valueOf(8),
                days
        );

        mockMvc.perform(put("/api/v1/work-week-configs")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.standardHoursPerWeek").value(44.00));
    }

    @Test
    @DisplayName("TC-03: Cập nhật với dữ liệu không hợp lệ (> 12h/ngày) trả về 400 Bad Request")
    void updateConfig_invalidHours_badRequest() throws Exception {
        UserJpaEntity hrUser = createUser("test-hr-bad", "VT-05", DataScope.COMPANY);
        UsernamePasswordAuthenticationToken auth = authFor(hrUser, RoleCode.VT_05, DataScope.COMPANY, "STANDARD_WORK_WEEK_MANAGE", "WORKING_CALENDAR_MANAGE");

        List<StandardWorkWeekDayRequest> days = createStandardDayRequests(BigDecimal.valueOf(14), BigDecimal.ZERO);
        UpdateStandardWorkWeekRequest request = new UpdateStandardWorkWeekRequest(
                "COMPANY",
                null,
                "HOURS",
                "MONDAY",
                BigDecimal.valueOf(8),
                days
        );

        mockMvc.perform(put("/api/v1/work-week-configs")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STANDARD_WORK_WEEK"));
    }

    @Test
    @DisplayName("TC-04: Quy đổi đơn vị năng lực 20h sang FTE trả về 200 OK")
    void convertCapacity_success() throws Exception {
        UserJpaEntity hrUser = createUser("test-hr-conv", "VT-05", DataScope.COMPANY);
        UsernamePasswordAuthenticationToken auth = authFor(hrUser, RoleCode.VT_05, DataScope.COMPANY, "STANDARD_WORK_WEEK_READ", "WORKING_CALENDAR_READ");

        CapacityConversionRequest request = new CapacityConversionRequest(
                BigDecimal.valueOf(20.0),
                "HOURS",
                "FTE",
                "COMPANY",
                null
        );

        mockMvc.perform(post("/api/v1/work-week-configs/convert")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fromUnit").value("HOURS"))
                .andExpect(jsonPath("$.data.toUnit").value("FTE"));
    }

    @Test
    @DisplayName("TC-05: VT-04 không có quyền cập nhật cấu hình -> trả về 403 Forbidden")
    void updateConfig_forbiddenForStaff() throws Exception {
        UserJpaEntity staffUser = createUser("test-staff", "VT-04", DataScope.SELF);
        UsernamePasswordAuthenticationToken auth = authFor(staffUser, RoleCode.VT_04, DataScope.SELF, "EMPLOYEE_READ");

        List<StandardWorkWeekDayRequest> days = createStandardDayRequests(BigDecimal.valueOf(8), BigDecimal.ZERO);
        UpdateStandardWorkWeekRequest request = new UpdateStandardWorkWeekRequest(
                "COMPANY",
                null,
                "HOURS",
                "MONDAY",
                BigDecimal.valueOf(8),
                days
        );

        mockMvc.perform(put("/api/v1/work-week-configs")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
