package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.billablerate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BillableRate Authorization & Audit Integration Test (NCL-10-CN-002-TC-03)")
class BillableRateAuthorizationAuditIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private SpringDataRoleRepository roleRepository;
    @Autowired private SpringDataUserRepository userRepository;
    @Autowired private SpringDataAuditLogRepository auditLogRepository;

    private MockMvc mockMvc;
    private UserJpaEntity deniedUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        RoleJpaEntity role = roleRepository.findByCode("VT-04").orElseThrow();
        deniedUser = new UserJpaEntity(
                null,
                "billable-denied-" + System.nanoTime(),
                "hash",
                role,
                true
        );
        deniedUser.setDataScope(DataScope.SELF.name());
        deniedUser = userRepository.saveAndFlush(deniedUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (deniedUser != null && deniedUser.getId() != null) {
            auditLogRepository.deleteByUserId(deniedUser.getId());
            userRepository.deleteById(deniedUser.getId());
        }
    }

    @Test
    @DisplayName("NCL-10-CN-002-TC-03: Người dùng không thuộc vai trò được phép (VT-04) bị từ chối 403 và ghi nhận nhật ký PERMISSION_DENIED")
    void deniedRequestPersistsExactlyOnePermissionDeniedAuditRecord() throws Exception {
        mockMvc.perform(get("/api/v1/reports/billable-rate")
                        .with(authentication(authenticationFor(deniedUser))))
                .andExpect(status().isForbidden());

        List<AuditLogJpaEntity> userLogs = auditLogRepository.findByUserId(deniedUser.getId());

        assertThat(userLogs).hasSize(1);
        assertThat(userLogs.getFirst().getAction()).isEqualTo("PERMISSION_DENIED");
        assertThat(userLogs.getFirst().getNewValue()).contains("BILLABLE_HOURS_REPORT_READ");
    }

    private UsernamePasswordAuthenticationToken authenticationFor(UserJpaEntity userEntity) {
        RoleCode roleCode = RoleCode.VT_04;
        User principal = new User(
                new UserId(userEntity.getId()),
                userEntity.getUsername(),
                userEntity.getPasswordHash(),
                new Role(new RoleId(userEntity.getRole().getId()), roleCode, roleCode.getName()),
                UserStatus.ACTIVE,
                new EmployeeId(1L),
                DataScope.SELF,
                null,
                1L
        );
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(roleCode.getCode()))
        );
    }
}
