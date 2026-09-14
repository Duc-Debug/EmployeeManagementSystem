package com.hrm.employeemanagement.application.service.conflict;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictQuery;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetScheduleConflictsUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.NotifyScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ResolveScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ScanScheduleConflictsUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;

public class ScheduleConflictService implements
        GetScheduleConflictsUseCase,
        ScanScheduleConflictsUseCase,
        NotifyScheduleConflictUseCase,
        ResolveScheduleConflictUseCase {

    private final LoadScheduleConflictPort loadConflictPort;
    private final SaveScheduleConflictPort saveConflictPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogInNewTransactionPort auditLogPort;
    private final SimulatedNotificationPort notificationPort;

    public ScheduleConflictService(
            LoadScheduleConflictPort loadConflictPort,
            SaveScheduleConflictPort saveConflictPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService,
            SaveAuditLogInNewTransactionPort auditLogPort,
            SimulatedNotificationPort notificationPort
    ) {
        this.loadConflictPort = Objects.requireNonNull(loadConflictPort, "loadConflictPort must not be null");
        this.saveConflictPort = Objects.requireNonNull(saveConflictPort, "saveConflictPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "loadAllocationPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "loadApprovedLeavesPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "loadProjectPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "loadOrgUnitPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.auditLogPort = Objects.requireNonNull(auditLogPort, "auditLogPort must not be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort must not be null");
    }

    @Override
    public List<ScheduleConflictResult> getScheduleConflicts(ScheduleConflictQuery query) {
        // TC-04: Enforce permission RESOURCE_SCHEDULE_CONFLICT_READ (VT-02 PM, VT-03 RM, VT-06 Admin)
        authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ,
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY
        );

        // GET endpoint is strictly read-only (HIGH 1: zero DB side-effects)
        List<ScheduleConflict> conflicts = loadConflictPort.findConflicts(
                query.yearNumber(),
                query.startWeek(),
                query.endWeek(),
                query.employeeId(),
                query.conflictType(),
                query.status()
        );

        if (query.projectId() != null) {
            String pIdStr = String.valueOf(query.projectId());
            conflicts = conflicts.stream()
                    .filter(c -> c.getProjectIds() != null && (c.getProjectIds().equals(pIdStr) || c.getProjectIds().contains("," + pIdStr) || c.getProjectIds().contains(pIdStr + ",")))
                    .collect(Collectors.toList());
        }

        return mapToResults(conflicts);
    }

    @Override
    public List<ScheduleConflictResult> scanScheduleConflicts(Integer yearNumber, Integer startWeek, Integer endWeek) {
        authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ,
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY
        );

        Integer year = yearNumber != null ? yearNumber : LocalDate.now().getYear();
        Integer startW = startWeek != null ? startWeek : LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        Integer endW = endWeek != null ? endWeek : Math.min(startW + 4, 52);

        List<ScheduleConflict> scanned = scanInternal(year, startW, endW);
        return mapToResults(scanned);
    }

    @Override
    public ScheduleConflictResult notifyScheduleConflict(Long conflictId) {
        // BLOCKER 1: Require strictly RESOURCE_SCHEDULE_CONFLICT_NOTIFY (READ permission is not allowed to mutate data)
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY
        );

        ScheduleConflict conflict = loadConflictPort.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo xung đột lịch với ID: " + conflictId));

        conflict.markAsNotified(currentUserId);
        ScheduleConflict saved = saveConflictPort.save(conflict);

        // Fetch employee info for simulated notification
        Optional<Employee> empOpt = loadEmployeePort.findById(new EmployeeId(conflict.getEmployeeId()));
        String empName = empOpt.map(Employee::getFullName).orElse("Nhân viên #" + conflict.getEmployeeId());
        String empCode = empOpt.map(Employee::getEmployeeCode).orElse("NV" + conflict.getEmployeeId());

        String conflictSummary = conflict.getConflictType() == ConflictType.MULTI_PROJECT_ALLOCATION
                ? "Trùng phân bổ nhiều dự án (" + conflict.getProjectNames() + ") - Giờ vượt: " + conflict.getExcessHours() + "h"
                : "Xung đột phân bổ (" + conflict.getProjectNames() + ") và đơn nghỉ phép đã duyệt (" + conflict.getLeaveInfo() + ")";

        // Trigger simulated notification email/message
        notificationPort.sendScheduleConflictWarningNotification(
                "pm.management@company.com",
                "Quản lý dự án / Resource Manager",
                empName + " (" + empCode + ")",
                conflictSummary,
                conflict.getDetails() != null ? conflict.getDetails() : "Xung đột phát hiện tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber()
        );

        // TC-05: Record audit log entry in audit_logs table
        auditLogPort.save(AuditLog.createChange(
                currentUserId,
                "NOTIFY_SCHEDULE_CONFLICT",
                "schedule_conflict_warnings",
                conflictId,
                null,
                "employee_id=" + conflict.getEmployeeId() + ";conflict_type=" + conflict.getConflictType() + ";excess_hours=" + conflict.getExcessHours()
        ));

        return mapToResult(saved);
    }

    @Override
    public ScheduleConflictResult resolveScheduleConflict(Long conflictId) {
        // BLOCKER 1: Require strictly RESOURCE_SCHEDULE_CONFLICT_NOTIFY (READ permission is not allowed to mutate data)
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY
        );

        ScheduleConflict conflict = loadConflictPort.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo xung đột lịch với ID: " + conflictId));

        conflict.markAsResolved();
        ScheduleConflict saved = saveConflictPort.save(conflict);

        // TC-05: Record audit log entry in audit_logs table
        auditLogPort.save(AuditLog.createChange(
                currentUserId,
                "RESOLVE_SCHEDULE_CONFLICT",
                "schedule_conflict_warnings",
                conflictId,
                null,
                "employee_id=" + conflict.getEmployeeId() + ";status=RESOLVED"
        ));

        return mapToResult(saved);
    }

    private List<ScheduleConflict> scanInternal(Integer year, Integer startWeek, Integer endWeek) {
        List<Employee> activeEmployees = loadEmployeePort.findAllActive();
        Map<Long, Employee> activeEmpMap = activeEmployees.stream()
                .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

        List<ScheduleConflict> resultConflicts = new ArrayList<>();

        for (int week = startWeek; week <= endWeek; week++) {
            final int currentWeekNum = week;

            // Load ALL allocations for target week (pass null employeeIds to search all allocations)
            List<WeeklyProjectAllocation> allocations = loadAllocationPort
                    .loadAllocationsForEmployeesInWeekRange(null, year, currentWeekNum, currentWeekNum);

            Map<Long, List<WeeklyProjectAllocation>> allocationsByEmp = allocations.stream()
                    .collect(Collectors.groupingBy(WeeklyProjectAllocation::getEmployeeId));

            Set<Long> candidateEmpIds = new HashSet<>(activeEmpMap.keySet());
            candidateEmpIds.addAll(allocationsByEmp.keySet());

            if (candidateEmpIds.isEmpty()) {
                continue;
            }

            // Batch load project names for all projects in allocations
            Set<ProjectId> allProjectIds = allocations.stream()
                    .map(WeeklyProjectAllocation::getProjectId)
                    .filter(Objects::nonNull)
                    .map(ProjectId::new)
                    .collect(Collectors.toSet());

            Map<Long, String> projectNameMap = allProjectIds.isEmpty() ? Collections.emptyMap() :
                    loadProjectPort.findAllById(new ArrayList<>(allProjectIds)).stream()
                            .collect(Collectors.toMap(p -> p.getId().value(), Project::getProjectName, (p1, p2) -> p1));

            List<Long> candidateList = new ArrayList<>(candidateEmpIds);

            // Load approved leaves for candidate employee IDs in target week
            YearWeek yw = new YearWeek(year, currentWeekNum);
            Map<Long, Map<YearWeek, BigDecimal>> approvedLeavesMap = loadApprovedLeavesPort
                    .loadApprovedLeaveHoursForEmployeesAndWeeks(candidateList, List.of(yw));

            for (Long empId : candidateEmpIds) {
                Employee emp = activeEmpMap.get(empId);
                List<WeeklyProjectAllocation> empAllocations = allocationsByEmp.getOrDefault(empId, Collections.emptyList());

                BigDecimal totalAllocatedHours = empAllocations.stream()
                        .map(WeeklyProjectAllocation::getAllocatedHours)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Set<Long> projectIdsSet = empAllocations.stream()
                        .map(WeeklyProjectAllocation::getProjectId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                BigDecimal approvedLeaveHours = approvedLeavesMap.getOrDefault(empId, Collections.emptyMap())
                        .getOrDefault(yw, BigDecimal.ZERO);

                BigDecimal standardCapacity = (emp != null && emp.getStandardHoursPerWeek() != null)
                        ? BigDecimal.valueOf(emp.getStandardHoursPerWeek())
                        : BigDecimal.valueOf(40.0);

                BigDecimal netAvailableHours = standardCapacity.subtract(approvedLeaveHours).max(BigDecimal.ZERO);

                // Scenario 1: Multi-project / Overload allocation conflict
                // Must require projectIdsSet.size() >= 2 for MULTI_PROJECT_ALLOCATION conflict type
                boolean isMultiProjectConflict = projectIdsSet.size() >= 2 &&
                        (totalAllocatedHours.compareTo(standardCapacity) > 0 || totalAllocatedHours.compareTo(netAvailableHours) > 0);

                if (isMultiProjectConflict) {
                    BigDecimal excessHours = totalAllocatedHours.subtract(netAvailableHours).max(BigDecimal.ZERO);
                    if (excessHours.compareTo(BigDecimal.ZERO) == 0 && totalAllocatedHours.compareTo(standardCapacity) > 0) {
                        excessHours = totalAllocatedHours.subtract(standardCapacity).max(BigDecimal.ZERO);
                    }

                    String projectIdsStr = projectIdsSet.stream().map(String::valueOf).collect(Collectors.joining(","));
                    List<String> pNames = projectIdsSet.stream()
                            .map(pId -> projectNameMap.getOrDefault(pId, "Dự án #" + pId))
                            .collect(Collectors.toList());
                    String projectNamesStr = String.join(", ", pNames);

                    ScheduleConflict existing = loadConflictPort
                            .findExistingConflict(empId, year, currentWeekNum, ConflictType.MULTI_PROJECT_ALLOCATION)
                            .orElse(null);

                    if (existing != null) {
                        existing.setProjectIds(projectIdsStr);
                        existing.setProjectNames(projectNamesStr);
                        existing.setTotalAllocatedHours(totalAllocatedHours);
                        existing.setNetAvailableHours(netAvailableHours);
                        existing.setExcessHours(excessHours);
                        existing.setDetails("Phân bổ trên " + projectIdsSet.size() + " dự án (" + projectNamesStr + ") với tổng " + totalAllocatedHours + "h/tuần");
                        saveConflictPort.save(existing);
                        resultConflicts.add(existing);
                    } else {
                        ScheduleConflict newConflict = ScheduleConflict.create(
                                empId,
                                year,
                                currentWeekNum,
                                ConflictType.MULTI_PROJECT_ALLOCATION,
                                projectIdsStr,
                                projectNamesStr,
                                null,
                                null,
                                totalAllocatedHours,
                                netAvailableHours,
                                excessHours,
                                "Phân bổ trên " + projectIdsSet.size() + " dự án (" + projectNamesStr + ") với tổng " + totalAllocatedHours + "h/tuần"
                        );
                        ScheduleConflict saved = saveConflictPort.save(newConflict);
                        resultConflicts.add(saved);
                    }
                }

                // Scenario 2: Leave & Allocation conflict (TC-02)
                if (approvedLeaveHours.compareTo(BigDecimal.ZERO) > 0 && totalAllocatedHours.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal excessHours = totalAllocatedHours.add(approvedLeaveHours).subtract(standardCapacity).max(BigDecimal.ZERO);
                    if (excessHours.compareTo(BigDecimal.ZERO) == 0) {
                        excessHours = totalAllocatedHours;
                    }

                    String projectIdsStr = projectIdsSet.stream().map(String::valueOf).collect(Collectors.joining(","));
                    List<String> pNames = projectIdsSet.stream()
                            .map(pId -> projectNameMap.getOrDefault(pId, "Dự án #" + pId))
                            .collect(Collectors.toList());
                    String projectNamesStr = String.join(", ", pNames);
                    String leaveInfoStr = "Đơn nghỉ phép đã duyệt (" + approvedLeaveHours + "h)";

                    ScheduleConflict existingLeaveConflict = loadConflictPort
                            .findExistingConflict(empId, year, currentWeekNum, ConflictType.LEAVE_ALLOCATION_CONFLICT)
                            .orElse(null);

                    if (existingLeaveConflict != null) {
                        existingLeaveConflict.setProjectIds(projectIdsStr);
                        existingLeaveConflict.setProjectNames(projectNamesStr);
                        existingLeaveConflict.setLeaveInfo(leaveInfoStr);
                        existingLeaveConflict.setTotalAllocatedHours(totalAllocatedHours);
                        existingLeaveConflict.setNetAvailableHours(netAvailableHours);
                        existingLeaveConflict.setExcessHours(excessHours);
                        existingLeaveConflict.setDetails("Có đơn nghỉ phép đã duyệt (" + approvedLeaveHours + "h) trùng tuần được phân bổ vào các dự án: " + projectNamesStr);
                        saveConflictPort.save(existingLeaveConflict);
                        resultConflicts.add(existingLeaveConflict);
                    } else {
                        ScheduleConflict newLeaveConflict = ScheduleConflict.create(
                                empId,
                                year,
                                currentWeekNum,
                                ConflictType.LEAVE_ALLOCATION_CONFLICT,
                                projectIdsStr,
                                projectNamesStr,
                                null,
                                leaveInfoStr,
                                totalAllocatedHours,
                                netAvailableHours,
                                excessHours,
                                "Có đơn nghỉ phép đã duyệt (" + approvedLeaveHours + "h) trùng tuần được phân bổ vào các dự án: " + projectNamesStr
                        );
                        ScheduleConflict saved = saveConflictPort.save(newLeaveConflict);
                        resultConflicts.add(saved);
                    }
                }
            }
        }

        return resultConflicts;
    }

    private List<ScheduleConflictResult> mapToResults(List<ScheduleConflict> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmployeeId> empIds = conflicts.stream()
                .map(c -> new EmployeeId(c.getEmployeeId()))
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Employee> empMap = loadEmployeePort.findAllByIdIn(empIds).stream()
                .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

        List<Long> orgUnitIds = empMap.values().stream()
                .map(Employee::getOrgUnitId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> orgUnitMap = orgUnitIds.isEmpty() ? Collections.emptyMap() :
                loadOrgUnitPort.findAllByIdIn(orgUnitIds).stream()
                        .collect(Collectors.toMap(u -> u.getId().getValue(), OrgUnit::getUnitName, (u1, u2) -> u1));

        return conflicts.stream()
                .map(c -> mapToResult(c, empMap.get(c.getEmployeeId()), orgUnitMap))
                .collect(Collectors.toList());
    }

    private ScheduleConflictResult mapToResult(ScheduleConflict conflict) {
        return mapToResults(List.of(conflict)).get(0);
    }

    private ScheduleConflictResult mapToResult(ScheduleConflict conflict, Employee emp, Map<Long, String> orgUnitMap) {
        String empCode = emp != null ? emp.getEmployeeCode() : "NV" + conflict.getEmployeeId();
        String empName = emp != null ? emp.getFullName() : "Nhân viên #" + conflict.getEmployeeId();

        String deptName = "Chưa phân bổ phòng";
        if (emp != null && emp.getOrgUnitId() != null) {
            deptName = orgUnitMap.getOrDefault(emp.getOrgUnitId(), "Chưa phân bổ phòng");
        }

        String conflictTypeLabel = conflict.getConflictType() == ConflictType.MULTI_PROJECT_ALLOCATION
                ? "Phân bổ nhiều dự án"
                : "Phân bổ trùng nghỉ phép";

        String statusLabel = switch (conflict.getStatus()) {
            case OPEN -> "MỚI PHÁT HIỆN";
            case NOTIFIED -> "ĐÃ THÔNG BÁO";
            case RESOLVED -> "ĐÃ XỬ LÝ";
        };

        String weekLabel = "Tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber();

        return new ScheduleConflictResult(
                conflict.getId(),
                conflict.getEmployeeId(),
                empCode,
                empName,
                deptName,
                conflict.getYearNumber(),
                conflict.getWeekNumber(),
                weekLabel,
                conflict.getConflictType(),
                conflictTypeLabel,
                conflict.getProjectIds(),
                conflict.getProjectNames(),
                conflict.getLeaveRequestId(),
                conflict.getLeaveInfo(),
                conflict.getTotalAllocatedHours(),
                conflict.getNetAvailableHours(),
                conflict.getExcessHours(),
                conflict.getStatus(),
                statusLabel,
                conflict.getDetails(),
                conflict.getNotifiedAt(),
                conflict.getNotifiedBy(),
                null,
                conflict.getCreatedAt(),
                conflict.getUpdatedAt()
        );
    }
}
