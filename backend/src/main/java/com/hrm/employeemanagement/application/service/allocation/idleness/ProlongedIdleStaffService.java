package com.hrm.employeemanagement.application.service.allocation.idleness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffCommand;
import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdleStaffItemResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessQuery;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessReportResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessWeeklyDetailResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.AcknowledgeProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.GetProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.idleness.ProlongedIdlenessPolicy;
import com.hrm.employeemanagement.domain.allocation.idleness.WeeklyIdlenessDetail;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdPolicy;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;

/**
 * Pure Java Application Service cho Use Case NCL-07-CN-006:
 * Cảnh báo nhân sự nhàn rỗi kéo dài (Warning about prolonged staff idleness) theo QTN-23.
 */
public class ProlongedIdleStaffService implements GetProlongedIdleStaffUseCase, AcknowledgeProlongedIdleStaffUseCase {

    private final AuthorizationService authorizationService;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadCapacityThresholdPort loadCapacityThresholdPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final SaveAuditLogInNewTransactionPort auditLogPort;
    private final SimulatedNotificationPort notificationPort;

    public ProlongedIdleStaffService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadCapacityThresholdPort loadCapacityThresholdPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadHolidaysPort loadHolidaysPort,
            SaveAuditLogInNewTransactionPort auditLogPort,
            SimulatedNotificationPort notificationPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "loadOrgUnitPort must not be null");
        this.loadCapacityThresholdPort = Objects.requireNonNull(loadCapacityThresholdPort, "loadCapacityThresholdPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "loadAllocationPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "loadApprovedLeavesPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "loadHolidaysPort must not be null");
        this.auditLogPort = Objects.requireNonNull(auditLogPort, "auditLogPort must not be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "notificationPort must not be null");
    }

    @Override
    public ProlongedIdlenessReportResult getProlongedIdleStaff(ProlongedIdlenessQuery query) {
        // 1. Kiểm tra quyền truy cập (NCL-07-CN-006-TC-03):
        // Chỉ Quản lý nguồn lực (VT-03) hoặc Quản trị viên (VT-06) mới được truy cập
        authorizationService.requireAny(
                PermissionCode.RESOURCE_ALLOCATION_READ,
                PermissionCode.RESOURCE_ALLOCATION_MANAGE,
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ
        );

        // 2. Xác định khoảng tuần mục tiêu
        int fromYear;
        int fromWeek;
        if (query.fromYear() != null && query.fromWeek() != null) {
            fromYear = query.fromYear();
            fromWeek = query.fromWeek();
        } else {
            LocalDate now = LocalDate.now();
            fromYear = now.get(IsoFields.WEEK_BASED_YEAR);
            fromWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        }

        int durationWeeks = Math.min(Math.max(1, query.durationWeeks() != null ? query.durationWeeks() : 4), 16);
        int consecutiveThreshold = query.consecutiveThreshold() != null && query.consecutiveThreshold() > 0
                ? query.consecutiveThreshold()
                : ProlongedIdlenessPolicy.DEFAULT_CONSECUTIVE_WEEKS;

        List<YearWeek> targetWeeks = buildTargetWeeks(fromYear, fromWeek, durationWeeks);

        // 3. Tra cứu cấu hình ngưỡng nhàn rỗi theo QTN-23 (từ DB cấu hình của Ban giám đốc)
        BigDecimal idleThreshold = resolveEffectiveIdleThreshold(query.orgUnitId());

        // 4. Lấy danh sách nhân sự thuộc phạm vi tìm kiếm
        List<Employee> employees = loadEmployeesInScope(query.orgUnitId());
        if (query.search() != null && !query.search().isBlank()) {
            String searchPattern = query.search().trim().toLowerCase();
            employees = employees.stream()
                    .filter(emp -> (emp.getFullName() != null && emp.getFullName().toLowerCase().contains(searchPattern))
                            || (emp.getEmployeeCode() != null && emp.getEmployeeCode().toLowerCase().contains(searchPattern))
                            || (emp.getProfessionalRole() != null && emp.getProfessionalRole().toLowerCase().contains(searchPattern)))
                    .toList();
        }

        // Tạo map OrgUnit để hiển thị tên phòng ban
        List<OrgUnit> allUnits = loadOrgUnitPort.findAll();
        Map<Long, String> orgUnitNameMap = allUnits.stream()
                .collect(Collectors.toMap(u -> u.getId().getValue(), OrgUnit::getUnitName, (u1, u2) -> u1));

        String orgUnitName = "Toàn công ty";
        if (query.orgUnitId() != null) {
            orgUnitName = orgUnitNameMap.getOrDefault(query.orgUnitId(), "Bộ phận " + query.orgUnitId());
        }

        if (employees.isEmpty()) {
            return new ProlongedIdlenessReportResult(
                    query.orgUnitId(),
                    orgUnitName,
                    fromYear,
                    fromWeek,
                    durationWeeks,
                    idleThreshold,
                    consecutiveThreshold,
                    0,
                    Collections.emptyList()
            );
        }

        // 5. Batch load dữ liệu phụ thuộc (Allocations, Leaves, Holidays)
        List<Long> employeeIds = employees.stream().map(Employee::getIdValue).toList();
        List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, BigDecimal> allocationMap = new HashMap<>();
        for (WeeklyProjectAllocation alloc : allocations) {
            if (alloc.getAllocatedHours() != null) {
                String key = alloc.getEmployeeId() + "_" + alloc.getYearWeek().year() + "_" + alloc.getYearWeek().weekNumber();
                allocationMap.merge(key, alloc.getAllocatedHours(), BigDecimal::add);
            }
        }

        Map<Long, Map<YearWeek, BigDecimal>> leaveMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(employeeIds, targetWeeks);

        LocalDate minDate = targetWeeks.get(0).getStartDate();
        LocalDate maxDate = targetWeeks.get(targetWeeks.size() - 1).getEndDate();
        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(minDate, maxDate);
        Set<DayOfWeek> workingDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

        Map<YearWeek, Integer> holidayHoursByWeek = targetWeeks.stream()
                .collect(Collectors.toMap(
                        yw -> yw,
                        yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays)
                ));

        // 6. Rà soát từng nhân sự và tính toán chuỗi tuần nhàn rỗi
        List<ProlongedIdleStaffItemResult> idleItems = new ArrayList<>();

        for (Employee emp : employees) {
            Long empId = emp.getIdValue();
            int standardHours = emp.getStandardHoursPerWeek() != null && emp.getStandardHoursPerWeek() > 0
                    ? emp.getStandardHoursPerWeek()
                    : 40;

            List<WeeklyIdlenessDetail> weeklyDetails = new ArrayList<>();

            for (YearWeek yw : targetWeeks) {
                int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);

                BigDecimal approvedLeaveHours = BigDecimal.ZERO;
                if (leaveMap != null && leaveMap.containsKey(empId)) {
                    approvedLeaveHours = leaveMap.get(empId).getOrDefault(yw, BigDecimal.ZERO);
                }

                BigDecimal availableHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(
                        standardHours,
                        holidayHours,
                        approvedLeaveHours
                );

                String key = empId + "_" + yw.year() + "_" + yw.weekNumber();
                BigDecimal allocatedHours = allocationMap.getOrDefault(key, BigDecimal.ZERO);

                BigDecimal emptyHours = ProlongedIdlenessPolicy.calculateEmptyHours(allocatedHours, availableHours);
                BigDecimal utilization = ProlongedIdlenessPolicy.calculateUtilizationRate(allocatedHours, availableHours);
                boolean underutilized = ProlongedIdlenessPolicy.isWeekUnderutilized(allocatedHours, availableHours, idleThreshold);

                boolean fullLeave = (approvedLeaveHours.compareTo(BigDecimal.valueOf(standardHours)) >= 0)
                        || (availableHours.compareTo(BigDecimal.ZERO) <= 0 && approvedLeaveHours.compareTo(BigDecimal.ZERO) > 0);

                weeklyDetails.add(new WeeklyIdlenessDetail(
                        yw.year(),
                        yw.weekNumber(),
                        BigDecimal.valueOf(standardHours).setScale(1, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(holidayHours).setScale(1, RoundingMode.HALF_UP),
                        approvedLeaveHours.setScale(1, RoundingMode.HALF_UP),
                        availableHours.setScale(1, RoundingMode.HALF_UP),
                        allocatedHours.setScale(1, RoundingMode.HALF_UP),
                        emptyHours,
                        utilization,
                        underutilized,
                        fullLeave
                ));
            }

            // Áp dụng chính sách kiểm tra nhàn rỗi kéo dài (TC-01, TC-02)
            if (ProlongedIdlenessPolicy.qualifiesForProlongedIdlenessAlert(weeklyDetails, consecutiveThreshold)) {
                int consecutiveWeeks = ProlongedIdlenessPolicy.findMaxConsecutiveIdleWeeks(weeklyDetails);

                BigDecimal totalEmpty = weeklyDetails.stream()
                        .map(WeeklyIdlenessDetail::emptyHours)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Tính % sử dụng trung bình trong các tuần có availableHours > 0
                List<BigDecimal> validUtils = weeklyDetails.stream()
                        .filter(d -> d.availableHours().compareTo(BigDecimal.ZERO) > 0)
                        .map(WeeklyIdlenessDetail::utilizationPercentage)
                        .toList();

                BigDecimal avgUtil = BigDecimal.ZERO;
                if (!validUtils.isEmpty()) {
                    BigDecimal sumUtil = validUtils.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    avgUtil = sumUtil.divide(BigDecimal.valueOf(validUtils.size()), 1, RoundingMode.HALF_UP);
                }

                List<ProlongedIdlenessWeeklyDetailResult> detailResults = weeklyDetails.stream()
                        .map(d -> new ProlongedIdlenessWeeklyDetailResult(
                                d.year(),
                                d.weekNumber(),
                                d.availableHours(),
                                d.allocatedHours(),
                                d.emptyHours(),
                                d.utilizationPercentage(),
                                d.isUnderutilized(),
                                d.isFullLeaveWeek()
                        ))
                        .toList();

                String deptName = emp.getOrgUnitId() != null
                        ? orgUnitNameMap.getOrDefault(emp.getOrgUnitId(), "Phòng ban " + emp.getOrgUnitId())
                        : "Chưa phân bổ";

                idleItems.add(new ProlongedIdleStaffItemResult(
                        emp.getIdValue(),
                        emp.getEmployeeCode(),
                        emp.getFullName(),
                        emp.getOrgUnitId(),
                        deptName,
                        emp.getProfessionalRole(),
                        consecutiveWeeks,
                        totalEmpty,
                        avgUtil,
                        detailResults
                ));
            }
        }

        // Sắp xếp: ưu tiên số tuần nhàn rỗi giảm dần, sau đó tổng số giờ trống giảm dần
        idleItems.sort(Comparator.comparingInt(ProlongedIdleStaffItemResult::consecutiveIdleWeeks).reversed()
                .thenComparing(ProlongedIdleStaffItemResult::totalEmptyHours, Comparator.reverseOrder()));

        return new ProlongedIdlenessReportResult(
                query.orgUnitId(),
                orgUnitName,
                fromYear,
                fromWeek,
                durationWeeks,
                idleThreshold,
                consecutiveThreshold,
                idleItems.size(),
                idleItems
        );
    }

    @Override
    public AcknowledgeProlongedIdleStaffResult acknowledgeProlongedIdleStaff(AcknowledgeProlongedIdleStaffCommand command) {
        // 1. Kiểm tra quyền hạn (NCL-07-CN-006-TC-04):
        // Quản lý nguồn lực (VT-03) hoặc Admin (VT-06)
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_ALLOCATION_MANAGE,
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY
        );

        // 2. Kiểm tra sự tồn tại của nhân sự
        Employee employee = loadEmployeePort.findById(new EmployeeId(command.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + command.employeeId()));

        LocalDateTime now = LocalDateTime.now();

        // 3. Ghi nhận nhật ký kiểm toán (NCL-07-CN-006-TC-04)
        auditLogPort.save(AuditLog.createChange(
                currentUserId,
                "ACKNOWLEDGE_PROLONGED_IDLENESS",
                "employees",
                command.employeeId(),
                null,
                "actionTaken=" + command.actionTaken() + ";notes=" + (command.notes() != null ? command.notes() : "")
        ));

        // 4. Phát thông báo mô phỏng
        notificationPort.sendScheduleConflictWarningNotification(
                "rm@company.com",
                "Quản lý nguồn lực",
                employee.getFullName(),
                "Xử lý cảnh báo nhân sự nhàn rỗi kéo dài",
                "Hành động xử lý: " + command.actionTaken() + ". Ghi chú: " + (command.notes() != null ? command.notes() : "Không có")
        );

        return new AcknowledgeProlongedIdleStaffResult(
                command.employeeId(),
                employee.getFullName(),
                command.actionTaken(),
                command.notes(),
                currentUserId,
                now,
                "ACKNOWLEDGED"
        );
    }

    private BigDecimal resolveEffectiveIdleThreshold(Long orgUnitId) {
        if (orgUnitId != null) {
            Optional<CapacityThresholdConfig> unitConfig = loadCapacityThresholdPort.findByScope(CapacityThresholdScope.ORG_UNIT, orgUnitId);
            if (unitConfig.isPresent()) {
                return unitConfig.get().getIdleThreshold();
            }
        }

        Optional<CapacityThresholdConfig> compConfig = loadCapacityThresholdPort.findByScope(CapacityThresholdScope.COMPANY, null);
        if (compConfig.isPresent()) {
            return compConfig.get().getIdleThreshold();
        }

        return CapacityThresholdPolicy.DEFAULT_IDLE_THRESHOLD;
    }

    private List<Employee> loadEmployeesInScope(Long orgUnitId) {
        if (orgUnitId != null) {
            List<Long> branchIds = resolveScopeBranchOrgUnitIds(orgUnitId);
            return loadEmployeePort.findActiveByOrgUnitIds(branchIds);
        }
        return loadEmployeePort.findAllActive();
    }

    private List<Long> resolveScopeBranchOrgUnitIds(Long orgUnitId) {
        if (orgUnitId == null) {
            return null;
        }
        List<OrgUnit> allUnits = loadOrgUnitPort.findAll();
        Set<Long> result = new java.util.HashSet<>();
        result.add(orgUnitId);
        boolean added = true;
        while (added) {
            added = false;
            for (OrgUnit u : allUnits) {
                if (u.getParentId() != null && result.contains(u.getParentId().getValue())) {
                    if (result.add(u.getId().getValue())) {
                        added = true;
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    private List<YearWeek> buildTargetWeeks(int fromYear, int fromWeek, int durationWeeks) {
        List<YearWeek> list = new ArrayList<>(durationWeeks);
        YearWeek start = YearWeek.of(fromYear, fromWeek);
        LocalDate monday = start.getStartDate();
        for (int i = 0; i < durationWeeks; i++) {
            int y = monday.get(IsoFields.WEEK_BASED_YEAR);
            int w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            list.add(YearWeek.of(y, w));
            monday = monday.plusWeeks(1);
        }
        return list;
    }
}
