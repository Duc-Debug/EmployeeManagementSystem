package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.ApproveLeaveWebRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.RejectLeaveWebRequest;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.entity.LeaveRequestJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.availability.repository.SpringDataLeaveRequestRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-05-CN-003: Integration & Security Tests for Leave Approval")
class ApproveLeaveRequestIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataLeaveRequestRepository leaveRequestRepository;

    @Autowired
    private SpringDataProjectRepository projectRepository;

    @Autowired
    private SpringDataWeeklyProjectAllocationRepository allocationRepository;

    @Autowired
    private SpringDataAuditLogRepository auditLogRepository;

    @Autowired
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    @Autowired
    private com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository orgUnitRepository;

    private UserJpaEntity rmUser;
    private EmployeeJpaEntity rmEmployee;

    private UserJpaEntity staffUser;
    private EmployeeJpaEntity staffEmployee;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        Long orgId = orgUnitRepository.findByUnitCode("COMPANY_ROOT")
                .map(com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity::getId)
                .orElse(1L);

        // 1. Tạo vai trò VT-03 (Quản lý nguồn lực)
        RoleJpaEntity rmRole = roleRepository.findByCode("VT-03").orElseGet(() -> {
            RoleJpaEntity r = new RoleJpaEntity();
            r.setCode("VT-03");
            r.setName("Quản lý nguồn lực");
            return roleRepository.save(r);
        });

        rmUser = new UserJpaEntity(
                null,
                "rm-test-" + System.nanoTime(),
                "$2a$10$dummyHashPlaceholderForTest123456789012345678901234567890",
                rmRole,
                true
        );
        rmUser.setDataScope(DataScope.ORGANIZATION_BRANCH.name());
        rmUser.setScopeOrgUnitId(orgId);
        rmUser = userRepository.saveAndFlush(rmUser);

        rmEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, rmUser.getId(), orgId, "EMP-RM-" + System.nanoTime(),
                "Resource Manager Test", false, 40, "ACTIVE"
        ));

        // 2. Tạo vai trò VT-04 (Nhân viên)
        RoleJpaEntity staffRole = roleRepository.findByCode("VT-04").orElseGet(() -> {
            RoleJpaEntity r = new RoleJpaEntity();
            r.setCode("VT-04");
            r.setName("Nhân viên chuyên môn");
            return roleRepository.save(r);
        });

        staffUser = new UserJpaEntity(
                null,
                "staff-test-" + System.nanoTime(),
                "$2a$10$dummyHashPlaceholderForTest123456789012345678901234567890",
                staffRole,
                true
        );
        staffUser.setDataScope(DataScope.SELF.name());
        staffUser = userRepository.saveAndFlush(staffUser);

        staffEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, staffUser.getId(), orgId, "EMP-STAFF-" + System.nanoTime(),
                "Staff Test", false, 40, "ACTIVE"
        ));
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        allocationRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        projectRepository.deleteAll();
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
                new SimpleGrantedAuthority("LEAVE_REQUEST_APPROVE"),
                new SimpleGrantedAuthority("LEAVE_REQUEST_VIEW")
        );
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken authForStaff() {
        User principal = new User(
                new UserId(staffUser.getId()),
                staffUser.getUsername(),
                staffUser.getPasswordHash(),
                new Role(new RoleId(staffUser.getRole().getId()), RoleCode.VT_04, "Nhân viên chuyên môn"),
                UserStatus.ACTIVE,
                new EmployeeId(staffEmployee.getId()),
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
    @DisplayName("TC-01 & QTN-10: Phê duyệt đơn thành công -> Cập nhật APPROVED và trừ giờ khả dụng tuần")
    void testApproveLeaveRequest_Success_ShouldUpdateStatusAndDeductAvailabilityHours_TC01_QTN10() throws Exception {
        // Tạo đơn xin nghỉ phép 3 ngày = 24 giờ (từ 02/11/2026 đến 04/11/2026 thuộc tuần 45/2026)
        LocalDate startDate = LocalDate.of(2026, 11, 2);
        LocalDate endDate = LocalDate.of(2026, 11, 4);
        LeaveRequestJpaEntity entity = new LeaveRequestJpaEntity(
                null,
                staffEmployee.getId(),
                "ANNUAL",
                startDate,
                endDate,
                "PENDING",
                BigDecimal.valueOf(24.00),
                "Xin nghỉ phép việc riêng",
                null,
                null,
                LocalDateTime.now(),
                null
        );
        entity = leaveRequestRepository.saveAndFlush(entity);

        // Trước khi duyệt: tổng giờ nghỉ đã duyệt trong tuần = 0
        BigDecimal hoursBefore = loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(staffEmployee.getId(), startDate, endDate);
        assertThat(hoursBefore).isEqualByComparingTo(BigDecimal.ZERO);

        // RM duyệt đơn
        ApproveLeaveWebRequest body = new ApproveLeaveWebRequest("Đồng ý cho nghỉ phép");
        mockMvc.perform(put("/api/v1/leave-requests/" + entity.getId() + "/approve")
                        .with(authentication(authForRM()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approverId").value(rmUser.getId()))
                .andExpect(jsonPath("$.data.approverComment").value("Đồng ý cho nghỉ phép"));

        // Sau khi duyệt: tổng giờ nghỉ đã duyệt theo QTN-10 được tính chính xác = 24.00 giờ
        BigDecimal hoursAfter = loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(staffEmployee.getId(), startDate, endDate);
        assertThat(hoursAfter).isEqualByComparingTo(BigDecimal.valueOf(24.00));
    }

    @Test
    @DisplayName("TC-02: Kiểm tra tác động dự án -> Cảnh báo xung đột nếu có giờ phân bổ trong tuần nghỉ")
    void testGetLeaveImpact_WhenEmployeeHasAllocations_ShouldWarnConflict_TC02() throws Exception {
        // Tạo 1 dự án và phân bổ 30h cho nhân viên vào tuần 45/2026
        ProjectJpaEntity project = new ProjectJpaEntity(
                null, "PRJ-TEST-LEAVE", "Dự án Nâng cấp Hệ thống", 1L, rmEmployee.getId(),
                ProjectStatus.ACTIVE, rmUser.getId(), LocalDateTime.now(), null, 0L
        );
        project = projectRepository.saveAndFlush(project);

        WeeklyProjectAllocationJpaEntity alloc = new WeeklyProjectAllocationJpaEntity(
                null, staffEmployee.getId(), project.getId(), 2026, 45, BigDecimal.valueOf(30.00), 0L
        );
        allocationRepository.saveAndFlush(alloc);

        // Đơn xin nghỉ trong tuần 45/2026
        LeaveRequestJpaEntity leave = new LeaveRequestJpaEntity(
                null, staffEmployee.getId(), "ANNUAL",
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4), "PENDING",
                BigDecimal.valueOf(24.00), "Xin nghỉ", null, null,
                LocalDateTime.now(), null
        );
        leave = leaveRequestRepository.saveAndFlush(leave);

        // Gọi API xem tác động
        mockMvc.perform(get("/api/v1/leave-requests/" + leave.getId() + "/impact")
                        .with(authentication(authForRM())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasConflict").value(true))
                .andExpect(jsonPath("$.data.totalAllocatedHoursInLeavePeriod").value(30.00))
                .andExpect(jsonPath("$.data.affectedProjects[0].projectName").value("Dự án Nâng cấp Hệ thống"))
                .andExpect(jsonPath("$.data.affectedProjects[0].allocatedHours").value(30.00));
    }

    @Test
    @DisplayName("TC-03: Phân quyền RBAC -> Nhân viên (VT-04) bị chặn 403 Forbidden khi duyệt hoặc xem tác động")
    void testLeaveApprovalEndpoints_Security_VT04ShouldBeForbidden_TC03() throws Exception {
        mockMvc.perform(get("/api/v1/leave-requests/pending")
                        .with(authentication(authForStaff())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/leave-requests/1/impact")
                        .with(authentication(authForStaff())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/leave-requests/1/approve")
                        .with(authentication(authForStaff()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/leave-requests/1/reject")
                        .with(authentication(authForStaff()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Lý do\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-04: Từ chối đơn phải có lý do (400 nếu thiếu) & Ghi nhận vào audit_logs")
    void testRejectLeaveRequest_AndAuditLog_TC04() throws Exception {
        LeaveRequestJpaEntity leave = new LeaveRequestJpaEntity(
                null, staffEmployee.getId(), "ANNUAL",
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4), "PENDING",
                BigDecimal.valueOf(24.00), "Xin nghỉ", null, null,
                LocalDateTime.now(), null
        );
        leave = leaveRequestRepository.saveAndFlush(leave);

        // 1. Thử từ chối không kèm lý do -> 400 Bad Request
        RejectLeaveWebRequest invalidBody = new RejectLeaveWebRequest("");
        mockMvc.perform(put("/api/v1/leave-requests/" + leave.getId() + "/reject")
                        .with(authentication(authForRM()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest());

        // 2. Từ chối hợp lệ kèm lý do
        RejectLeaveWebRequest validBody = new RejectLeaveWebRequest("Dự án đang trong tuần cao điểm release");
        mockMvc.perform(put("/api/v1/leave-requests/" + leave.getId() + "/reject")
                        .with(authentication(authForRM()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.approverComment").value("Dự án đang trong tuần cao điểm release"));

        // 3. Kiểm tra Audit Log được lưu
        List<AuditLogJpaEntity> logs = auditLogRepository.findAll().stream()
                .filter(a -> "REJECT_LEAVE_REQUEST".equals(a.getAction()) && "leave_requests".equals(a.getTableName()))
                .toList();
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getUserId()).isEqualTo(rmUser.getId());
    }
}
