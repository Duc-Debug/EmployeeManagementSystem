package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.BulkAllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.BulkAllocationResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.BulkAllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException;
import com.hrm.employeemanagement.domain.exception.allocation.ProjectInactiveException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * NCL-06-CN-006: Nghiệp vụ phân bổ hàng loạt cho nhiều tuần.
 * Xử lý kiểm tra giờ khả dụng (QTN-11), hạn hợp đồng (QTN-05), trạng thái dự án
 * (QTN-08),
 * phân quyền (TC-03), phân bổ từng phần (TC-02) và ghi nhật ký kiểm toán
 * (TC-04).
 */
public class BulkResourceAllocationService implements BulkAllocateResourceUseCase {

    private final AuthorizationService authorizationService;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    public BulkResourceAllocationService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort) {
        this.authorizationService = Objects.requireNonNull(authorizationService,
                "AuthorizationService must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort,
                "LoadWeeklyAvailabilityPort must not be null");
        this.saveAllocationPort = Objects.requireNonNull(saveAllocationPort,
                "SaveWeeklyProjectAllocationPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort,
                "LoadWeeklyProjectAllocationPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort,
                "SaveAuditLogInNewTransactionPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
    }

    @Override
    public BulkAllocationResult bulkAllocateResource(BulkAllocateResourceCommand command) {
        // [TC-03] Kiểm tra quyền hạn cơ bản
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));

        // Load nhân sự (Pessimistic write lock)
        Employee employee = loadEmployeePort.findByIdForUpdate(new EmployeeId(command.employeeId()))
                .orElseThrow(
                        () -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + command.employeeId()));

        // Kiểm tra Data Scope cho nhân sự
        requireOrgUnitInDataScope(currentUser, employee.getOrgUnitId(), PermissionCode.RESOURCE_ALLOCATION_MANAGE);

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeInactiveException("Không thể phân bổ cho nhân sự không còn ở trạng thái hoạt động");
        }

        // Load dự án
        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        // Kiểm tra Data Scope cho dự án
        requireOrgUnitInDataScope(currentUser, project.getOrgUnitId(), PermissionCode.RESOURCE_ALLOCATION_MANAGE);

        // QTN-08: Kiểm tra trạng thái dự án
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new ProjectInactiveException("Không thể phân bổ nhân sự vào dự án không ở trạng thái hoạt động");
        }

        // Sinh danh sách các tuần trong khoảng yêu cầu
        List<YearWeek> targetWeeks = generateYearWeeks(
                command.fromYear(), command.fromWeek(),
                command.toYear(), command.toWeek());

        // Batch load thông tin availability & allocation trong dải tuần
        List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(
                List.of(employee.getIdValue()), targetWeeks);
        Map<YearWeek, WeeklyAvailability> availabilityMap = availabilities.stream()
                .collect(Collectors.toMap(WeeklyAvailability::getYearWeek, a -> a, (a, b) -> a));

        List<WeeklyProjectAllocation> existingAllocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(
                List.of(employee.getIdValue()), targetWeeks);
        Map<YearWeek, List<WeeklyProjectAllocation>> allocationMap = existingAllocations.stream()
                .collect(Collectors.groupingBy(WeeklyProjectAllocation::getYearWeek));

        List<BulkAllocationResult.AllocatedWeekSummary> successWeeks = new ArrayList<>();
        List<BulkAllocationResult.BlockedWeekSummary> blockedWeeks = new ArrayList<>();
        List<WeeklyProjectAllocation> allocationsToSave = new ArrayList<>();

        if (command.allocatedHoursPerWeek() != null && command.allocationPercentagePerWeek() != null) {
            throw new IllegalArgumentException("Không được cung cấp đồng thời số giờ phân bổ và tỷ lệ phần trăm phân bổ mỗi tuần");
        }

        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;

        for (YearWeek yw : targetWeeks) {
            // 1. Tính số giờ khả dụng (Net Available)
            WeeklyAvailability avail = availabilityMap.get(yw);
            BigDecimal netAvailable = avail != null ? avail.getNetAvailableHours() : BigDecimal.valueOf(standardHours);

            // Tính toán effectiveHours và effectivePercentage cho tuần này
            BigDecimal effectiveHours;
            BigDecimal effectivePercentage;

            if (command.allocationPercentagePerWeek() != null) {
                effectivePercentage = command.allocationPercentagePerWeek();
                effectiveHours = netAvailable.multiply(effectivePercentage)
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            } else {
                effectiveHours = command.allocatedHoursPerWeek();
                if (netAvailable.compareTo(BigDecimal.ZERO) > 0) {
                    effectivePercentage = effectiveHours.multiply(BigDecimal.valueOf(100))
                            .divide(netAvailable, 2, java.math.RoundingMode.HALF_UP);
                } else {
                    effectivePercentage = BigDecimal.ZERO;
                }
            }

            // 2. QTN-05: Kiểm tra ngày kết thúc hợp đồng
            LocalDate weekStartDate = yw.getStartDate();
            if (employee.getContractEndDate() != null && employee.getContractEndDate().isBefore(weekStartDate)) {
                blockedWeeks.add(new BulkAllocationResult.BlockedWeekSummary(
                        yw.year(),
                        yw.weekNumber(),
                        "CONTRACT_EXPIRED",
                        "Nhân sự đã kết thúc hợp đồng lao động trước tuần " + yw.weekNumber() + "/" + yw.year(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        effectiveHours));
                continue;
            }

            // 3. Tính tổng giờ phân bổ cho các dự án KHÁC và tổng hiện tại của tất cả dự án
            List<WeeklyProjectAllocation> weekAllocs = allocationMap.getOrDefault(yw, List.of());
            BigDecimal otherProjectsSum = weekAllocs.stream()
                    .filter(a -> !a.getProjectId().equals(command.projectId()))
                    .map(WeeklyProjectAllocation::getAllocatedHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal currentTotalAllocated = weekAllocs.stream()
                    .map(WeeklyProjectAllocation::getAllocatedHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalRequested = otherProjectsSum.add(effectiveHours);

            // 4. QTN-11: Kiểm tra giới hạn công suất tuần
            if (totalRequested.compareTo(netAvailable) > 0) {
                blockedWeeks.add(new BulkAllocationResult.BlockedWeekSummary(
                        yw.year(),
                        yw.weekNumber(),
                        "CAPACITY_EXCEEDED",
                        "Không thể phân bổ: Tổng số giờ phân bổ (" + totalRequested + "h) vượt quá số giờ khả dụng ("
                                + netAvailable + "h) của nhân sự trong tuần " + yw.weekNumber() + "/" + yw.year(),
                        netAvailable,
                        currentTotalAllocated,
                        effectiveHours));
                continue;
            }

            // 5. Tuần hợp lệ: Chuẩn bị cập nhật / tạo mới bản ghi phân bổ
            Optional<WeeklyProjectAllocation> existingOpt = weekAllocs.stream()
                    .filter(a -> a.getProjectId().equals(command.projectId()))
                    .findFirst();

            WeeklyProjectAllocation alloc;
            if (existingOpt.isPresent()) {
                alloc = existingOpt.get();
                alloc.updateAllocation(effectiveHours, effectivePercentage);
            } else {
                alloc = WeeklyProjectAllocation.createNew(
                        employee.getIdValue(),
                        command.projectId(),
                        yw,
                        effectiveHours,
                        effectivePercentage);
            }
            allocationsToSave.add(alloc);
            BigDecimal remaining = netAvailable.subtract(totalRequested);
            successWeeks.add(new BulkAllocationResult.AllocatedWeekSummary(
                    yw.year(),
                    yw.weekNumber(),
                    effectiveHours,
                    remaining));
        }

        // Lưu các dòng phân bổ hợp lệ vào CSDL
        for (WeeklyProjectAllocation alloc : allocationsToSave) {
            saveAllocationPort.save(alloc);
        }

        // [TC-04] Ghi nhật ký kiểm toán (Audit Log)
        if (!successWeeks.isEmpty() || !blockedWeeks.isEmpty()) {
            String allocationDetail = command.allocationPercentagePerWeek() != null
                    ? command.allocationPercentagePerWeek() + "%/tuần"
                    : command.allocatedHoursPerWeek() + "h/tuần";

            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "RESOURCE_BULK_ALLOCATED",
                    "weekly_project_allocations",
                    null,
                    "Phân bổ hàng loạt: " + successWeeks.size() + " tuần thành công, " + blockedWeeks.size()
                            + " tuần bị chặn",
                    "Nhân sự ID: " + employee.getIdValue() + ", Dự án ID: " + command.projectId() + ", Phân bổ: "
                            + allocationDetail));
        }

        return new BulkAllocationResult(
                command.employeeId(),
                command.projectId(),
                targetWeeks.size(),
                successWeeks.size(),
                blockedWeeks.size(),
                successWeeks,
                blockedWeeks);
    }

    private List<YearWeek> generateYearWeeks(int fromYear, int fromWeek, int toYear, int toWeek) {
        YearWeek start = YearWeek.of(fromYear, fromWeek);
        YearWeek end = YearWeek.of(toYear, toWeek);

        if (start.year() > end.year() || (start.year() == end.year() && start.weekNumber() > end.weekNumber())) {
            throw new IllegalArgumentException(
                    "Khoảng tuần không hợp lệ: Tuần bắt đầu phải trước hoặc bằng tuần kết thúc");
        }

        List<YearWeek> weeks = new ArrayList<>();
        int curYear = fromYear;
        int curWeek = fromWeek;
        while (curYear < toYear || (curYear == toYear && curWeek <= toWeek)) {
            weeks.add(YearWeek.of(curYear, curWeek));
            int maxWeeks = YearWeek.maxWeeksInYear(curYear);
            curWeek++;
            if (curWeek > maxWeeks) {
                curYear++;
                curWeek = 1;
            }
            if (weeks.size() > 52) {
                throw new IllegalArgumentException("Khoảng tuần phân bổ hàng loạt không được vượt quá 52 tuần");
            }
        }
        return weeks;
    }

    private void requireOrgUnitInDataScope(User currentUser, Long orgUnitId, PermissionCode permission) {
        if (!isOrgUnitInDataScope(currentUser, orgUnitId)) {
            throw new PermissionDeniedException(permission);
        }
    }

    private boolean isOrgUnitInDataScope(User currentUser, Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> false;
            case ORGANIZATION_BRANCH -> currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, currentUser.getScopeOrgUnitId());
        };
    }
}
