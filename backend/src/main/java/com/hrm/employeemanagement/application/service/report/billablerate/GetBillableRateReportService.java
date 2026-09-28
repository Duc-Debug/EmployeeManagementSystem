package com.hrm.employeemanagement.application.service.report.billablerate;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateExport;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateItem;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateQuery;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateResult;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateSummary;
import com.hrm.employeemanagement.application.dto.report.billablerate.DepartmentBillableRateSummary;
import com.hrm.employeemanagement.application.port.inbound.report.billablerate.ExportBillableRateReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.billablerate.GetBillableRateReportUseCase;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.report.billablerate.LoadBillableRateTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.report.billablerate.LoadBillableRateTimesheetPort.EmployeeHoursData;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Application Service thực thi Use Case Báo cáo tỷ lệ giờ tính phí (NCL-10-CN-002).
 */
public class GetBillableRateReportService implements GetBillableRateReportUseCase, ExportBillableRateReportUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadBillableRateTimesheetPort loadBillableRateTimesheetPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public GetBillableRateReportService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadBillableRateTimesheetPort loadBillableRateTimesheetPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.loadBillableRateTimesheetPort = Objects.requireNonNull(loadBillableRateTimesheetPort, "LoadBillableRateTimesheetPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public BillableRateResult execute(BillableRateQuery query) {
        // 1. Phân quyền: Kiểm tra BILLABLE_HOURS_REPORT_READ
        Long currentUserId = authorizationService.require(PermissionCode.BILLABLE_HOURS_REPORT_READ);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Data Scope Validation (QTN-01)
        Long effectiveOrgUnitId;
        try {
            switch (currentUser.getDataScope()) {
                case COMPANY -> effectiveOrgUnitId = query != null ? query.orgUnitId() : null;
                case ORGANIZATION_BRANCH -> {
                    if (currentUser.getScopeOrgUnitId() == null) {
                        throw new PermissionDeniedException(PermissionCode.BILLABLE_HOURS_REPORT_READ);
                    }
                    if (query != null && query.orgUnitId() != null) {
                        boolean inScope = loadOrgUnitPort.existsInOrgUnitBranch(query.orgUnitId(), currentUser.getScopeOrgUnitId());
                        if (!inScope) {
                            throw new PermissionDeniedException(PermissionCode.BILLABLE_HOURS_REPORT_READ);
                        }
                        effectiveOrgUnitId = query.orgUnitId();
                    } else {
                        effectiveOrgUnitId = currentUser.getScopeOrgUnitId();
                    }
                }
                default -> throw new PermissionDeniedException(PermissionCode.BILLABLE_HOURS_REPORT_READ);
            }
        } catch (PermissionDeniedException e) {
            recordDeniedAuditLog(currentUserId, "DATA_SCOPE_UNAUTHORIZED");
            throw e;
        }

        String orgUnitName = "Toàn công ty";
        if (effectiveOrgUnitId != null) {
            OrgUnit unit = loadOrgUnitPort.findById(new OrgUnitId(effectiveOrgUnitId))
                    .orElseThrow(() -> new OrgUnitNotFoundException("Không tìm thấy bộ phận: " + effectiveOrgUnitId));
            orgUnitName = unit.getUnitName();
        }

        // 3. Xử lý khoảng tuần (Week Range Validation)
        YearWeek startWeek;
        YearWeek endWeek;

        boolean hasFrom = query != null && (query.fromYear() != null || query.fromWeek() != null);
        boolean hasTo = query != null && (query.toYear() != null || query.toWeek() != null);

        if (!hasFrom && !hasTo) {
            LocalDate now = LocalDate.now();
            int currentYear = now.get(IsoFields.WEEK_BASED_YEAR);
            int currentWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            startWeek = YearWeek.of(currentYear, currentWeek);
            endWeek = startWeek;
        } else if (!hasFrom && hasTo) {
            throw new IllegalArgumentException("Phải cung cấp đồng thời fromYear và fromWeek khi chỉ định toYear/toWeek");
        } else {
            if (query.fromYear() == null || query.fromWeek() == null) {
                throw new IllegalArgumentException("fromYear và fromWeek phải được cung cấp cùng nhau");
            }
            startWeek = YearWeek.of(query.fromYear(), query.fromWeek());

            if (hasTo) {
                if (query.toYear() == null || query.toWeek() == null) {
                    throw new IllegalArgumentException("toYear và toWeek phải được cung cấp cùng nhau");
                }
                endWeek = YearWeek.of(query.toYear(), query.toWeek());
            } else {
                endWeek = startWeek;
            }

            if (startWeek.isAfter(endWeek)) {
                throw new IllegalArgumentException("Tuần bắt đầu không được lớn hơn tuần kết thúc");
            }
        }

        long numberOfWeeks = ChronoUnit.WEEKS.between(startWeek.getStartDate(), endWeek.getStartDate()) + 1;
        if (numberOfWeeks > 104) {
            throw new IllegalArgumentException("Khoảng thời gian báo cáo tối đa là 104 tuần (2 năm)");
        }

        List<YearWeek> targetWeeks = buildTargetWeeks(startWeek, endWeek);
        LocalDate startDate = startWeek.getStartDate();
        LocalDate endDate = endWeek.getEndDate();

        // 4. Lấy danh sách nhân sự trong phạm vi
        List<Employee> targetEmployees = loadEmployeesInScope(effectiveOrgUnitId, query != null ? query.employeeId() : null);

        if (targetEmployees.isEmpty()) {
            BillableRateSummary emptySummary = new BillableRateSummary(
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    null,
                    0, 0, targetWeeks.size()
            );

            recordSuccessAuditLog(currentUserId, orgUnitName, startWeek, endWeek, 0);

            return new BillableRateResult(
                    effectiveOrgUnitId,
                    orgUnitName,
                    startWeek.year(),
                    startWeek.weekNumber(),
                    endWeek.year(),
                    endWeek.weekNumber(),
                    startDate,
                    endDate,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    emptySummary,
                    false,
                    "Không tìm thấy dữ liệu nhân sự trong phạm vi báo cáo",
                    LocalDateTime.now()
            );
        }

        List<Long> employeeIds = targetEmployees.stream().map(Employee::getIdValue).toList();

        // Load OrgUnit mapping
        Map<Long, String> orgUnitNames = new HashMap<>();
        for (Employee emp : targetEmployees) {
            if (emp.getOrgUnitId() != null && !orgUnitNames.containsKey(emp.getOrgUnitId())) {
                loadOrgUnitPort.findById(new OrgUnitId(emp.getOrgUnitId()))
                        .ifPresent(u -> orgUnitNames.put(emp.getOrgUnitId(), u.getUnitName()));
            }
        }

        // 5. Nạp cấu hình ngày lễ và lịch làm việc
        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(startDate, endDate);
        Set<DayOfWeek> workingDays = resolveWorkingDays();

        // Map tuần -> số giờ nghỉ lễ
        Map<YearWeek, Integer> holidayHoursByWeek = new HashMap<>();
        for (YearWeek yw : targetWeeks) {
            int hHours = WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays);
            holidayHoursByWeek.put(yw, hHours);
        }

        // 6. Nạp giờ khả dụng đã lưu và đơn nghỉ phép đã duyệt
        List<WeeklyAvailability> customAvailabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, WeeklyAvailability> customAvailMap = new HashMap<>();
        for (WeeklyAvailability wa : customAvailabilities) {
            customAvailMap.put(wa.getEmployeeId() + "_" + wa.getYear() + "_" + wa.getWeekNumber(), wa);
        }

        Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(employeeIds, targetWeeks);

        // 7. Nạp giờ công thực tế đã duyệt (status = 'APPROVED')
        Map<Long, EmployeeHoursData> timesheetHoursMap = loadBillableRateTimesheetPort.loadApprovedHoursByEmployeesAndDateRange(
                employeeIds, startDate, endDate
        );

        // 8. Tính toán tỷ lệ giờ tính phí cho từng nhân sự
        List<BillableRateItem> employeeItems = new ArrayList<>();
        BigDecimal totalStandardHours = BigDecimal.ZERO;
        BigDecimal totalHolidayHours = BigDecimal.ZERO;
        BigDecimal totalApprovedLeaveHours = BigDecimal.ZERO;
        BigDecimal totalAvailableHours = BigDecimal.ZERO;
        BigDecimal totalBillableHours = BigDecimal.ZERO;
        BigDecimal totalNonBillableHours = BigDecimal.ZERO;
        BigDecimal totalActualHours = BigDecimal.ZERO;

        for (Employee emp : targetEmployees) {
            Long empId = emp.getIdValue();
            int defaultStandardHours = emp.getStandardHoursPerWeek() != null ? emp.getStandardHoursPerWeek() : 40;

            BigDecimal empStandardHours = BigDecimal.ZERO;
            BigDecimal empHolidayHours = BigDecimal.ZERO;
            BigDecimal empApprovedLeaveHours = BigDecimal.ZERO;
            BigDecimal empNetAvailableHours = BigDecimal.ZERO;

            Map<YearWeek, BigDecimal> empLeaves = leaveHoursMap.getOrDefault(empId, Collections.emptyMap());

            for (YearWeek yw : targetWeeks) {
                String key = empId + "_" + yw.year() + "_" + yw.weekNumber();
                WeeklyAvailability wa = customAvailMap.get(key);

                int stdH = wa != null ? wa.getStandardHours() : defaultStandardHours;
                int holH = holidayHoursByWeek.getOrDefault(yw, 0);
                BigDecimal leaveH = empLeaves.getOrDefault(yw, BigDecimal.ZERO);

                BigDecimal weekNet = WeeklyAvailabilityPolicy.calculateNetAvailableHours(stdH, holH, leaveH);

                empStandardHours = empStandardHours.add(BigDecimal.valueOf(stdH));
                empHolidayHours = empHolidayHours.add(BigDecimal.valueOf(holH));
                empApprovedLeaveHours = empApprovedLeaveHours.add(leaveH);
                empNetAvailableHours = empNetAvailableHours.add(weekNet);
            }

            EmployeeHoursData hoursData = timesheetHoursMap.getOrDefault(empId, new EmployeeHoursData(BigDecimal.ZERO, BigDecimal.ZERO));
            BigDecimal billableHours = hoursData.billableHours().setScale(1, RoundingMode.HALF_UP);
            BigDecimal nonBillableHours = hoursData.nonBillableHours().setScale(1, RoundingMode.HALF_UP);
            BigDecimal actualHours = hoursData.totalActualHours().setScale(1, RoundingMode.HALF_UP);

            BigDecimal billableRate = null;
            boolean hasAvailable = empNetAvailableHours.compareTo(BigDecimal.ZERO) > 0;
            if (hasAvailable) {
                billableRate = billableHours.multiply(BigDecimal.valueOf(100))
                        .divide(empNetAvailableHours, 1, RoundingMode.HALF_UP);
            }

            String status;
            if (!hasAvailable && empApprovedLeaveHours.compareTo(BigDecimal.ZERO) > 0) {
                status = "ON_LEAVE";
            } else if (billableRate == null || billableHours.compareTo(BigDecimal.ZERO) == 0) {
                status = "NO_BILLABLE_HOURS";
            } else if (billableRate.compareTo(BigDecimal.valueOf(85.0)) >= 0) {
                status = "HIGH_UTILIZATION";
            } else if (billableRate.compareTo(BigDecimal.valueOf(70.0)) >= 0) {
                status = "OPTIMAL";
            } else {
                status = "LOW_UTILIZATION";
            }

            String unitName = emp.getOrgUnitId() != null ? orgUnitNames.getOrDefault(emp.getOrgUnitId(), "-") : "-";

            employeeItems.add(new BillableRateItem(
                    empId,
                    emp.getEmployeeCode(),
                    emp.getFullName(),
                    emp.getOrgUnitId(),
                    unitName,
                    empStandardHours.setScale(1, RoundingMode.HALF_UP),
                    empHolidayHours.setScale(1, RoundingMode.HALF_UP),
                    empApprovedLeaveHours.setScale(1, RoundingMode.HALF_UP),
                    empNetAvailableHours.setScale(1, RoundingMode.HALF_UP),
                    billableHours,
                    nonBillableHours,
                    actualHours,
                    billableRate,
                    hasAvailable,
                    status
            ));

            totalStandardHours = totalStandardHours.add(empStandardHours);
            totalHolidayHours = totalHolidayHours.add(empHolidayHours);
            totalApprovedLeaveHours = totalApprovedLeaveHours.add(empApprovedLeaveHours);
            totalAvailableHours = totalAvailableHours.add(empNetAvailableHours);
            totalBillableHours = totalBillableHours.add(billableHours);
            totalNonBillableHours = totalNonBillableHours.add(nonBillableHours);
            totalActualHours = totalActualHours.add(actualHours);
        }

        // Sắp xếp danh sách nhân sự: theo Phòng ban rồi đến Tên nhân sự
        employeeItems.sort(Comparator
                .comparing(BillableRateItem::orgUnitName)
                .thenComparing(BillableRateItem::fullName));

        // 9. Tổng hợp theo Từng phòng ban (Department Breakdown)
        Map<Long, List<BillableRateItem>> itemsByDept = employeeItems.stream()
                .collect(Collectors.groupingBy(item -> item.orgUnitId() != null ? item.orgUnitId() : 0L));

        List<DepartmentBillableRateSummary> departmentSummaries = new ArrayList<>();
        for (Map.Entry<Long, List<BillableRateItem>> entry : itemsByDept.entrySet()) {
            Long deptId = entry.getKey();
            List<BillableRateItem> deptItems = entry.getValue();

            String dName = deptId != 0L ? orgUnitNames.getOrDefault(deptId, "Phòng #" + deptId) : "Chưa phân phòng";

            BigDecimal dStandard = BigDecimal.ZERO;
            BigDecimal dLeave = BigDecimal.ZERO;
            BigDecimal dAvailable = BigDecimal.ZERO;
            BigDecimal dBillable = BigDecimal.ZERO;
            BigDecimal dNonBillable = BigDecimal.ZERO;
            BigDecimal dActual = BigDecimal.ZERO;

            for (BillableRateItem it : deptItems) {
                dStandard = dStandard.add(it.standardHours());
                dLeave = dLeave.add(it.approvedLeaveHours());
                dAvailable = dAvailable.add(it.netAvailableHours());
                dBillable = dBillable.add(it.billableHours());
                dNonBillable = dNonBillable.add(it.nonBillableHours());
                dActual = dActual.add(it.totalActualHours());
            }

            BigDecimal dRate = null;
            if (dAvailable.compareTo(BigDecimal.ZERO) > 0) {
                dRate = dBillable.multiply(BigDecimal.valueOf(100))
                        .divide(dAvailable, 1, RoundingMode.HALF_UP);
            }

            departmentSummaries.add(new DepartmentBillableRateSummary(
                    deptId != 0L ? deptId : null,
                    dName,
                    dStandard.setScale(1, RoundingMode.HALF_UP),
                    dLeave.setScale(1, RoundingMode.HALF_UP),
                    dAvailable.setScale(1, RoundingMode.HALF_UP),
                    dBillable.setScale(1, RoundingMode.HALF_UP),
                    dNonBillable.setScale(1, RoundingMode.HALF_UP),
                    dActual.setScale(1, RoundingMode.HALF_UP),
                    dRate,
                    deptItems.size()
            ));
        }

        departmentSummaries.sort(Comparator.comparing(DepartmentBillableRateSummary::orgUnitName));

        // 10. Tổng hợp toàn công ty (Overall Summary)
        BigDecimal overallBillableRate = null;
        if (totalAvailableHours.compareTo(BigDecimal.ZERO) > 0) {
            overallBillableRate = totalBillableHours.multiply(BigDecimal.valueOf(100))
                    .divide(totalAvailableHours, 1, RoundingMode.HALF_UP);
        }

        BillableRateSummary summary = new BillableRateSummary(
                totalStandardHours.setScale(1, RoundingMode.HALF_UP),
                totalHolidayHours.setScale(1, RoundingMode.HALF_UP),
                totalApprovedLeaveHours.setScale(1, RoundingMode.HALF_UP),
                totalAvailableHours.setScale(1, RoundingMode.HALF_UP),
                totalBillableHours.setScale(1, RoundingMode.HALF_UP),
                totalNonBillableHours.setScale(1, RoundingMode.HALF_UP),
                totalActualHours.setScale(1, RoundingMode.HALF_UP),
                overallBillableRate,
                employeeItems.size(),
                departmentSummaries.size(),
                targetWeeks.size()
        );

        recordSuccessAuditLog(currentUserId, orgUnitName, startWeek, endWeek, employeeItems.size());

        return new BillableRateResult(
                effectiveOrgUnitId,
                orgUnitName,
                startWeek.year(),
                startWeek.weekNumber(),
                endWeek.year(),
                endWeek.weekNumber(),
                startDate,
                endDate,
                departmentSummaries,
                employeeItems,
                summary,
                true,
                "Lấy báo cáo tỷ lệ giờ tính phí thành công",
                LocalDateTime.now()
        );
    }

    @Override
    public BillableRateExport export(BillableRateQuery query) {
        BillableRateResult result = execute(query);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8)) {
            // Write UTF-8 BOM for Excel compatibility
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            writer.println("BÁO CÁO TỶ LỆ GIỜ TÍNH PHÍ (BILLABLE UTILIZATION REPORT)");
            writer.println("Đơn vị," + escapeCsv(result.orgUnitName()));
            writer.println("Kỳ phân tích,Từ tuần " + result.fromWeek() + "/" + result.fromYear() + " đến tuần " + result.toWeek() + "/" + result.toYear());
            writer.println("Thời gian," + result.startDate() + " đến " + result.endDate());
            writer.println("Thời điểm xuất," + result.generatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println();

            // Tổng quan toàn công ty
            writer.println("--- TỔNG HỢP TOÀN CÔNG TY ---");
            writer.println("Tổng nhân sự," + result.summary().totalEmployees());
            writer.println("Tổng giờ chuẩn," + result.summary().totalStandardHours());
            writer.println("Tổng giờ nghỉ phép," + result.summary().totalApprovedLeaveHours());
            writer.println("Tổng giờ khả dụng ròng," + result.summary().totalAvailableHours());
            writer.println("Tổng giờ tính phí (Billable)," + result.summary().totalBillableHours());
            writer.println("Tổng giờ không tính phí," + result.summary().totalNonBillableHours());
            writer.println("Tổng giờ thực tế đã duyệt," + result.summary().totalActualHours());
            writer.println("Tỷ lệ giờ tính phí chung," + (result.summary().overallBillableRate() != null ? result.summary().overallBillableRate() + "%" : "N/A"));
            writer.println();

            // Tổng hợp theo phòng ban
            writer.println("--- TỔNG HỢP THEO PHÒNG BAN ---");
            writer.println("Phòng ban,Số nhân sự,Giờ chuẩn,Nghỉ phép,Giờ khả dụng,Giờ tính phí,Giờ không tính phí,Tổng giờ thực tế,Tỷ lệ tính phí (%)");
            for (DepartmentBillableRateSummary d : result.departmentBreakdown()) {
                writer.println(String.format("%s,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%s",
                        escapeCsv(d.orgUnitName()),
                        d.employeeCount(),
                        d.totalStandardHours(),
                        d.totalApprovedLeaveHours(),
                        d.totalAvailableHours(),
                        d.totalBillableHours(),
                        d.totalNonBillableHours(),
                        d.totalActualHours(),
                        d.billableRate() != null ? d.billableRate() + "%" : "N/A"
                ));
            }
            writer.println();

            // Chi tiết từng nhân sự
            writer.println("--- CHI TIẾT THEO TỪNG NHÂN SỰ ---");
            writer.println("Mã NV,Họ và tên,Phòng ban,Giờ chuẩn,Nghỉ lễ,Nghỉ phép,Giờ khả dụng ròng,Giờ tính phí,Giờ không tính phí,Tổng giờ thực tế,Tỷ lệ tính phí (%),Trạng thái");
            for (BillableRateItem item : result.employeeBreakdown()) {
                writer.println(String.format("%s,%s,%s,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%s,%s",
                        escapeCsv(item.employeeCode()),
                        escapeCsv(item.fullName()),
                        escapeCsv(item.orgUnitName()),
                        item.standardHours(),
                        item.holidayHours(),
                        item.approvedLeaveHours(),
                        item.netAvailableHours(),
                        item.billableHours(),
                        item.nonBillableHours(),
                        item.totalActualHours(),
                        item.billableRate() != null ? item.billableRate() + "%" : "N/A",
                        escapeCsv(item.status())
                ));
            }

            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất tệp CSV báo cáo tỷ lệ giờ tính phí: " + e.getMessage(), e);
        }

        String filename = "bao-cao-ty-le-gio-tinh-phi-W" + result.fromWeek() + "_" + result.fromYear() + "-W" + result.toWeek() + "_" + result.toYear() + ".csv";
        return new BillableRateExport(filename, out.toByteArray());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<Long> resolveScopeBranchOrgUnitIds(Long effectiveOrgUnitId) {
        if (effectiveOrgUnitId != null) {
            Optional<OrgUnit> unitOpt = loadOrgUnitPort.findById(new OrgUnitId(effectiveOrgUnitId));
            if (unitOpt.isPresent()) {
                List<OrgUnit> subTree = loadOrgUnitPort.findSubTree(unitOpt.get().getTreePath());
                return subTree.stream().map(u -> u.getId().getValue()).toList();
            }
            return List.of(effectiveOrgUnitId);
        }
        return null;
    }

    private List<Employee> loadEmployeesInScope(Long effectiveOrgUnitId, Long filterEmployeeId) {
        if (filterEmployeeId != null) {
            Employee emp = loadEmployeePort.findById(new EmployeeId(filterEmployeeId))
                    .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + filterEmployeeId));
            if (effectiveOrgUnitId != null) {
                List<Long> branchIds = resolveScopeBranchOrgUnitIds(effectiveOrgUnitId);
                if (emp.getOrgUnitId() == null || (branchIds != null && !branchIds.contains(emp.getOrgUnitId()))) {
                    throw new EmployeeNotFoundException("Nhân viên nằm ngoài phạm vi quản lý: " + filterEmployeeId);
                }
            }
            return List.of(emp);
        }

        List<Long> branchIds = resolveScopeBranchOrgUnitIds(effectiveOrgUnitId);
        if (branchIds != null) {
            return loadEmployeePort.findActiveByOrgUnitIds(branchIds);
        }
        return loadEmployeePort.findAllActive();
    }

    private List<YearWeek> buildTargetWeeks(YearWeek start, YearWeek end) {
        List<YearWeek> list = new ArrayList<>();
        YearWeek current = start;
        LocalDate monday = current.getStartDate();
        LocalDate endMonday = end.getStartDate();

        while (!monday.isAfter(endMonday)) {
            int y = monday.get(IsoFields.WEEK_BASED_YEAR);
            int w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            list.add(YearWeek.of(y, w));
            monday = monday.plusWeeks(1);
        }
        return list;
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            try {
                return loadWorkingCalendarPort.loadCompanyCalendar().getWorkingDays();
            } catch (Exception ignored) {
            }
        }
        return Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );
    }

    private void recordDeniedAuditLog(Long userId, String reason) {
        if (saveAuditLogPort != null) {
            try {
                saveAuditLogPort.save(AuditLog.createChange(
                        userId,
                        "ACCESS_DENIED_BILLABLE_HOURS_REPORT",
                        "billable_rate_report",
                        null,
                        null,
                        "user_id=" + (userId != null ? userId : "ANONYMOUS") + ";reason=" + reason
                ));
            } catch (Exception ignored) {
            }
        }
    }

    private void recordSuccessAuditLog(Long userId, String orgUnitName, YearWeek start, YearWeek end, int employeeCount) {
        if (saveAuditLogPort != null) {
            try {
                saveAuditLogPort.save(AuditLog.createChange(
                        userId,
                        "BILLABLE_HOURS_REPORT_VIEWED",
                        "billable_rate_report",
                        null,
                        null,
                        "Xem báo cáo tỷ lệ giờ tính phí từ T" + start.weekNumber() + "/" + start.year() +
                        " đến T" + end.weekNumber() + "/" + end.year() + " cho đơn vị " + orgUnitName + " (Tổng " + employeeCount + " nhân sự)"
                ));
            } catch (Exception ignored) {
            }
        }
    }
}
