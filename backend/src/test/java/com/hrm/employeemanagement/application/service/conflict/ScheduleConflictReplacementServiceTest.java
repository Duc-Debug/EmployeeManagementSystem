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
import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictReplacementPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictReplacement;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillId;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

class ScheduleConflictReplacementServiceTest {

    private AuthorizationService authorizationService;
    private LoadScheduleConflictPort loadConflictPort;
    private SaveScheduleConflictPort saveConflictPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private LoadSkillPort loadSkillPort;
    private EmployeeSkillRepository employeeSkillRepository;
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private LoadApprovedLeavesPort loadApprovedLeavesPort;
    private SaveScheduleConflictReplacementPort replacementPort;
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
        employeeSkillRepository = Mockito.mock(EmployeeSkillRepository.class);
        loadWeeklyAvailabilityPort = Mockito.mock(LoadWeeklyAvailabilityPort.class);
        loadAllocationPort = Mockito.mock(LoadWeeklyProjectAllocationPort.class);
        loadApprovedLeavesPort = Mockito.mock(LoadApprovedLeavesPort.class);
        replacementPort = Mockito.mock(SaveScheduleConflictReplacementPort.class);
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
                replacementPort,
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
        EmployeeSkill origSkill = new EmployeeSkill(
                1L, conflictedEmpId, skillId, 3, new BigDecimal("3.5"),
                SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeId(conflictedEmpId)).thenReturn(List.of(origSkill));

        // Matching candidates with same skill (>= 3)
        EmployeeSkill cand1Skill = new EmployeeSkill(
                2L, 20L, skillId, 4, new BigDecimal("5.0"),
                SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );

        EmployeeSkill cand2Skill = new EmployeeSkill(
                3L, 30L, skillId, 3, new BigDecimal("3.0"),
                SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );

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

        // Skill approved for candidate
        EmployeeSkill candSkill = new EmployeeSkill(
                2L, replacementEmpId, skillId, 4, new BigDecimal("5.0"),
                SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeIdAndSkillId(replacementEmpId, skillId)).thenReturn(Optional.of(candSkill));

        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyMap());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyList());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyList());

        Skill skill = new Skill(skillId, "SKILL-JAVA", "Java Backend", "Backend", "Descr", LocalDateTime.now());
        when(loadSkillPort.findById(new SkillId(skillId))).thenReturn(Optional.of(skill));

        User rmUser = Mockito.mock(User.class);
        when(rmUser.getUsername()).thenReturn("rm_manager");
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));

        ScheduleConflictReplacement savedDomain = new ScheduleConflictReplacement(
                99L, conflictId, conflictedEmpId, replacementEmpId, skillId, 4,
                new BigDecimal("40.0"), "PROPOSED", "Thay thế do NV010 bị trùng dự án Alpha",
                rmUserId, LocalDateTime.now()
        );
        when(replacementPort.save(any())).thenReturn(savedDomain);

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

    @Test
    void testConfirmProposal_InactiveCandidate_ThrowsIllegalArgumentException() {
        Long rmUserId = 100L;
        Long conflictId = 5L;
        Long conflictedEmpId = 10L;
        Long replacementEmpId = 20L;

        when(authorizationService.requireAny(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST)).thenReturn(rmUserId);

        ScheduleConflict conflict = ScheduleConflict.create(
                conflictedEmpId, 2026, 38, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1", "Dự án Alpha", null, null,
                new BigDecimal("48.0"), new BigDecimal("40.0"), new BigDecimal("8.0"), "Overload"
        );
        when(loadConflictPort.findById(conflictId)).thenReturn(Optional.of(conflict));

        Employee origEmp = new Employee(new EmployeeId(conflictedEmpId), new UserId(1000L), 1L, "NV010", "Nguyễn Văn A", "Dev", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee inactiveReplEmp = new Employee(new EmployeeId(replacementEmpId), new UserId(2000L), 1L, "NV020", "Trần Văn B", "Dev", LocalDate.now(), null, false, 40, EmployeeStatus.TERMINATED);

        when(loadEmployeePort.findById(new EmployeeId(conflictedEmpId))).thenReturn(Optional.of(origEmp));
        when(loadEmployeePort.findById(new EmployeeId(replacementEmpId))).thenReturn(Optional.of(inactiveReplEmp));

        ConfirmReplacementProposalCommand command = new ConfirmReplacementProposalCommand(
                conflictId, replacementEmpId, 50L, 3, "Notes"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.confirmReplacementProposal(command));
        assertTrue(ex.getMessage().contains("ACTIVE"));
    }

    @Test
    void testConfirmProposal_SameConflictedEmployee_ThrowsIllegalArgumentException() {
        Long rmUserId = 100L;
        Long conflictId = 6L;
        Long conflictedEmpId = 10L;

        when(authorizationService.requireAny(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST)).thenReturn(rmUserId);

        ScheduleConflict conflict = ScheduleConflict.create(
                conflictedEmpId, 2026, 38, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1", "Dự án Alpha", null, null,
                new BigDecimal("48.0"), new BigDecimal("40.0"), new BigDecimal("8.0"), "Overload"
        );
        when(loadConflictPort.findById(conflictId)).thenReturn(Optional.of(conflict));

        Employee origEmp = new Employee(new EmployeeId(conflictedEmpId), new UserId(1000L), 1L, "NV010", "Nguyễn Văn A", "Dev", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);

        when(loadEmployeePort.findById(new EmployeeId(conflictedEmpId))).thenReturn(Optional.of(origEmp));

        ConfirmReplacementProposalCommand command = new ConfirmReplacementProposalCommand(
                conflictId, conflictedEmpId, 50L, 3, "Notes"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.confirmReplacementProposal(command));
        assertTrue(ex.getMessage().contains("không được trùng"));
    }

    @Test
    void testConfirmProposal_UnapprovedOrInsufficientSkill_ThrowsIllegalArgumentException() {
        Long rmUserId = 100L;
        Long conflictId = 7L;
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

        // Low proficiency level (2 < 4)
        EmployeeSkill lowSkill = new EmployeeSkill(
                2L, replacementEmpId, skillId, 2, new BigDecimal("1.0"),
                SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeIdAndSkillId(replacementEmpId, skillId)).thenReturn(Optional.of(lowSkill));

        ConfirmReplacementProposalCommand command = new ConfirmReplacementProposalCommand(
                conflictId, replacementEmpId, skillId, 4, "Notes"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.confirmReplacementProposal(command));
        assertTrue(ex.getMessage().contains("thấp hơn mức yêu cầu"));
    }

    @Test
    void testFilterCandidates_OnlySuggestsCandidatesMeetingExcessHoursRequirement() {
        // Validation: excessHours = 8h. Candidate A = 2h (filtered out), Candidate B = 8h (suggested), Candidate C = 12h (suggested)
        Long rmUserId = 100L;
        Long conflictId = 10L;
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

        Skill skill = new Skill(skillId, "SKILL-JAVA", "Java Backend", "Backend", "Descr", LocalDateTime.now());
        when(loadSkillPort.findById(new SkillId(skillId))).thenReturn(Optional.of(skill));

        EmployeeSkill origSkill = new EmployeeSkill(
                1L, conflictedEmpId, skillId, 3, new BigDecimal("3.5"),
                SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeId(conflictedEmpId)).thenReturn(List.of(origSkill));

        // Candidates skills
        EmployeeSkill candASkill = new EmployeeSkill(2L, 20L, skillId, 3, new BigDecimal("3.0"), SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now());
        EmployeeSkill candBSkill = new EmployeeSkill(3L, 30L, skillId, 3, new BigDecimal("3.0"), SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now());
        EmployeeSkill candCSkill = new EmployeeSkill(4L, 40L, skillId, 3, new BigDecimal("3.0"), SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now());

        when(employeeSkillRepository.findApprovedBySkillAndMinLevel(eq(skillId), eq(3)))
                .thenReturn(List.of(candASkill, candBSkill, candCSkill));

        Employee candA = new Employee(new EmployeeId(20L), new UserId(2000L), 1L, "NV020", "Anh A (2h free)", "Developer", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee candB = new Employee(new EmployeeId(30L), new UserId(3000L), 1L, "NV030", "Anh B (8h free)", "Developer", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);
        Employee candC = new Employee(new EmployeeId(40L), new UserId(4000L), 1L, "NV040", "Anh C (12h free)", "Developer", LocalDate.now(), null, false, 40, EmployeeStatus.ACTIVE);

        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(candA, candB, candC));
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyMap());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyList());

        // Allocation: A has 38h allocated -> 2h free (< 8h required)
        // B has 32h allocated -> 8h free (== 8h required)
        // C has 28h allocated -> 12h free (> 8h required)
        com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation allocA = new com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation(
                1L, 20L, 100L, new com.hrm.employeemanagement.domain.availability.YearWeek(2026, 38), new BigDecimal("38.0"), 0L
        );
        com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation allocB = new com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation(
                2L, 30L, 100L, new com.hrm.employeemanagement.domain.availability.YearWeek(2026, 38), new BigDecimal("32.0"), 0L
        );
        com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation allocC = new com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation(
                3L, 40L, 100L, new com.hrm.employeemanagement.domain.availability.YearWeek(2026, 38), new BigDecimal("28.0"), 0L
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(allocA, allocB, allocC));

        ReplacementSuggestionResult result = service.getReplacementSuggestions(conflictId, skillId, 3);

        assertNotNull(result);
        assertTrue(result.hasAvailableReplacements());
        // Only B (8h) and C (12h) meet excessHours (8h) requirement
        assertEquals(2, result.candidates().size());
        assertEquals("Anh C (12h free)", result.candidates().get(0).fullName());
        assertEquals("Anh B (8h free)", result.candidates().get(1).fullName());
    }

    @Test
    void testConfirmProposal_InsufficientFreeHours_ThrowsIllegalArgumentException() {
        Long rmUserId = 100L;
        Long conflictId = 11L;
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

        EmployeeSkill candSkill = new EmployeeSkill(
                2L, replacementEmpId, skillId, 4, new BigDecimal("5.0"),
                SkillStatus.APPROVED, rmUserId, LocalDateTime.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(employeeSkillRepository.findByEmployeeIdAndSkillId(replacementEmpId, skillId)).thenReturn(Optional.of(candSkill));

        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyMap());
        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(Collections.emptyList());

        // Allocated 38h -> free hours = 2h < 8h excessHours required!
        com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation alloc = new com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation(
                1L, replacementEmpId, 100L, new com.hrm.employeemanagement.domain.availability.YearWeek(2026, 38), new BigDecimal("38.0"), 0L
        );
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of(alloc));

        ConfirmReplacementProposalCommand command = new ConfirmReplacementProposalCommand(
                conflictId, replacementEmpId, skillId, 4, "Notes"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.confirmReplacementProposal(command));
        assertTrue(ex.getMessage().contains("không đủ giờ rảnh"));
    }
}

