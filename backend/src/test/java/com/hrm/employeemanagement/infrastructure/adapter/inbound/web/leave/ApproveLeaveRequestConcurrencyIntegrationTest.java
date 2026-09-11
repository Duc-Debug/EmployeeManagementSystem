package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.ApproveLeaveWebRequest;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.LeaveRequestJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.WeeklyAvailabilityJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataLeaveRequestRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataWeeklyAvailabilityRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-05-CN-003: Concurrency Integration Test for Leave Request Approval")
class ApproveLeaveRequestConcurrencyIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataLeaveRequestRepository leaveRequestRepository;

    @Autowired
    private SpringDataWeeklyAvailabilityRepository weeklyAvailabilityRepository;

    @Autowired
    private SpringDataAuditLogRepository auditLogRepository;

    private UserJpaEntity rmUser;
    private EmployeeJpaEntity rmEmployee;
    private UserJpaEntity staffUser;
    private EmployeeJpaEntity staffEmployee;
    private Long orgId;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        orgId = 1L;

        RoleJpaEntity rmRole = roleRepository.findByCode("VT-03").orElseGet(() -> {
            RoleJpaEntity r = new RoleJpaEntity();
            r.setCode("VT-03");
            r.setName("Quản lý nguồn lực");
            return roleRepository.save(r);
        });

        rmUser = new UserJpaEntity(
                null,
                "rm-conc-" + System.nanoTime(),
                "$2a$10$dummyHashPlaceholderForTest123456789012345678901234567890",
                rmRole,
                true
        );
        rmUser.setDataScope(DataScope.ORGANIZATION_BRANCH.name());
        rmUser.setScopeOrgUnitId(orgId);
        rmUser = userRepository.saveAndFlush(rmUser);

        rmEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, rmUser.getId(), orgId, "EMP-RM-CONC-" + System.nanoTime(),
                "Resource Manager Conc", false, 40, "ACTIVE"
        ));

        RoleJpaEntity staffRole = roleRepository.findByCode("VT-04").orElseGet(() -> {
            RoleJpaEntity r = new RoleJpaEntity();
            r.setCode("VT-04");
            r.setName("Nhân viên chuyên môn");
            return roleRepository.save(r);
        });

        staffUser = new UserJpaEntity(
                null,
                "staff-conc-" + System.nanoTime(),
                "$2a$10$dummyHashPlaceholderForTest123456789012345678901234567890",
                staffRole,
                true
        );
        staffUser.setDataScope(DataScope.SELF.name());
        staffUser = userRepository.saveAndFlush(staffUser);

        staffEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, staffUser.getId(), orgId, "EMP-STAFF-CONC-" + System.nanoTime(),
                "Staff Conc", false, 40, "ACTIVE"
        ));
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        weeklyAvailabilityRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        if (staffEmployee != null) employeeRepository.deleteById(staffEmployee.getId());
        if (rmEmployee != null) employeeRepository.deleteById(rmEmployee.getId());
        if (staffUser != null) {
            auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                    .filter(a -> staffUser.getId().equals(a.getUserId())).toList());
            userRepository.deleteById(staffUser.getId());
        }
        if (rmUser != null) {
            auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                    .filter(a -> rmUser.getId().equals(a.getUserId())).toList());
            userRepository.deleteById(rmUser.getId());
        }
    }

    private UsernamePasswordAuthenticationToken authForRM() {
        User principal = new User(
                new UserId(rmUser.getId()),
                rmUser.getUsername(),
                rmUser.getPasswordHash(),
                new Role(new RoleId(rmUser.getRole().getId()), RoleCode.VT_03, "Quản lý nguồn lực"),
                UserStatus.ACTIVE,
                new EmployeeId(rmEmployee.getId()),
                DataScope.ORGANIZATION_BRANCH,
                rmUser.getScopeOrgUnitId(),
                0L
        );
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("VT-03"),
                new SimpleGrantedAuthority("ROLE_VT-03"),
                new SimpleGrantedAuthority("LEAVE_REQUEST_APPROVE")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    @DisplayName("Duyệt đồng thời 1 đơn nghỉ phép -> Chỉ 1 request 200 OK thành công, request còn lại bị từ chối 400 Bad Request (ILLEGAL_STATE)")
    void testConcurrentApproveLeaveRequest_ShouldAllowOnlyOneAndRejectOther() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 11, 2);
        LocalDate endDate = LocalDate.of(2026, 11, 4);
        LeaveRequestJpaEntity leave = new LeaveRequestJpaEntity(
                null,
                staffEmployee.getId(),
                "ANNUAL",
                startDate,
                endDate,
                "PENDING",
                BigDecimal.valueOf(24.00),
                "Nghỉ phép thường niên kiểm tra concurrency",
                null,
                null,
                LocalDateTime.now(),
                null
        );
        leave = leaveRequestRepository.saveAndFlush(leave);
        final Long leaveId = leave.getId();

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIdx = i;
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await(); // Cả 2 thread đồng loạt bắn request

                ApproveLeaveWebRequest body = new ApproveLeaveWebRequest("Đồng ý duyệt từ thread " + threadIdx);
                MvcResult result = mockMvc.perform(put("/api/v1/leave-requests/" + leaveId + "/approve")
                                .with(authentication(authForRM()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                        .andReturn();

                int status = result.getResponse().getStatus();
                if (status == 200) {
                    successCount.incrementAndGet();
                } else if (status == 400) {
                    failCount.incrementAndGet();
                }
                return status;
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Bắt đầu chạy đồng thời

        for (Future<Integer> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executorService.shutdown();

        // 1. Kiểm tra HTTP response: chính xác 1 request thành công và 1 request thất bại vì trạng thái không còn PENDING
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        // 2. Database: đơn có trạng thái APPROVED
        LeaveRequestJpaEntity updatedLeave = leaveRequestRepository.findById(leaveId).orElseThrow();
        assertThat(updatedLeave.getStatus()).isEqualTo("APPROVED");
        assertThat(updatedLeave.getApproverId()).isEqualTo(rmUser.getId());

        // 3. Audit Log: chỉ có đúng 1 bản ghi audit phê duyệt cho đơn này, không bị nhân đôi
        List<AuditLogJpaEntity> auditLogs = auditLogRepository.findAll().stream()
                .filter(a -> "APPROVE_LEAVE_REQUEST".equals(a.getAction())
                        && "leave_requests".equals(a.getTableName())
                        && rmUser.getId().equals(a.getUserId()))
                .toList();
        assertThat(auditLogs).hasSize(1);

        // 4. Giờ khả dụng tuần: chỉ bị trừ 1 lần duy nhất (24.00h), netAvailableHours = 16.00h
        Optional<WeeklyAvailabilityJpaEntity> availabilityOpt = weeklyAvailabilityRepository
                .findByEmployeeIdAndYearAndWeekNumber(staffEmployee.getId(), 2026, 45);
        assertThat(availabilityOpt).isPresent();
        assertThat(availabilityOpt.get().getApprovedLeaveHours()).isEqualByComparingTo(BigDecimal.valueOf(24.00));
        assertThat(availabilityOpt.get().getNetAvailableHours()).isEqualByComparingTo(BigDecimal.valueOf(16.00));
    }
}
