package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.reservation.dto.CreateReservationRequest;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation.entity.ResourceReservationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation.repository.SpringDataResourceReservationRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@DisplayName("ResourceReservation Concurrency & Data Integrity Integration Test (NCL-06-CN-005)")
class ResourceReservationConcurrencyIntegrationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataProjectRepository projectRepository;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Autowired
    private SpringDataWeeklyProjectAllocationRepository allocationRepository;

    @Autowired
    private SpringDataResourceReservationRepository reservationRepository;

    @Autowired
    private com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository auditLogRepository;

    private UserJpaEntity pmUser;
    private EmployeeJpaEntity pmEmployee;
    private EmployeeJpaEntity targetEmployee;
    private ProjectJpaEntity plannedProjectA;
    private ProjectJpaEntity plannedProjectB;
    private OrgUnitJpaEntity orgUnit;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        orgUnit = orgUnitRepository.findAll().stream().findFirst().orElseGet(() -> {
            OrgUnitJpaEntity ou = new OrgUnitJpaEntity();
            ou.setUnitCode("OU-CONC-" + System.nanoTime());
            ou.setUnitName("Org Unit Concurrency");
            ou.setStatus(com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus.ACTIVE);
            return orgUnitRepository.saveAndFlush(ou);
        });

        RoleJpaEntity pmRole = roleRepository.findByCode("VT-02").orElseGet(() -> {
            RoleJpaEntity r = new RoleJpaEntity();
            r.setCode("VT-02");
            r.setName("Quản lý dự án");
            return roleRepository.saveAndFlush(r);
        });

        pmUser = new UserJpaEntity(
                null,
                "pm-conc-" + System.nanoTime(),
                "$2a$10$dummyHashPlaceholderForTest123456789012345678901234567890",
                pmRole,
                true
        );
        pmUser.setDataScope(DataScope.SELF.name());
        pmUser = userRepository.saveAndFlush(pmUser);

        pmEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, pmUser.getId(), orgUnit.getId(), "PM-CONC-" + System.nanoTime(),
                "PM User Concurrency", false, 40, "ACTIVE"
        ));

        targetEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, null, orgUnit.getId(), "EMP-CONC-" + System.nanoTime(),
                "Target Employee Concurrency", false, 40, "ACTIVE"
        ));

        plannedProjectA = projectRepository.saveAndFlush(new ProjectJpaEntity(
                null, "PRJ-CONC-A-" + System.nanoTime(), "Planned Project A", orgUnit.getId(),
                pmEmployee.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(500), "Desc", ProjectStatus.PLANNED, pmUser.getId(),
                LocalDateTime.now(), null, 0L, 0
        ));

        plannedProjectB = projectRepository.saveAndFlush(new ProjectJpaEntity(
                null, "PRJ-CONC-B-" + System.nanoTime(), "Planned Project B", orgUnit.getId(),
                pmEmployee.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(500), "Desc", ProjectStatus.PLANNED, pmUser.getId(),
                LocalDateTime.now(), null, 0L, 0
        ));
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        if (targetEmployee != null) {
            reservationRepository.findAllByEmployeeIdAndYearNumberAndWeekNumberAndStatus(
                    targetEmployee.getId(), 2026, 40, ReservationStatus.ACTIVE
            ).forEach(r -> reservationRepository.deleteById(r.getId()));
            allocationRepository.deleteAll(allocationRepository.findByEmployeeIdAndYearAndWeekNumber(
                    targetEmployee.getId(), 2026, 40
            ));
            employeeRepository.deleteById(targetEmployee.getId());
        }
        if (plannedProjectA != null) {
            projectRepository.deleteById(plannedProjectA.getId());
        }
        if (plannedProjectB != null) {
            projectRepository.deleteById(plannedProjectB.getId());
        }
        if (pmEmployee != null) {
            employeeRepository.deleteById(pmEmployee.getId());
        }
        if (pmUser != null) {
            auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                    .filter(a -> pmUser.getId().equals(a.getUserId()))
                    .toList());
            userRepository.deleteById(pmUser.getId());
        }
    }

    private UsernamePasswordAuthenticationToken authForPm() {
        User principal = new User(
                new UserId(pmUser.getId()),
                pmUser.getUsername(),
                pmUser.getPasswordHash(),
                new Role(new RoleId(pmUser.getRole().getId()), RoleCode.VT_02, "Quản lý dự án"),
                UserStatus.ACTIVE,
                new EmployeeId(pmEmployee.getId()),
                DataScope.SELF,
                null,
                0L
        );
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("VT-02"),
                new SimpleGrantedAuthority("ROLE_VT-02"),
                new SimpleGrantedAuthority("RESOURCE_RESERVATION_CREATE"),
                new SimpleGrantedAuthority("RESOURCE_ALLOCATION_READ")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    @DisplayName("Concurrency Check: 2 request đồng thời xin giữ chỗ 15h khi còn rảnh 20h -> Chỉ 1 thành công, 1 bị từ chối")
    void testConcurrentReservations_ShouldPreventOverbooking() throws Exception {
        // Đã phân bổ 20h cho dự án khác -> Chỉ còn khả dụng 20h (40h - 20h)
        WeeklyProjectAllocationJpaEntity existingAlloc = new WeeklyProjectAllocationJpaEntity(
                null, targetEmployee.getId(), plannedProjectA.getId(), 2026, 40, BigDecimal.valueOf(20.00), pmUser.getId()
        );
        allocationRepository.saveAndFlush(existingAlloc);

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        CreateReservationRequest reqA = new CreateReservationRequest(
                plannedProjectA.getId(), targetEmployee.getId(), 2026, 40, BigDecimal.valueOf(15.0), "Req A 15h"
        );
        CreateReservationRequest reqB = new CreateReservationRequest(
                plannedProjectB.getId(), targetEmployee.getId(), 2026, 40, BigDecimal.valueOf(15.0), "Req B 15h"
        );

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<Future<Integer>> futures = new ArrayList<>();

        futures.add(executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            MvcResult result = mockMvc.perform(post("/api/v1/resource-reservations")
                            .with(authentication(authForPm()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqA)))
                    .andReturn();
            int status = result.getResponse().getStatus();
            if (status == 201) successCount.incrementAndGet();
            else rejectedCount.incrementAndGet();
            return status;
        }));

        futures.add(executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();
            MvcResult result = mockMvc.perform(post("/api/v1/resource-reservations")
                            .with(authentication(authForPm()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqB)))
                    .andReturn();
            int status = result.getResponse().getStatus();
            if (status == 201) successCount.incrementAndGet();
            else rejectedCount.incrementAndGet();
            return status;
        }));

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Cả 2 luồng cùng thực hiện đồng thời

        for (Future<Integer> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executorService.shutdown();

        // Kiểm tra kết quả: chính xác 1 request 201 Created và 1 request bị từ chối
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(rejectedCount.get()).isEqualTo(1);

        // Tổng số giờ giữ chỗ thực tế trong DB cho nhân sự này ở tuần 40 không bao giờ vượt quá capacity
        List<ResourceReservationJpaEntity> reservations = reservationRepository
                .findAllByEmployeeIdAndYearNumberAndWeekNumberAndStatus(targetEmployee.getId(), 2026, 40, ReservationStatus.ACTIVE);
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getReservedHours()).isEqualByComparingTo(BigDecimal.valueOf(15.0));
    }

    @Test
    @DisplayName("DB Unique Constraint: 2 request đồng thời tạo giữ chỗ cho cùng 1 slot -> Không bao giờ sinh ra 2 bản ghi ACTIVE")
    void testConcurrentSameSlotReservations_ShouldNotCreateDuplicateActiveRecords() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        CreateReservationRequest req = new CreateReservationRequest(
                plannedProjectA.getId(), targetEmployee.getId(), 2026, 40, BigDecimal.valueOf(10.0), "Same slot 10h"
        );

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/resource-reservations")
                                .with(authentication(authForPm()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andReturn();
                return result.getResponse().getStatus();
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<Integer> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executorService.shutdown();

        // Database tuyệt đối không có 2 bản ghi ACTIVE trùng lặp cho slot này
        List<ResourceReservationJpaEntity> activeSlots = reservationRepository
                .findAllByEmployeeIdAndYearNumberAndWeekNumberAndStatus(targetEmployee.getId(), 2026, 40, ReservationStatus.ACTIVE);
        assertThat(activeSlots).hasSize(1);
    }
}
