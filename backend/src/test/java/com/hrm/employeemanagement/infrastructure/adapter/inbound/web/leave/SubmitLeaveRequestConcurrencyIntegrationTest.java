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
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-05-CN-002: Concurrency Integration Test for Submit Leave Request")
class SubmitLeaveRequestConcurrencyIntegrationTest {

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
    private SpringDataLeaveRequestRepository leaveRequestRepository;

    private UserJpaEntity testUser;
    private EmployeeJpaEntity testEmployee;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        RoleJpaEntity role = roleRepository.findByCode("VT-04").orElseGet(() -> {
            RoleJpaEntity r = new RoleJpaEntity();
            r.setCode("VT-04");
            r.setName("Nhân viên chuyên môn");
            return roleRepository.save(r);
        });

        testUser = new UserJpaEntity(
                null,
                "emp-concurrent-" + System.nanoTime(),
                "$2a$10$dummyHashPlaceholderForTest123456789012345678901234567890",
                role,
                true
        );
        testUser.setDataScope(DataScope.SELF.name());
        testUser = userRepository.saveAndFlush(testUser);

        testEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, testUser.getId(), null, "EMP-CONC-" + System.nanoTime(),
                "Nhân viên Concurrent", false, 40, "ACTIVE"
        ));
    }

    @Autowired
    private com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository auditLogRepository;

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        if (testEmployee != null) {
            leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(testEmployee.getId())
                    .forEach(lr -> leaveRequestRepository.deleteById(lr.getId()));
            employeeRepository.deleteById(testEmployee.getId());
        }
        if (testUser != null) {
            auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                    .filter(a -> testUser.getId().equals(a.getUserId()))
                    .toList());
            userRepository.deleteById(testUser.getId());
        }
    }

    private UsernamePasswordAuthenticationToken authForEmployee() {
        User principal = new User(
                new UserId(testUser.getId()),
                testUser.getUsername(),
                testUser.getPasswordHash(),
                new Role(new RoleId(testUser.getRole().getId()), RoleCode.VT_04, "Nhân viên chuyên môn"),
                UserStatus.ACTIVE,
                new EmployeeId(testEmployee.getId()),
                DataScope.SELF,
                null,
                0L
        );
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("VT-04"),
                new SimpleGrantedAuthority("ROLE_VT-04"),
                new SimpleGrantedAuthority("LEAVE_REQUEST_CREATE")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    @DisplayName("Gửi 2 đơn nghỉ phép trùng lặp đồng thời -> Chỉ 1 đơn 201 thành công và 1 đơn 409 xung đột")
    void testConcurrentSubmitLeaveRequests_ShouldAllowOnlyOneAndRejectOtherWith409() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        String jsonPayload = """
                {
                    "leaveType": "ANNUAL",
                    "startDate": "2026-11-02",
                    "endDate": "2026-11-04",
                    "reason": "Nghỉ phép thường niên test concurrency"
                }
                """;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await(); // Cả 2 thread cùng bắn request tại cùng 1 thời điểm

                MvcResult result = mockMvc.perform(post("/api/v1/leave-requests")
                                .with(authentication(authForEmployee()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonPayload))
                        .andReturn();

                int status = result.getResponse().getStatus();
                if (status == 201) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    conflictCount.incrementAndGet();
                }
                return status;
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Phát lệnh chạy đồng thời

        for (Future<Integer> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executorService.shutdown();

        // Kiểm tra kết quả: chính xác 1 request thành công và 1 request trả về lỗi xung đột trùng lặp (409)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Database chỉ có duy nhất 1 bản ghi
        var savedLeaves = leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(testEmployee.getId());
        assertThat(savedLeaves).hasSize(1);
    }
}
