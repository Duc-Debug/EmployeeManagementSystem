package com.hrm.employeemanagement.application.service.allocation;

import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult.*;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetCompanyWeeklyCapacityUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

public class GetCompanyWeeklyCapacityService implements GetCompanyWeeklyCapacityUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;

    public GetCompanyWeeklyCapacityService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
    }

    @Override
    public CompanyWeeklyCapacityMatrixResult getWeeklyCapacityMatrix(CompanyWeeklyCapacityQuery query) {
        // [TC-03 & Security]: Bắt buộc quyền RESOURCE_ALLOCATION_READ
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // Xác thực và áp dụng Data Scope chặt chẽ
        Long effectiveOrgUnitId;
        switch (currentUser.getDataScope()) {
            case COMPANY -> {
                effectiveOrgUnitId = query.orgUnitId();
            }
            case ORGANIZATION_BRANCH -> {
                if (currentUser.getScopeOrgUnitId() == null) {
                    throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
                }
                if (query.orgUnitId() != null) {
                    // [TC-03]: Kiểm tra xem orgUnitId được yêu cầu có thuộc branch của người dùng không
                    boolean inScope = loadOrgUnitPort.existsInOrgUnitBranch(query.orgUnitId(), currentUser.getScopeOrgUnitId());
                    if (!inScope) {
                        throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
                    }
                    effectiveOrgUnitId = query.orgUnitId();
                } else {
                    // Mặc định giới hạn trong branch của người dùng
                    effectiveOrgUnitId = currentUser.getScopeOrgUnitId();
                }
            }
            case SELF -> {
                // Người dùng chỉ có quyền SELF không được xem bảng năng lực tổng thể công ty/bộ phận
                throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
            }
            default -> throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
        }

        String orgUnitName = "Toàn công ty";
        if (effectiveOrgUnitId != null) {
            OrgUnit unit = loadOrgUnitPort.findById(new OrgUnitId(effectiveOrgUnitId))
                    .orElseThrow(() -> new OrgUnitNotFoundException("Không tìm thấy bộ phận: " + effectiveOrgUnitId));
            orgUnitName = unit.getUnitName();
        }

        // Xác định khoảng tuần mục tiêu
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

        int rawDuration = query.durationWeeks() != null ? query.durationWeeks() : 8;
        int durationWeeks = Math.min(Math.max(1, rawDuration), 16);

        List<YearWeek> targetWeeks = buildTargetWeeks(fromYear, fromWeek, durationWeeks);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");
        List<HeaderWeekInfo> weekHeaders = targetWeeks.stream()
                .map(yw -> new HeaderWeekInfo(
                        yw.year(),
                        yw.weekNumber(),
                        yw.getStartDate(),
                        yw.getEndDate(),
                        "T" + yw.weekNumber() + " (" + yw.getStartDate().format(dtf) + " - " + yw.getEndDate().format(dtf) + ")"
                ))
                .toList();

        // Batch load ngày lễ cho khoảng thời gian tuần (1 query dùng chung cho các nhân sự)
        LocalDate minStart = targetWeeks.get(0).getStartDate();
        LocalDate maxEnd = targetWeeks.get(targetWeeks.size() - 1).getEndDate();
        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(minStart, maxEnd);
        Set<DayOfWeek> workingDays = resolveWorkingDays();
        Map<YearWeek, Integer> holidayHoursByWeek = targetWeeks.stream()
                .collect(Collectors.toMap(
                        yw -> yw,
                        yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays)
                ));

        // [🔴 HIGH REVIEW FIX]: Phân trang Server-side thực thụ ở tầng Database
        // Khi không có bộ lọc trạng thái (status == null), truy vấn phân trang trực tiếp từ DB
        // CHỈ nạp đúng pageSize nhân sự và CHỈ batch-load DB cho các nhân sự trên trang đó
        if (query.status() == null) {
            List<Long> branchIds = resolveScopeBranchOrgUnitIds(effectiveOrgUnitId);
            String search = (query.search() != null && !query.search().isBlank()) ? query.search().trim() : null;

            int pageSize = query.size();
            int page = query.page();
            int offset = page * pageSize;

            long totalEmployees = loadEmployeePort.countActive(branchIds, search);
            int totalPages = totalEmployees == 0 ? 0 : (int) Math.ceil((double) totalEmployees / pageSize);

            if (totalEmployees == 0) {
                return new CompanyWeeklyCapacityMatrixResult(
                        effectiveOrgUnitId,
                        orgUnitName,
                        fromYear,
                        fromWeek,
                        durationWeeks,
                        weekHeaders,
                        List.of(),
                        new CapacityMatrixSummaryResult(0, durationWeeks, 0, 0, 0, BigDecimal.ZERO),
                        page,
                        pageSize,
                        0,
                        0
                );
            }

            List<Employee> pageEmployees = loadEmployeePort.findActivePaged(branchIds, search, pageSize, offset);

            ComputationResult computation = computeMatrixForEmployees(pageEmployees, targetWeeks, workingDays, holidayHoursByWeek);

            return new CompanyWeeklyCapacityMatrixResult(
                    effectiveOrgUnitId,
                    orgUnitName,
                    fromYear,
                    fromWeek,
                    durationWeeks,
                    weekHeaders,
                    computation.rows(),
                    computation.summary(),
                    page,
                    pageSize,
                    (int) totalEmployees,
                    totalPages
            );
        }

        // [🟠 MEDIUM REVIEW FIX]: Tối ưu hóa pipeline khi có bộ lọc trạng thái
        List<Employee> employees = loadEmployeesInScope(effectiveOrgUnitId);

        // Áp dụng bộ lọc tìm kiếm theo từ khóa nếu có (search)
        if (query.search() != null && !query.search().isBlank()) {
            String searchPattern = query.search().trim().toLowerCase();
            employees = employees.stream()
                    .filter(emp -> (emp.getFullName() != null && emp.getFullName().toLowerCase().contains(searchPattern))
                            || (emp.getEmployeeCode() != null && emp.getEmployeeCode().toLowerCase().contains(searchPattern))
                            || (emp.getProfessionalRole() != null && emp.getProfessionalRole().toLowerCase().contains(searchPattern)))
                    .toList();
        }

        // Sắp xếp ổn định và tất định (Deterministic Sort) theo họ tên, sau đó mã nhân viên
        employees = employees.stream()
                .sorted(Comparator.comparing(Employee::getFullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Employee::getEmployeeCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        if (employees.isEmpty()) {
            return new CompanyWeeklyCapacityMatrixResult(
                    effectiveOrgUnitId,
                    orgUnitName,
                    fromYear,
                    fromWeek,
                    durationWeeks,
                    weekHeaders,
                    List.of(),
                    new CapacityMatrixSummaryResult(0, durationWeeks, 0, 0, 0, BigDecimal.ZERO),
                    query.page(),
                    query.size(),
                    0,
                    0
            );
        }

        List<Employee> candidatesToEvaluate = employees;
        if (query.status() == CapacityStatus.OVERLOADED) {
            // Pre-filter: Theo QTN-12, nhân sự chỉ có thể quá tải nếu có tổng phân bổ > 0 trong tuần.
            // Nhân sự không có phân bổ nào trong targetWeeks (allocatedHours == 0) không bao giờ bị quá tải.
            List<Long> allEmpIds = employees.stream().map(Employee::getIdValue).toList();
            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(allEmpIds, targetWeeks);
            Set<Long> allocatedEmpIds = allocations.stream()
                    .filter(a -> a.getAllocatedHours() != null && a.getAllocatedHours().compareTo(BigDecimal.ZERO) > 0)
                    .map(WeeklyProjectAllocation::getEmployeeId)
                    .collect(Collectors.toSet());

            candidatesToEvaluate = employees.stream()
                    .filter(e -> allocatedEmpIds.contains(e.getIdValue()))
                    .toList();
        }

        ComputationResult computation = computeMatrixForEmployees(candidatesToEvaluate, targetWeeks, workingDays, holidayHoursByWeek);
        List<EmployeeCapacityRowResult> matchingRows = computation.rows().stream()
                .filter(r -> matchesStatus(r, query.status()))
                .toList();

        int totalMatching = matchingRows.size();
        int pageSize = query.size();
        int page = query.page();
        int totalPages = totalMatching == 0 ? 0 : (int) Math.ceil((double) totalMatching / pageSize);

        int fromIndex = Math.min(page * pageSize, totalMatching);
        int toIndex = Math.min(fromIndex + pageSize, totalMatching);
        List<EmployeeCapacityRowResult> pageRows = matchingRows.subList(fromIndex, toIndex);

        CapacityMatrixSummaryResult summary = new CapacityMatrixSummaryResult(
                pageRows.size(),
                targetWeeks.size(),
                query.status() == CapacityStatus.OVERLOADED ? totalMatching : computation.summary().overloadedEmployeesCount(),
                computation.summary().overloadedCellsCount(),
                computation.summary().underutilizedCellsCount(),
                computation.summary().averageUtilization()
        );

        return new CompanyWeeklyCapacityMatrixResult(
                effectiveOrgUnitId,
                orgUnitName,
                fromYear,
                fromWeek,
                durationWeeks,
                weekHeaders,
                pageRows,
                summary,
                page,
                pageSize,
                totalMatching,
                totalPages
        );
    }

    /**
     * Tính toán ma trận năng lực tuần cho danh sách nhân sự mục tiêu.
     * Chỉ batch-load dữ liệu phân bổ, khả dụng và nghỉ phép cho đúng danh sách này.
     */
    private ComputationResult computeMatrixForEmployees(
            List<Employee> targetEmployees,
            List<YearWeek> targetWeeks,
            Set<DayOfWeek> workingDays,
            Map<YearWeek, Integer> holidayHoursByWeek
    ) {
        if (targetEmployees == null || targetEmployees.isEmpty()) {
            return new ComputationResult(List.of(), new CapacityMatrixSummaryResult(0, targetWeeks.size(), 0, 0, 0, BigDecimal.ZERO));
        }

        List<Long> employeeIds = targetEmployees.stream().map(Employee::getIdValue).toList();

        // 1. Batch load toàn bộ phân bổ của danh sách nhân sự mục tiêu (1 query)
        List<WeeklyProjectAllocation> allAllocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, BigDecimal> allocationMap = allAllocations.stream()
                .collect(Collectors.groupingBy(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
                ));

        // 2. Batch load tính khả dụng đã lưu (1 query)
        List<WeeklyAvailability> allAvailabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, WeeklyAvailability> availabilityMap = allAvailabilities.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        a -> a,
                        (existing, replacing) -> existing
                ));

        // 3. Batch load đơn nghỉ phép đã duyệt cho danh sách nhân sự mục tiêu (1 query)
        Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(employeeIds, targetWeeks);

        // 4. Nạp tên phòng ban cho từng nhân sự
        List<Long> orgUnitIds = targetEmployees.stream().map(Employee::getOrgUnitId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> orgUnitNameMap = orgUnitIds.isEmpty() ? Map.of() :
                loadOrgUnitPort.findAllByIdIn(orgUnitIds).stream()
                        .collect(Collectors.toMap(u -> u.getId().getValue(), OrgUnit::getUnitName, (e1, e2) -> e1));

        List<EmployeeCapacityRowResult> rows = new ArrayList<>();
        int totalOverloadedCells = 0;
        int totalUnderutilizedCells = 0;
        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;

        for (Employee emp : targetEmployees) {
            BigDecimal empTotalAllocated = BigDecimal.ZERO;
            BigDecimal empTotalAvailable = BigDecimal.ZERO;
            int overloadedWeeksCount = 0;
            List<CapacityMatrixCellResult> cells = new ArrayList<>();

            for (YearWeek yw : targetWeeks) {
                String key = makeKey(emp.getIdValue(), yw.year(), yw.weekNumber());

                // 1. Xác định baseAvailableHours: Luôn luôn áp dụng số giờ chuẩn (ưu tiên WeeklyAvailability nếu khai báo riêng),
                // trừ ngày lễ và trừ giờ nghỉ phép đã duyệt từ nguồn dữ liệu thực tế (leave_requests)
                WeeklyAvailability savedAvail = availabilityMap.get(key);
                int standardHours = savedAvail != null
                        ? savedAvail.getStandardHours()
                        : (emp.getStandardHoursPerWeek() != null ? emp.getStandardHoursPerWeek() : 40);
                int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);
                BigDecimal leaveHours = leaveHoursMap.getOrDefault(emp.getIdValue(), Map.of()).getOrDefault(yw, BigDecimal.ZERO);
                BigDecimal baseAvailableHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, leaveHours);

                // 2. Luôn luôn áp dụng điều chỉnh hợp đồng lao động
                int weekWorkingDaysCount = workingDays.isEmpty() ? 5 : workingDays.size();
                BigDecimal availableHours = WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(
                        baseAvailableHours,
                        emp.getContractEndDate(),
                        yw.getStartDate(),
                        yw.getEndDate(),
                        weekWorkingDaysCount
                );

                // 3. Tính Allocated Hours
                BigDecimal allocatedHours = allocationMap.getOrDefault(key, BigDecimal.ZERO);

                // 4. Áp dụng QTN-12
                boolean isOverloaded = WeeklyCapacityMatrixPolicy.isOverloaded(allocatedHours, availableHours);
                BigDecimal excessHours = WeeklyCapacityMatrixPolicy.calculateExcessHours(allocatedHours, availableHours);
                BigDecimal remainingHours = WeeklyCapacityMatrixPolicy.calculateRemainingHours(availableHours, allocatedHours);
                BigDecimal utilizationPercentage = WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(allocatedHours, availableHours);
                CapacityStatus status = WeeklyCapacityMatrixPolicy.determineStatus(allocatedHours, availableHours);

                if (isOverloaded) {
                    overloadedWeeksCount++;
                    totalOverloadedCells++;
                } else if (status == CapacityStatus.UNDERUTILIZED) {
                    totalUnderutilizedCells++;
                }

                empTotalAllocated = empTotalAllocated.add(allocatedHours);
                empTotalAvailable = empTotalAvailable.add(availableHours);

                cells.add(new CapacityMatrixCellResult(
                        yw.year(),
                        yw.weekNumber(),
                        allocatedHours,
                        availableHours,
                        remainingHours,
                        utilizationPercentage,
                        isOverloaded,
                        excessHours,
                        status
                ));
            }

            BigDecimal empAvgUtilization = WeeklyCapacityMatrixPolicy.calculateAverageUtilization(empTotalAllocated, empTotalAvailable);

            totalAllocated = totalAllocated.add(empTotalAllocated);
            totalAvailable = totalAvailable.add(empTotalAvailable);

            String empDeptName = emp.getOrgUnitId() != null ? orgUnitNameMap.getOrDefault(emp.getOrgUnitId(), "Chưa gán") : "Chưa gán";
            rows.add(new EmployeeCapacityRowResult(
                    emp.getIdValue(),
                    emp.getEmployeeCode(),
                    emp.getFullName(),
                    emp.getOrgUnitId(),
                    empDeptName,
                    emp.getProfessionalRole() != null ? emp.getProfessionalRole() : "Nhân viên",
                    cells,
                    empTotalAllocated,
                    empTotalAvailable,
                    empAvgUtilization,
                    overloadedWeeksCount
            ));
        }

        int overloadedEmployeesCount = (int) rows.stream().filter(r -> r.overloadedWeeksCount() > 0).count();
        BigDecimal avgUtilization = WeeklyCapacityMatrixPolicy.calculateAverageUtilization(totalAllocated, totalAvailable);

        CapacityMatrixSummaryResult summary = new CapacityMatrixSummaryResult(
                rows.size(),
                targetWeeks.size(),
                overloadedEmployeesCount,
                totalOverloadedCells,
                totalUnderutilizedCells,
                avgUtilization
        );

        return new ComputationResult(rows, summary);
    }

    private record ComputationResult(List<EmployeeCapacityRowResult> rows, CapacityMatrixSummaryResult summary) {}

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

    private List<Employee> loadEmployeesInScope(Long effectiveOrgUnitId) {
        List<Long> branchIds = resolveScopeBranchOrgUnitIds(effectiveOrgUnitId);
        if (branchIds != null) {
            return loadEmployeePort.findActiveByOrgUnitIds(branchIds);
        }
        return loadEmployeePort.findAllActive();
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

    private String makeKey(Long employeeId, int year, int weekNumber) {
        return employeeId + "_" + year + "_" + weekNumber;
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
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

    private boolean matchesStatus(EmployeeCapacityRowResult row, CapacityStatus status) {
        if (status == null) {
            return true;
        }
        return switch (status) {
            case OVERLOADED -> row.overloadedWeeksCount() > 0;
            case UNDERUTILIZED -> row.cells().stream().anyMatch(c -> c.status() == CapacityStatus.UNDERUTILIZED);
            case OPTIMAL -> row.cells().stream().anyMatch(c -> c.status() == CapacityStatus.OPTIMAL);
        };
    }
}