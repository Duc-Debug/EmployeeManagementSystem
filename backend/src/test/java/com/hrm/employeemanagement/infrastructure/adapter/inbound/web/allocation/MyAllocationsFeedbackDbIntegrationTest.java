package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.ScheduleConfirmationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataScheduleConfirmationRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-13-CN-002 / QTN-24: Schedule Feedback DB Integration Test (confirmed_at IS NULL & status HAS_FEEDBACK)")
class MyAllocationsFeedbackDbIntegrationTest {

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
    private SpringDataScheduleConfirmationRepository scheduleConfirmationRepository;

    private UserJpaEntity testUserEntity;
    private EmployeeJpaEntity testEmployeeEntity;
    private UsernamePasswordAuthenticationToken authToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        String suffix = String.valueOf(System.nanoTime());
        RoleJpaEntity role = roleRepository.findByCode("VT-04")
                .orElseGet(() -> {
                    RoleJpaEntity r = new RoleJpaEntity();
                    r.setCode("VT-04");
                    r.setName("Nhân sự chuyên môn");
                    return roleRepository.saveAndFlush(r);
                });

        testUserEntity = new UserJpaEntity(
                null,
                "test_feedback_user_" + suffix,
                "$2a$10$dummyPasswordHashForIntegrationTest1234567890123456789012",
                role,
                true
        );
        testUserEntity.setDataScope("SELF");
        testUserEntity.setTokenVersion(1);
        testUserEntity = userRepository.saveAndFlush(testUserEntity);

        testEmployeeEntity = new EmployeeJpaEntity(
                null,
                testUserEntity.getId(),
                null,
                "EMP-FB-" + suffix,
                "Test Feedback Employee",
                "DEVELOPER",
                LocalDate.of(2025, 1, 1),
                null,
                false,
                40,
                "ACTIVE"
        );
        testEmployeeEntity = employeeRepository.saveAndFlush(testEmployeeEntity);

        User principal = new User(
                new UserId(testUserEntity.getId()),
                testUserEntity.getUsername(),
                testUserEntity.getPasswordHash(),
                new Role(new RoleId(role.getId()), RoleCode.VT_04, role.getName()),
                UserStatus.ACTIVE,
                new EmployeeId(testEmployeeEntity.getId()),
                DataScope.SELF,
                null,
                0L
        );

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_VT_04"));
        authorities.add(new SimpleGrantedAuthority("VT-04"));
        authorities.add(new SimpleGrantedAuthority("MY_ALLOCATION_READ"));
        authorities.add(new SimpleGrantedAuthority("MY_ALLOCATION_CONFIRM"));

        authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("End-to-End Review Blocker Verification: POST feedback lần đầu -> DB confirmed_at IS NULL -> GET schedule trả về HAS_FEEDBACK")
    void testProvideFeedback_WhenNoExistingConfirmation_KeepsConfirmedAtNull_AndReturnsHasFeedback() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        String reason = "Khối lượng công việc tuần này quá tải, đề xuất PM/RM xem xét phân bổ lại.";

        // 1. Verify initially: No confirmation record exists in DB for this user and week
        Optional<ScheduleConfirmationJpaEntity> beforeOpt = scheduleConfirmationRepository
                .findByUserIdAndWeekStartDate(testUserEntity.getId(), monday);
        assertThat(beforeOpt).isEmpty();

        // 2. POST /api/v1/my-allocations/{week_start}/feedback
        mockMvc.perform(post("/api/v1/my-allocations/2026-09-21/feedback")
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"" + reason + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.week_start_date").value("2026-09-21"))
                .andExpect(jsonPath("$.confirmation_status").value("HAS_FEEDBACK"))
                .andExpect(jsonPath("$.feedback_note").value(reason))
                .andExpect(jsonPath("$.feedback_at").exists());

        // 3. Verify DB: confirmed_at IS NULL, feedback_note and feedback_at are populated
        Optional<ScheduleConfirmationJpaEntity> dbRecordOpt = scheduleConfirmationRepository
                .findByUserIdAndWeekStartDate(testUserEntity.getId(), monday);
        assertThat(dbRecordOpt).isPresent();
        ScheduleConfirmationJpaEntity dbEntity = dbRecordOpt.get();
        assertThat(dbEntity.getConfirmedAt()).isNull();
        assertThat(dbEntity.getFeedbackNote()).isEqualTo(reason);
        assertThat(dbEntity.getFeedbackAt()).isNotNull();
        assertThat(dbEntity.getConfirmationStatus()).isEqualTo("HAS_FEEDBACK");

        // 4. GET /api/v1/my-allocations?week_start=2026-09-21&weeks=1
        mockMvc.perform(get("/api/v1/my-allocations?week_start=2026-09-21&weeks=1")
                        .with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks[0].week_start_date").value("2026-09-21"))
                // 5. Verify confirmation_status = HAS_FEEDBACK and confirmed_at is null
                .andExpect(jsonPath("$.weeks[0].confirmation_status").value("HAS_FEEDBACK"))
                .andExpect(jsonPath("$.weeks[0].confirmed_at", nullValue()))
                .andExpect(jsonPath("$.weeks[0].feedback_note").value(reason))
                .andExpect(jsonPath("$.weeks[0].feedback_at").exists());
    }
}