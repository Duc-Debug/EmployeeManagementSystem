package com.hrm.employeemanagement.application.service.conflict;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.conflict.ConfirmReplacementProposalCommand;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementProposalResult;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementSuggestionResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.entity.ScheduleConflictReplacementJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.conflict.repository.SpringDataScheduleConflictReplacementRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;

class ScheduleConflictReplacementServiceTest {

    private AuthorizationService authorizationService;
    private LoadScheduleConflictPort loadConflictPort;
    private SaveScheduleConflictPort saveConflictPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadSkillPort loadSkillPort;
    private SpringDataEmployeeSkillRepository employeeSkillRepository;
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private LoadApprovedLeavesPort loadApprovedLeavesPort;
    private SpringDataScheduleConflictReplacementRepository replacementRepository;
    private SaveAuditLogInNewTransactionPort auditLogPort;
    private SimulatedNotificationPort notificationPort;

    private ScheduleConflictReplacementService service;

    @BeforeEach
    void setUp() {
        authorizationService = Mockito.mock(AuthorizationService.class);
        loadConflictPort = Mockito.mock(LoadScheduleConflictPort.class);
        saveConflictPort = Mockito.mock(SaveScheduleConflictPort.class);
        loadEmployeePort = Mockito.mock(LoadEmployeePort.class);
        loadUserPort = Mockito.mock(LoadUserPort.class);
        loadOrgUnitPort = Mockito.mock(LoadOrgUnitPort.class);
        loadSkillPort = Mockito.mock(LoadSkillPort.class);
        employeeSkillRepository = Mockito.mock(SpringDataEmployeeSkillRepository.class);
        loadWeeklyAvailabilityPort = Mockito.mock(LoadWeeklyAvailabilityPort.class);
        loadAllocationPort = Mockito.mock(LoadWeeklyProjectAllocationPort.class);
        loadApprovedLeavesPort = Mockito.mock(LoadApprovedLeavesPort.class);
        replacementRepository = Mockito.mock(SpringDataScheduleConflictReplacementRepository.class);
        auditLogPort = Mockito.mock(SaveAuditLogInNewTransactionPort.class);
        notificationPort = Mockito.mock(SimulatedNotificationPort.class);

        service = new ScheduleConflictReplacementService(
                authorizationService,
                loadConflictPort,
                saveConflictPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                loadSkillPort,
                employeeSkillRepository,
                loadWeeklyAvailabilityPort,
                loadAllocationPort,
                loadApprovedLeavesPort,
                replacementRepository,
                auditLogPort,
                notificationPort
        );
    }

    @Test
    void testTC01_SuccessFlow_ReturnsTwoAvailableReplacementCandidates() {
        // NCL-07-CN-002-TC-01: Có xung đột ở một vai trò và hai người khác cùng kỹ năng đang rảnh -> Liệt kê 2 người kèm mức thành thạo & giờ rảnh
        Long rmUserId = 100L;
        Long conflictId = 1L;
        Long conflictedEmpId = 10L;
        Long skillId = 50L;

        when(authorizationService.requireAny(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST)).thenReturn(rmUserId);

        ScheduleConflict conflict = ScheduleConflict.create(
                conflictedEmpId, 2026, 38, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án A, Dự án B", null, null,
                new BigDecimal("48.0"), new BigDecimal("40.0"), new BigDecimal("8.0"), "Overload"
        );
        when(loadConflictPort.findById(conflictId)).thenReturn(Optional.of(conflict));

        Employee conflictedEmp = new Employee(
                new EmployeeId(conflictedEmpId), new UserId(1000L), 1L, "NV010", "Nguyễn Văn A",
                "Developer", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(conflictedEmpId))).thenReturn(Optional.of(conflictedEmp));

        // Skill
        Skill skill = new Skill(skillId, "SKILL-JAVA", "Java Backend", "Backend", "Descr", LocalDateTime.now());
        when(loadSkillPort.findById(new SkillId(skillId))).thenReturn(Optional.of(skill));

        // Emp skill for conflicted employee
        EmployeeSkillJpaEntity origSkill = new EmployeeSkillJpaEntity();
        origSkill.setEmployeeId(conflictedEmpId);
        origSkill.setSkillId(skillId);
        origSkill.setProficiencyLevel(3);
        when(employeeSkillRepository.findByEmployeeId(conflictedEmpId)).thenReturn(List.of(origSkill));

        // Matching candidates with same skill (>= 3)
        EmployeeSkillJpaEntity cand1Skill = new EmployeeSkillJpaEntity();
        cand1Skill.setEmployeeId(20L);
        cand1Skill.setSkillId(skillId);
        cand1Skill.setProficiencyLevel(4);

        EmployeeSkillJpaEntity cand2Skill = new EmployeeSkillJpaEntity();
        cand2Skill.setEmployeeId(30L);
        cand2Skill.setSkillId(skillId);
        cand2Skill.setProficiencyLevel(3);

        when(employeeSkillRepository.findApprovedBySkillAndMinLevel(eq(skillId), eq(3)))
                .thenReturn(List.of(cand1Skill, cand2Skill));

        Employee cand1 = new Employee(new EmployeeId(20L), new UserId(2000L), 1L, "NV020", "Trần Văn B", "Developer", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee cand2 = new Employee(new EmployeeId(30L), new UserId(3000L), 1L, "NV030", "Lê Thị C", "Developer", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);

        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(cand1, cand2));
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyMap());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyList());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyList());

        ReplacementSuggestionResult result = service.getReplacementSuggestions(conflictId, skillId, 3);

        assertNotNull(result);
        assertTrue(result.hasAvailableReplacements());
        assertEquals(2, result.candidates().size());
        assertEquals("Trần Văn B", result.candidates().get(0).fullName());
        assertEquals("Lê Thị C", result.candidates().get(1).fullName());
        assertEquals(0, new BigDecimal("40.0").compareTo(result.candidates().get(0).freeHours()));
    }

    @Test
    void testTC02_EmptyData_NoCandidatesAvailable_ReturnsRescheduleSuggestion() {
        // NCL-07-CN-002-TC-02: Không ai cùng kỹ năng còn rảnh -> Hệ thống báo không tìm được người thay thế và gợi ý dời lịch
        Long rmUserId = 100L;
        Long conflictId = 2L;
        Long conflictedEmpId = 10L;
        Long skillId = 99L;

        when(authorizationService.requireAny(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST)).thenReturn(rmUserId);

        ScheduleConflict conflict = ScheduleConflict.create(
                conflictedEmpId, 2026, 38, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án X", null, null,
                new BigDecimal("50.0"), new BigDecimal("40.0"), new BigDecimal("10.0"), "Overload"
        );
        when(loadConflictPort.findById(conflictId)).thenReturn(Optional.of(conflict));

        Employee conflictedEmp = new Employee(
                new EmployeeId(conflictedEmpId), new UserId(1000L), 1L, "NV010", "Nguyễn Văn A",
                "Architect", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(conflictedEmpId))).thenReturn(Optional.of(conflictedEmp));

        when(employeeSkillRepository.findApprovedBySkillAndMinLevel(eq(skillId), any(Integer.class)))
                .thenReturn(Collections.emptyList());

        ReplacementSuggestionResult result = service.getReplacementSuggestions(conflictId, skillId, 4);

        assertNotNull(result);
        assertFalse(result.hasAvailableReplacements());
        assertTrue(result.candidates().isEmpty());
        assertTrue(result.recommendationMessage().contains("Gợi ý: Dời lịch phân bổ công việc"));
    }

    @Test
    void testTC03_NoPermission_ThrowsPermissionDeniedExceptionAndLogsAudit() {
        // NCL-07-CN-002-TC-03: Người dùng không thuộc Quản lý nguồn lực -> Từ chối truy cập và ghi nhật ký lần từ chối
        Long conflictId = 3L;
        when(authorizationService.requireAny(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST));

        assertThrows(PermissionDeniedException.class, () -> {
            service.getReplacementSuggestions(conflictId, null, null);
        });

        verify(auditLogPort).save(any());
    }

    @Test
    void testTC04_SaveHistory_ConfirmProposal_SavesHistoryAndNotification() {
        // NCL-07-CN-002-TC-04: Xác nhận thao tác -> Ghi lại người thực hiện, nội dung và thời điểm trong Nhật ký thông báo / Audit log
        Long rmUserId = 100L;
        Long conflictId = 4L;
        Long conflictedEmpId = 10L;
        Long replacementEmpId = 20L;
        Long skillId = 50L;

        when(authorizationService.requireAny(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST)).thenReturn(rmUserId);

        ScheduleConflict conflict = ScheduleConflict.create(
                conflictedEmpId, 2026, 38, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1", "Dự án Alpha", null, null,
                new BigDecimal("48.0"), new BigDecimal("40.0"), new BigDecimal("8.0"), "Overload"
        );
        when(loadConflictPort.findById(conflictId)).thenReturn(Optional.of(conflict));

        Employee origEmp = new Employee(new EmployeeId(conflictedEmpId), new UserId(1000L), 1L, "NV010", "Nguyễn Văn A", "Dev", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee replEmp = new Employee(new EmployeeId(replacementEmpId), new UserId(2000L), 1L, "NV020", "Trần Văn B", "Dev", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);

        when(loadEmployeePort.findById(new EmployeeId(conflictedEmpId))).thenReturn(Optional.of(origEmp));
        when(loadEmployeePort.findById(new EmployeeId(replacementEmpId))).thenReturn(Optional.of(replEmp));

        Skill skill = new Skill(skillId, "SKILL-JAVA", "Java Backend", "Backend", "Descr", LocalDateTime.now());
        when(loadSkillPort.findById(new SkillId(skillId))).thenReturn(Optional.of(skill));

        User rmUser = Mockito.mock(User.class);
        when(rmUser.getUsername()).thenReturn("rm_manager");
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));

        ScheduleConflictReplacementJpaEntity savedEntity = new ScheduleConflictReplacementJpaEntity();
        savedEntity.setId(99L);
        when(replacementRepository.save(any())).thenReturn(savedEntity);

        ConfirmReplacementProposalCommand command = new ConfirmReplacementProposalCommand(
                conflictId, replacementEmpId, skillId, 4, "Thay thế do NV010 bị trùng dự án Alpha"
        );

        ReplacementProposalResult result = service.confirmReplacementProposal(command);

        assertNotNull(result);
        assertEquals(99L, result.proposalId());
        assertEquals("Nguyễn Văn A", result.originalEmployeeName());
        assertEquals("Trần Văn B", result.replacementEmployeeName());

        verify(notificationPort).sendReplacementSuggestionNotification(any(), any(), any(), any(), any());
        verify(auditLogPort).save(any());
    }
}
