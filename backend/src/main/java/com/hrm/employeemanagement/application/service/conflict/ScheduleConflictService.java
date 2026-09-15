package com.hrm.employeemanagement.application.service.conflict;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.hrm.employeemanagement.application.dto.conflict.AssignScheduleConflictHandlerCommand;
import com.hrm.employeemanagement.application.dto.conflict.ResolveScheduleConflictWithNoteCommand;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictQuery;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.AssignScheduleConflictHandlerUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetScheduleConflictsUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.NotifyScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ResolveScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ResolveScheduleConflictWithNoteUseCase;
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
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;

public class ScheduleConflictService implements
        GetScheduleConflictsUseCase,
        ScanScheduleConflictsUseCase,
        NotifyScheduleConflictUseCase,
        ResolveScheduleConflictUseCase,
        ResolveScheduleConflictWithNoteUseCase,
        AssignScheduleConflictHandlerUseCase {

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
        // Enforce permission (VT-02 PM, VT-03 RM, VT-06 Admin)
        authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ,
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY,
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        );

        List<ScheduleConflict> conflicts = loadConflictPort.findConflicts(
                query.yearNumber(),
                query.startWeek(),
                query.endWeek(),
                query.employeeId(),
                query.conflictType(),
                query.status()
        );

        // The default view is a work queue. Resolved records remain accessible
        // through the explicit RESOLVED status filter, but do not belong here.
        if (query.status() == null) {
            conflicts = conflicts.stream()
                    .filter(conflict -> conflict.getStatus() != ScheduleConflictStatus.RESOLVED)
                    .collect(Collectors.toList());
        }

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
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY,
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        );

        LocalDate today = LocalDate.now();
        WeekFields iso = WeekFields.ISO;
        Integer year = yearNumber != null ? yearNumber : today.get(iso.weekBasedYear());
        Integer startW = startWeek != null ? startWeek : today.get(iso.weekOfWeekBasedYear());
        int maxIsoWeeks = YearWeek.maxWeeksInYear(year);
        Integer endW = endWeek != null ? endWeek : Math.min(startW + 4, maxIsoWeeks);

        List<ScheduleConflict> scanned = scanInternal(year, startW, endW);
        return mapToResults(scanned);
    }

    @Override
    public ScheduleConflictResult notifyScheduleConflict(Long conflictId) {
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY,
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        );

        ScheduleConflict conflict = loadConflictPort.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo xung đột lịch với ID: " + conflictId));

        conflict.markAsNotified(currentUserId);
        ScheduleConflict saved = saveConflictPort.save(conflict);

        Optional<Employee> empOpt = loadEmployeePort.findById(new EmployeeId(conflict.getEmployeeId()));
        String empName = empOpt.map(Employee::getFullName).orElse("Nhân viên #" + conflict.getEmployeeId());
        String empCode = empOpt.map(Employee::getEmployeeCode).orElse("NV" + conflict.getEmployeeId());

        String conflictSummary = conflict.getConflictType() == ConflictType.MULTI_PROJECT_ALLOCATION
                ? "Trùng phân bổ nhiều dự án (" + conflict.getProjectNames() + ") - Giờ vượt: " + conflict.getExcessHours() + "h"
                : "Xung đột phân bổ (" + conflict.getProjectNames() + ") và đơn nghỉ phép đã duyệt (" + conflict.getLeaveInfo() + ")";

        notificationPort.sendScheduleConflictWarningNotification(
                "pm.management@company.com",
                "Quản lý dự án / Resource Manager",
                empName + " (" + empCode + ")",
                conflictSummary,
                conflict.getDetails() != null ? conflict.getDetails() : "Xung đột phát hiện tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber()
        );

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
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        );

        ScheduleConflict conflict = loadConflictPort.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo xung đột lịch với ID: " + conflictId));

        conflict.markAsResolved();
        ScheduleConflict saved = saveConflictPort.save(conflict);

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

    @Override
    public ScheduleConflictResult resolveScheduleConflictWithNote(ResolveScheduleConflictWithNoteCommand command) {
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        );

        validateAssignedHandler(command.assignedHandlerId());

        ScheduleConflict conflict = loadConflictPort.findById(command.conflictId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo xung đột lịch với ID: " + command.conflictId()));

        conflict.resolveWithNote(currentUserId, command.assignedHandlerId(), command.resolutionNote());
        ScheduleConflict saved = saveConflictPort.save(conflict);

        auditLogPort.save(AuditLog.createChange(
                currentUserId,
                "RESOLVE_SCHEDULE_CONFLICT",
                "schedule_conflict_warnings",
                command.conflictId(),
                null,
                "employee_id=" + conflict.getEmployeeId() + ";status=RESOLVED;assigned_handler_id=" + conflict.getAssignedHandlerId() + ";resolution_note=" + (command.resolutionNote() != null ? command.resolutionNote() : "")
        ));

        return mapToResult(saved);
    }

    @Override
    public ScheduleConflictResult assignScheduleConflictHandler(AssignScheduleConflictHandlerCommand command) {
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        );

        validateAssignedHandler(command.assignedHandlerId());

        ScheduleConflict conflict = loadConflictPort.findById(command.conflictId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cảnh báo xung đột lịch với ID: " + command.conflictId()));

        conflict.assignHandler(command.assignedHandlerId());
        ScheduleConflict saved = saveConflictPort.save(conflict);

        auditLogPort.save(AuditLog.createChange(
                currentUserId,
                "ASSIGN_SCHEDULE_CONFLICT_HANDLER",
                "schedule_conflict_warnings",
                command.conflictId(),
                null,
                "employee_id=" + conflict.getEmployeeId() + ";assigned_handler_id=" + command.assignedHandlerId()
        ));

        return mapToResult(saved);
    }

    private void validateAssignedHandler(Long handlerId) {
        if (handlerId != null) {
            Employee handler = loadEmployeePort.findById(new EmployeeId(handlerId))
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người xử lý (Employee) với ID: " + handlerId));
            if (handler.getStatus() != EmployeeStatus.ACTIVE) {
                throw new IllegalStateException("Người xử lý được gán không ở trạng thái đang hoạt động (ACTIVE)");
            }
        }
    }

    private List<ScheduleConflict> scanInternal(Integer year, Integer startWeek, Integer endWeek) {
        List<Employee> activeEmployees = loadEmployeePort.findAllActive();
        if (activeEmployees.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Employee> activeEmpMap = activeEmployees.stream()
                .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

        // Candidate employees are strictly active employees
        Set<Long> candidateEmpIds = activeEmpMap.keySet();

        // 1. Bulk query allocations across the entire week range [startWeek, endWeek]
        List<WeeklyProjectAllocation> allAllocations = loadAllocationPort
                .loadAllocationsForEmployeesInWeekRange(null, year, startWeek, endWeek);

        // 2. Load project names once for all allocations across the range
        Set<ProjectId> allProjectIds = allAllocations.stream()
                .map(WeeklyProjectAllocation::getProjectId)
                .filter(Objects::nonNull)
                .map(ProjectId::new)
                .collect(Collectors.toSet());

        Map<Long, String> projectNameMap = allProjectIds.isEmpty() ? Collections.emptyMap() :
                loadProjectPort.findAllById(new ArrayList<>(allProjectIds)).stream()
                        .collect(Collectors.toMap(p -> p.getId().value(), Project::getProjectName, (p1, p2) -> p1));

        // 3. Bulk query approved leaves across active employees and all weeks in range
        List<YearWeek> allWeekRange = IntStream.rangeClosed(startWeek, endWeek)
                .mapToObj(w -> new YearWeek(year, w))
                .collect(Collectors.toList());

        Map<Long, Map<YearWeek, BigDecimal>> approvedLeavesMap = loadApprovedLeavesPort
                .loadApprovedLeaveHoursForEmployeesAndWeeks(new ArrayList<>(candidateEmpIds), allWeekRange);

        // 4. Bulk query existing conflicts across the entire week range [startWeek, endWeek]
        List<ScheduleConflict> existingConflicts = loadConflictPort.findConflicts(year, startWeek, endWeek, null, null, null);
        Map<ConflictKey, ScheduleConflict> existingConflictMap = existingConflicts.stream()
                .collect(Collectors.toMap(
                        c -> new ConflictKey(c.getEmployeeId(), c.getWeekNumber(), c.getConflictType()),
                        c -> c,
                        (c1, c2) -> c1
                ));

        // Group allocations by week number and employeeId
        Map<Integer, Map<Long, List<WeeklyProjectAllocation>>> allocationsByWeekAndEmp = allAllocations.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getYearWeek().weekNumber(),
                        Collectors.groupingBy(WeeklyProjectAllocation::getEmployeeId)
                ));

        List<ScheduleConflict> resultConflicts = new ArrayList<>();

        for (int week = startWeek; week <= endWeek; week++) {
            final int currentWeekNum = week;
            YearWeek yw = new YearWeek(year, currentWeekNum);
            Map<Long, List<WeeklyProjectAllocation>> allocationsByEmp = allocationsByWeekAndEmp.getOrDefault(currentWeekNum, Collections.emptyMap());

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

                // Scenario 1: Multi-project overload allocation conflict
                boolean isMultiProjectConflict = projectIdsSet.size() >= 2 &&
                        totalAllocatedHours.compareTo(standardCapacity) > 0;

                if (isMultiProjectConflict) {
                    BigDecimal excessHours = totalAllocatedHours.subtract(standardCapacity).max(BigDecimal.ZERO);

                    String projectIdsStr = projectIdsSet.stream().map(String::valueOf).collect(Collectors.joining(","));
                    List<String> pNames = projectIdsSet.stream()
                            .map(pId -> projectNameMap.getOrDefault(pId, "Dự án #" + pId))
                            .collect(Collectors.toList());
                    String projectNamesStr = String.join(", ", pNames);

                    ScheduleConflict existing = existingConflictMap.get(new ConflictKey(empId, currentWeekNum, ConflictType.MULTI_PROJECT_ALLOCATION));

                    if (existing != null) {
                        if (existing.getStatus() == ScheduleConflictStatus.RESOLVED) {
                            existing.reopenAsRecurrent("Nguyên nhân gây xung đột vẫn còn sau khi rà soát lại.");
                        }
                        existing.setProjectIds(projectIdsStr);
                        existing.setProjectNames(projectNamesStr);
                        existing.setTotalAllocatedHours(totalAllocatedHours);
                        existing.setNetAvailableHours(netAvailableHours);
                        existing.setExcessHours(excessHours);
                        existing.setDetails("Phân bổ trên " + projectIdsSet.size() + " dự án (" + projectNamesStr + ") với tổng " + totalAllocatedHours + "h/tuần");

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
                        resultConflicts.add(newConflict);
                    }
                }

                // Scenario 2: Leave & Allocation conflict
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

                    ScheduleConflict existingLeaveConflict = existingConflictMap.get(new ConflictKey(empId, currentWeekNum, ConflictType.LEAVE_ALLOCATION_CONFLICT));

                    if (existingLeaveConflict != null) {
                        if (existingLeaveConflict.getStatus() == ScheduleConflictStatus.RESOLVED) {
                            existingLeaveConflict.reopenAsRecurrent("Nguyên nhân gây xung đột vẫn còn sau khi rà soát lại.");
                        }
                        existingLeaveConflict.setProjectIds(projectIdsStr);
                        existingLeaveConflict.setProjectNames(projectNamesStr);
                        existingLeaveConflict.setLeaveInfo(leaveInfoStr);
                        existingLeaveConflict.setTotalAllocatedHours(totalAllocatedHours);
                        existingLeaveConflict.setNetAvailableHours(netAvailableHours);
                        existingLeaveConflict.setExcessHours(excessHours);
                        existingLeaveConflict.setDetails("Có đơn nghỉ phép đã duyệt (" + approvedLeaveHours + "h) trùng tuần được phân bổ vào các dự án: " + projectNamesStr);

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
                        resultConflicts.add(newLeaveConflict);
                    }
                }
            }
        }

        if (resultConflicts.isEmpty()) {
            return Collections.emptyList();
        }

        return saveConflictPort.saveAll(resultConflicts);
    }

    private record ConflictKey(Long employeeId, Integer weekNumber, ConflictType conflictType) {}

    private List<ScheduleConflictResult> mapToResults(List<ScheduleConflict> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> empIdSet = conflicts.stream()
                .flatMap(c -> Stream.of(c.getEmployeeId(), c.getAssignedHandlerId(), c.getNotifiedBy(), c.getResolvedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<EmployeeId> empIds = empIdSet.stream().map(EmployeeId::new).collect(Collectors.toList());

        Map<Long, Employee> empMap = empIds.isEmpty() ? Collections.emptyMap() :
                loadEmployeePort.findAllByIdIn(empIds).stream()
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
                .map(c -> mapToResult(c, empMap, orgUnitMap))
                .collect(Collectors.toList());
    }

    private ScheduleConflictResult mapToResult(ScheduleConflict conflict) {
        if (conflict == null) {
            return null;
        }
        List<ScheduleConflictResult> results = mapToResults(Collections.singletonList(conflict));
        return (results != null && !results.isEmpty()) ? results.get(0) : null;
    }

    private ScheduleConflictResult mapToResult(ScheduleConflict conflict, Map<Long, Employee> empMap, Map<Long, String> orgUnitMap) {
        Employee emp = empMap.get(conflict.getEmployeeId());
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
            case REOPENED -> "TÁI PHÁT";
        };

        String weekLabel = "Tuần " + conflict.getWeekNumber() + "/" + conflict.getYearNumber();

        Employee handler = conflict.getAssignedHandlerId() != null ? empMap.get(conflict.getAssignedHandlerId()) : null;
        String handlerCode = handler != null ? handler.getEmployeeCode() : (conflict.getAssignedHandlerId() != null ? "NV" + conflict.getAssignedHandlerId() : null);
        String handlerName = handler != null ? handler.getFullName() : (conflict.getAssignedHandlerId() != null ? "Người xử lý #" + conflict.getAssignedHandlerId() : null);

        Employee notifiedUser = conflict.getNotifiedBy() != null ? empMap.get(conflict.getNotifiedBy()) : null;
        String notifiedUserName = notifiedUser != null ? notifiedUser.getFullName() : (conflict.getNotifiedBy() != null ? "User #" + conflict.getNotifiedBy() : null);

        Employee resolvedUser = conflict.getResolvedBy() != null ? empMap.get(conflict.getResolvedBy()) : null;
        String resolvedUserName = resolvedUser != null ? resolvedUser.getFullName() : (conflict.getResolvedBy() != null ? "User #" + conflict.getResolvedBy() : null);

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
                notifiedUserName,
                conflict.getAssignedHandlerId(),
                handlerCode,
                handlerName,
                conflict.getResolutionNote(),
                conflict.getIsRecurrent(),
                conflict.getRecurrentNote(),
                conflict.getResolvedAt(),
                conflict.getResolvedBy(),
                resolvedUserName,
                conflict.getCreatedAt(),
                conflict.getUpdatedAt()
        );
    }
}
