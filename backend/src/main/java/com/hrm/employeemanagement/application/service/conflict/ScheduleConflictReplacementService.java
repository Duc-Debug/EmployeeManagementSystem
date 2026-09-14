package com.hrm.employeemanagement.application.service.conflict;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.conflict.ConfirmReplacementProposalCommand;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementCandidateResult;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementProposalResult;
import com.hrm.employeemanagement.application.dto.conflict.ReplacementSuggestionResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.ConfirmReplacementProposalUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetReplacementSuggestionsUseCase;
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
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictReplacement;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ScheduleConflictReplacementService implements GetReplacementSuggestionsUseCase, ConfirmReplacementProposalUseCase {

    private final AuthorizationService authorizationService;
    private final LoadScheduleConflictPort loadConflictPort;
    private final SaveScheduleConflictPort saveConflictPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadSkillPort loadSkillPort;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final SaveScheduleConflictReplacementPort replacementPort;
    private final SaveAuditLogInNewTransactionPort auditLogPort;
    private final SimulatedNotificationPort notificationPort;

    public ScheduleConflictReplacementService(
            AuthorizationService authorizationService,
            LoadScheduleConflictPort loadConflictPort,
            SaveScheduleConflictPort saveConflictPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadSkillPort loadSkillPort,
            EmployeeSkillRepository employeeSkillRepository,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            SaveScheduleConflictReplacementPort replacementPort,
            SaveAuditLogInNewTransactionPort auditLogPort,
            SimulatedNotificationPort notificationPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.loadConflictPort = Objects.requireNonNull(loadConflictPort, "loadConflictPort must not be null");
        this.saveConflictPort = Objects.requireNonNull(saveConflictPort, "saveConflictPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "loadOrgUnitPort must not be null");
        this.loadSkillPort = Objects.requireNonNull(loadSkillPort, "loadSkillPort must not be null");
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "employeeSkillRepository must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "loadWeeklyAvailabilityPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "loadAllocationPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "loadApprovedLeavesPort must not be null");
        this.replacementPort = Objects.requireNonNull(replacementPort, "replacementPort must not be null");
        this.auditLogPort = Objects.requireNonNull(auditLogPort, "auditLogPort must not be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort must not be null");
    }

    @Override
    public ReplacementSuggestionResult getReplacementSuggestions(Long conflictId, Long targetSkillId, Integer minProficiencyLevel) {
        Long currentUserId;
        try {
            currentUserId = authorizationService.requireAny(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST);
        } catch (PermissionDeniedException ex) {
            auditLogPort.save(AuditLog.create(
                    null,
                    "ACCESS_DENIED",
                    "REPLACEMENT_SUGGESTION",
                    conflictId
            ));
            throw ex;
        }

        ScheduleConflict conflict = loadConflictPort.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo xung đột lịch với ID: " + conflictId));

        Employee conflictedEmployee = loadEmployeePort.findById(new EmployeeId(conflict.getEmployeeId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân sự bị xung đột với ID: " + conflict.getEmployeeId()));

        String deptName = "Chưa phân bổ phòng";
        if (conflictedEmployee.getOrgUnitId() != null) {
            Optional<OrgUnit> orgOpt = loadOrgUnitPort.findById(new com.hrm.employeemanagement.domain.orgunit.OrgUnitId(conflictedEmployee.getOrgUnitId()));
            if (orgOpt.isPresent()) {
                deptName = orgOpt.get().getUnitName();
            }
        }

        // Tìm các kỹ năng của nhân sự bị xung đột nếu không được truyền trực tiếp
        List<EmployeeSkill> conflictedEmpSkills = employeeSkillRepository.findByEmployeeId(conflictedEmployee.getIdValue());
        Long selectedSkillId = targetSkillId;
        int requiredLevel = minProficiencyLevel != null ? minProficiencyLevel : 1;

        if (selectedSkillId == null) {
            if (!conflictedEmpSkills.isEmpty()) {
                // Ưu tiên chọn kỹ năng có mức thành thạo cao nhất
                EmployeeSkill bestSkill = conflictedEmpSkills.stream()
                        .max(Comparator.comparingInt(EmployeeSkill::getProficiencyLevelValue))
                        .orElse(conflictedEmpSkills.get(0));
                selectedSkillId = bestSkill.getSkillId();
                if (minProficiencyLevel == null) {
                    requiredLevel = bestSkill.getProficiencyLevelValue();
                }
            } else {
                // FALLBACK: Nếu nhân sự chưa khai báo kỹ năng riêng, tự động lấy kỹ năng đã được duyệt phổ biến nhất trong hệ thống
                List<EmployeeSkill> allApproved = employeeSkillRepository.findByStatus(SkillStatus.APPROVED);
                if (!allApproved.isEmpty()) {
                    selectedSkillId = allApproved.stream()
                            .collect(Collectors.groupingBy(EmployeeSkill::getSkillId, Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(allApproved.get(0).getSkillId());
                }
            }
        }

        String skillName = "Chưa xác định kỹ năng";
        if (selectedSkillId != null) {
            Optional<Skill> skillOpt = loadSkillPort.findById(new com.hrm.employeemanagement.domain.skill.SkillId(selectedSkillId));
            if (skillOpt.isPresent()) {
                skillName = skillOpt.get().getName();
            }
        }

        // Nếu không có kỹ năng nào để tìm kiếm
        if (selectedSkillId == null) {
            return new ReplacementSuggestionResult(
                    conflict.getId(),
                    conflictedEmployee.getIdValue(),
                    conflictedEmployee.getEmployeeCode(),
                    conflictedEmployee.getFullName(),
                    deptName,
                    conflict.getYearNumber(),
                    conflict.getWeekNumber(),
                    "Tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber(),
                    null,
                    "Chưa khai báo kỹ năng",
                    1,
                    conflict.getExcessHours(),
                    Collections.emptyList(),
                    false,
                    "Nhân sự bị xung đột chưa khai báo kỹ năng. Gợi ý: Yêu cầu khai báo kỹ năng hoặc dời lịch phân bổ."
            );
        }

        // 1. Tìm các nhân sự khác có cùng kỹ năng với mức thành thạo tương đương (>= requiredLevel)
        List<EmployeeSkill> matchingEmpSkills = employeeSkillRepository
                .findApprovedBySkillAndMinLevel(selectedSkillId, requiredLevel);

        // Lọc bỏ chính nhân sự bị xung đột
        List<Long> candidateEmpIds = matchingEmpSkills.stream()
                .map(EmployeeSkill::getEmployeeId)
                .filter(id -> !id.equals(conflictedEmployee.getIdValue()))
                .distinct()
                .toList();

        if (candidateEmpIds.isEmpty()) {
            // [NCL-07-CN-002-TC-02] Không ai cùng kỹ năng rảnh -> Báo không tìm được & gợi ý dời lịch
            return new ReplacementSuggestionResult(
                    conflict.getId(),
                    conflictedEmployee.getIdValue(),
                    conflictedEmployee.getEmployeeCode(),
                    conflictedEmployee.getFullName(),
                    deptName,
                    conflict.getYearNumber(),
                    conflict.getWeekNumber(),
                    "Tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber(),
                    selectedSkillId,
                    skillName,
                    requiredLevel,
                    conflict.getExcessHours(),
                    Collections.emptyList(),
                    false,
                    "Không tìm thấy người thay thế phù hợp có cùng kỹ năng còn đủ giờ rảnh trong tuần này. Gợi ý: Dời lịch phân bổ công việc sang tuần khác hoặc điều chỉnh thời gian dự án."
            );
        }

        // 2. Tính số giờ rảnh còn lại trong tuần xảy ra xung đột của từng ứng viên
        YearWeek targetWeek = new YearWeek(conflict.getYearNumber(), conflict.getWeekNumber());

        List<Employee> candidateEmployees = loadEmployeePort.findAllByIdIn(
                candidateEmpIds.stream().map(EmployeeId::new).toList()
        );

        Map<Long, Employee> candidateEmpMap = candidateEmployees.stream()
                .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

        // Tải phòng ban
        List<Long> orgUnitIds = candidateEmployees.stream()
                .map(Employee::getOrgUnitId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> orgUnitMap = orgUnitIds.isEmpty() ? Collections.emptyMap() :
                loadOrgUnitPort.findAllByIdIn(orgUnitIds).stream()
                        .collect(Collectors.toMap(u -> u.getId().getValue(), OrgUnit::getUnitName, (u1, u2) -> u1));

        // Tải availability & allocations & approved leaves
        Map<Long, Map<YearWeek, BigDecimal>> approvedLeavesMap = loadApprovedLeavesPort
                .loadApprovedLeaveHoursForEmployeesAndWeeks(candidateEmpIds, List.of(targetWeek));

        List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort
                .loadAvailabilityForEmployeesAndWeeks(candidateEmpIds, List.of(targetWeek));

        Map<Long, WeeklyAvailability> availMap = availabilities.stream()
                .collect(Collectors.toMap(WeeklyAvailability::getEmployeeId, a -> a, (a1, a2) -> a1));

        List<WeeklyProjectAllocation> allocations = loadAllocationPort
                .loadAllocationsForEmployeesAndWeeks(candidateEmpIds, List.of(targetWeek));

        Map<Long, BigDecimal> totalAllocatedMap = allocations.stream()
                .collect(Collectors.groupingBy(
                        WeeklyProjectAllocation::getEmployeeId,
                        Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
                ));

        Map<Long, EmployeeSkill> empSkillMap = matchingEmpSkills.stream()
                .collect(Collectors.toMap(EmployeeSkill::getEmployeeId, s -> s, (s1, s2) -> s1));

        List<ReplacementCandidateResult> candidates = new ArrayList<>();

        for (Long empId : candidateEmpIds) {
            Employee emp = candidateEmpMap.get(empId);
            if (emp == null || emp.getStatus() == null || !"ACTIVE".equalsIgnoreCase(emp.getStatus().name())) {
                continue;
            }

            int stdHours = emp.getStandardHoursPerWeek() != null ? emp.getStandardHoursPerWeek() : 40;
            BigDecimal leaveHours = approvedLeavesMap.getOrDefault(empId, Collections.emptyMap()).getOrDefault(targetWeek, BigDecimal.ZERO);
            WeeklyAvailability avail = availMap.get(empId);
            BigDecimal netAvail = avail != null ? avail.getNetAvailableHours() : BigDecimal.valueOf(stdHours).subtract(leaveHours).max(BigDecimal.ZERO);

            BigDecimal allocated = totalAllocatedMap.getOrDefault(empId, BigDecimal.ZERO);
            BigDecimal freeHours = netAvail.subtract(allocated);

            if (freeHours.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Bỏ qua nếu không còn giờ rảnh
            }

            EmployeeSkill es = empSkillMap.get(empId);
            int profLevel = es != null ? es.getProficiencyLevelValue() : requiredLevel;

            String candidateDept = emp.getOrgUnitId() != null
                    ? orgUnitMap.getOrDefault(emp.getOrgUnitId(), "Chưa phân bổ phòng")
                    : "Chưa phân bổ phòng";

            candidates.add(new ReplacementCandidateResult(
                    emp.getIdValue(),
                    emp.getEmployeeCode(),
                    emp.getFullName(),
                    emp.getOrgUnitId(),
                    candidateDept,
                    selectedSkillId,
                    skillName,
                    profLevel,
                    formatProficiencyLevel(profLevel),
                    freeHours,
                    stdHours,
                    allocated,
                    emp.getContractEndDate() != null ? emp.getContractEndDate().toString() : null
            ));
        }

        // [NCL-07-CN-002-TC-01] Sắp xếp danh sách người thay thế theo giờ còn rảnh giảm dần, sau đó đến mức thành thạo
        candidates.sort(Comparator.comparing(ReplacementCandidateResult::freeHours)
                .thenComparing(ReplacementCandidateResult::proficiencyLevel)
                .reversed());

        if (candidates.isEmpty()) {
            // [NCL-07-CN-002-TC-02] Không ai cùng kỹ năng rảnh trong tuần -> Gợi ý dời lịch
            return new ReplacementSuggestionResult(
                    conflict.getId(),
                    conflictedEmployee.getIdValue(),
                    conflictedEmployee.getEmployeeCode(),
                    conflictedEmployee.getFullName(),
                    deptName,
                    conflict.getYearNumber(),
                    conflict.getWeekNumber(),
                    "Tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber(),
                    selectedSkillId,
                    skillName,
                    requiredLevel,
                    conflict.getExcessHours(),
                    Collections.emptyList(),
                    false,
                    "Không có nhân sự cùng kỹ năng còn giờ rảnh trong tuần này. Gợi ý: Dời lịch phân bổ công việc sang tuần khác hoặc điều chỉnh tiến độ."
            );
        }

        return new ReplacementSuggestionResult(
                conflict.getId(),
                conflictedEmployee.getIdValue(),
                conflictedEmployee.getEmployeeCode(),
                conflictedEmployee.getFullName(),
                deptName,
                conflict.getYearNumber(),
                conflict.getWeekNumber(),
                "Tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber(),
                selectedSkillId,
                skillName,
                requiredLevel,
                conflict.getExcessHours(),
                candidates,
                true,
                "Tìm thấy " + candidates.size() + " nhân sự thay thế phù hợp có cùng kỹ năng và còn đủ giờ rảnh trong tuần."
        );
    }

    @Override
    public ReplacementProposalResult confirmReplacementProposal(ConfirmReplacementProposalCommand command) {
        Long currentUserId;
        try {
            currentUserId = authorizationService.requireAny(PermissionCode.RESOURCE_REPLACEMENT_SUGGEST);
        } catch (PermissionDeniedException ex) {
            auditLogPort.save(AuditLog.create(
                    null,
                    "ACCESS_DENIED",
                    "REPLACEMENT_CONFIRMATION",
                    command.conflictId()
            ));
            throw ex;
        }

        ScheduleConflict conflict = loadConflictPort.findById(command.conflictId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo xung đột lịch với ID: " + command.conflictId()));

        Employee originalEmp = loadEmployeePort.findById(new EmployeeId(conflict.getEmployeeId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân sự gốc: " + conflict.getEmployeeId()));

        Employee replacementEmp = loadEmployeePort.findById(new EmployeeId(command.replacementEmployeeId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân sự thay thế: " + command.replacementEmployeeId()));

        // 1. Re-validate: Trạng thái nhân sự thay thế phải là ACTIVE
        if (replacementEmp.getStatus() == null || !"ACTIVE".equalsIgnoreCase(replacementEmp.getStatus().name())) {
            throw new IllegalArgumentException("Nhân sự thay thế không ở trạng thái đang hoạt động (ACTIVE)");
        }

        // 2. Re-validate: Không được tự thay thế chính nhân sự đang bị xung đột
        if (replacementEmp.getIdValue().equals(originalEmp.getIdValue())) {
            throw new IllegalArgumentException("Nhân sự thay thế không được trùng với nhân sự đang bị xung đột lịch");
        }

        // 3. Re-validate: Kỹ năng và mức thành thạo của candidate tại thời điểm confirm
        int reqLevel = command.proficiencyLevel() != null ? command.proficiencyLevel() : 1;
        if (command.skillId() != null) {
            Optional<EmployeeSkill> empSkillOpt = employeeSkillRepository
                    .findByEmployeeIdAndSkillId(replacementEmp.getIdValue(), command.skillId());
            if (empSkillOpt.isEmpty() || empSkillOpt.get().getStatus() != SkillStatus.APPROVED) {
                throw new IllegalArgumentException("Nhân sự thay thế chưa có kỹ năng được phê duyệt cho kỹ năng yêu cầu (ID: " + command.skillId() + ")");
            }
            if (empSkillOpt.get().getProficiencyLevelValue() < reqLevel) {
                throw new IllegalArgumentException(String.format(
                        "Mức thành thạo kỹ năng của nhân sự thay thế (%d) thấp hơn mức yêu cầu (%d)",
                        empSkillOpt.get().getProficiencyLevelValue(), reqLevel
                ));
            }
        }

        // 4. Re-validate: Số giờ rảnh của candidate trong đúng tuần xảy ra xung đột
        YearWeek targetWeek = new YearWeek(conflict.getYearNumber(), conflict.getWeekNumber());

        Map<Long, Map<YearWeek, BigDecimal>> approvedLeavesMap = loadApprovedLeavesPort
                .loadApprovedLeaveHoursForEmployeesAndWeeks(List.of(replacementEmp.getIdValue()), List.of(targetWeek));

        List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort
                .loadAvailabilityForEmployeesAndWeeks(List.of(replacementEmp.getIdValue()), List.of(targetWeek));
        WeeklyAvailability avail = availabilities.stream().findFirst().orElse(null);

        List<WeeklyProjectAllocation> allocations = loadAllocationPort
                .loadAllocationsForEmployeesAndWeeks(List.of(replacementEmp.getIdValue()), List.of(targetWeek));

        int stdHours = replacementEmp.getStandardHoursPerWeek() != null ? replacementEmp.getStandardHoursPerWeek() : 40;
        BigDecimal leaveHours = approvedLeavesMap.getOrDefault(replacementEmp.getIdValue(), Collections.emptyMap())
                .getOrDefault(targetWeek, BigDecimal.ZERO);
        BigDecimal netAvail = avail != null ? avail.getNetAvailableHours() : BigDecimal.valueOf(stdHours).subtract(leaveHours).max(BigDecimal.ZERO);

        BigDecimal allocated = allocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal freeHours = netAvail.subtract(allocated);

        if (freeHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nhân sự thay thế không còn giờ rảnh trong tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber());
        }

        // Tải skill name
        String skillName = "Chuyên môn";
        if (command.skillId() != null) {
            Optional<Skill> sOpt = loadSkillPort.findById(new com.hrm.employeemanagement.domain.skill.SkillId(command.skillId()));
            if (sOpt.isPresent()) skillName = sOpt.get().getName();
        }

        // Lưu bản ghi đề xuất thay thế vào CSDL
        ScheduleConflictReplacement domainToSave = new ScheduleConflictReplacement(
                null,
                command.conflictId(),
                originalEmp.getIdValue(),
                replacementEmp.getIdValue(),
                command.skillId(),
                command.proficiencyLevel() != null ? command.proficiencyLevel() : 3,
                freeHours,
                "PROPOSED",
                command.notes(),
                currentUserId,
                null
        );

        ScheduleConflictReplacement saved = replacementPort.save(domainToSave);

        // Cập nhật trạng thái của xung đột thành RESOLVED hoặc NOTIFIED
        conflict.markAsNotified(currentUserId);
        saveConflictPort.save(conflict);

        // Tải thông tin người thực hiện thao tác
        String actorName = "Quản lý nguồn lực (ID: " + currentUserId + ")";
        Optional<User> userOpt = loadUserPort.findById(new UserId(currentUserId));
        if (userOpt.isPresent()) {
            actorName = userOpt.get().getUsername();
        }

        String proposalDetails = String.format(
                "Đề xuất thay thế nhân sự [%s - %s] bằng [%s - %s] cho xung đột tuần %d/%d (Kỹ năng: %s, Mức thành thạo: %s). Ghi chú: %s",
                originalEmp.getEmployeeCode(), originalEmp.getFullName(),
                replacementEmp.getEmployeeCode(), replacementEmp.getFullName(),
                conflict.getWeekNumber(), conflict.getYearNumber(),
                skillName, formatProficiencyLevel(command.proficiencyLevel()),
                command.notes() != null ? command.notes() : "Không có"
        );

        // [NCL-07-CN-002-CV-02] Phát thông báo mô phỏng
        notificationPort.sendReplacementSuggestionNotification(
                "pm.resource@company.com",
                "Quản lý dự án & Nguồn lực",
                originalEmp.getFullName() + " (" + originalEmp.getEmployeeCode() + ")",
                replacementEmp.getFullName() + " (" + replacementEmp.getEmployeeCode() + ")",
                proposalDetails
        );

        // [NCL-07-CN-002-TC-04] Ghi lại lịch sử thực hiện vào Audit Log
        auditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PROPOSE_REPLACEMENT_STAFF",
                "schedule_conflict_replacements",
                saved.getId(),
                null,
                proposalDetails
        ));

        return new ReplacementProposalResult(
                saved.getId(),
                conflict.getId(),
                originalEmp.getIdValue(),
                originalEmp.getFullName(),
                replacementEmp.getIdValue(),
                replacementEmp.getFullName(),
                command.skillId(),
                skillName,
                command.proficiencyLevel(),
                conflict.getExcessHours(),
                "PROPOSED",
                command.notes(),
                currentUserId,
                actorName,
                saved.getCreatedAt() != null ? saved.getCreatedAt() : java.time.LocalDateTime.now()
        );
    }

    private String formatProficiencyLevel(Integer level) {
        if (level == null) return "Thành thạo";
        return switch (level) {
            case 1 -> "Level 1 - Cơ bản";
            case 2 -> "Level 2 - Trung cấp";
            case 3 -> "Level 3 - Thành thạo";
            case 4 -> "Level 4 - Cao cấp";
            case 5 -> "Level 5 - Chuyên gia";
            default -> "Level " + level + " - Thành thạo";
        };
    }
}
