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

        int durationWeeks = query.durationWeeks() != null ? query.durationWeeks() : 8;

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

        // Nạp danh sách nhân sự active theo Data Scope
        List<Employee> employees = loadEmployeesInScope(effectiveOrgUnitId);
        if (employees.isEmpty()) {
            return new CompanyWeeklyCapacityMatrixResult(
                    effectiveOrgUnitId,
                    orgUnitName,
                    fromYear,
                    fromWeek,
                    durationWeeks,
                    weekHeaders,
                    List.of(),
                    new CapacityMatrixSummaryResult(0, durationWeeks, 0, 0, 0, BigDecimal.ZERO)
            );
        }

        List<Long> employeeIds = employees.stream().map(Employee::getIdValue).toList();

        // 1. Batch load toàn bộ phân bổ của danh sách nhân sự trong các tuần (1 query)
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

        // 3. Batch load ngày lễ cho khoảng thời gian tuần (1 query)
        LocalDate minStart = targetWeeks.get(0).getStartDate();
        LocalDate maxEnd = targetWeeks.get(targetWeeks.size() - 1).getEndDate();
        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(minStart, maxEnd);
        Set<DayOfWeek> workingDays = resolveWorkingDays();
        Map<YearWeek, Integer> holidayHoursByWeek = targetWeeks.stream()
                .collect(Collectors.toMap(
                        yw -> yw,
                        yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays)
                ));

        // 4. Batch load đơn nghỉ phép đã duyệt cho danh sách nhân sự (1 query)
        Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(employeeIds, targetWeeks);

        // 5. Nạp tên phòng ban cho từng nhân sự
        List<Long> orgUnitIds = employees.stream().map(Employee::getOrgUnitId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> orgUnitNameMap = orgUnitIds.isEmpty() ? Map.of() :
                loadOrgUnitPort.findAllByIdIn(orgUnitIds).stream()
                        .collect(Collectors.toMap(u -> u.getId().getValue(), OrgUnit::getUnitName, (e1, e2) -> e1));

        // Xây dựng các hàng (Rows) nhân sự
        List<EmployeeCapacityRowResult> rows = new ArrayList<>();
        int totalOverloadedCells = 0;
        int totalUnderutilizedCells = 0;
        BigDecimal companyTotalAllocated = BigDecimal.ZERO;
        BigDecimal companyTotalAvailable = BigDecimal.ZERO;

        for (Employee emp : employees) {
            BigDecimal empTotalAllocated = BigDecimal.ZERO;
            BigDecimal empTotalAvailable = BigDecimal.ZERO;
            int overloadedWeeksCount = 0;
            List<CapacityMatrixCellResult> cells = new ArrayList<>();

            for (YearWeek yw : targetWeeks) {
                String key = makeKey(emp.getIdValue(), yw.year(), yw.weekNumber());

                // Tính Available Hours
                BigDecimal availableHours;
                WeeklyAvailability savedAvail = availabilityMap.get(key);
                if (savedAvail != null) {
                    availableHours = savedAvail.getNetAvailableHours();
                } else {
                    int standardHours = emp.getStandardHoursPerWeek() != null ? emp.getStandardHoursPerWeek() : 40;
                    int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);
                    BigDecimal leaveHours = leaveHoursMap.getOrDefault(emp.getIdValue(), Map.of()).getOrDefault(yw, BigDecimal.ZERO);
                    availableHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, leaveHours);

                    // Xử lý hết hạn hợp đồng lao động
                    if (emp.getContractEndDate() != null && emp.getContractEndDate().isBefore(yw.getStartDate())) {
                        availableHours = BigDecimal.ZERO;
                    } else if (emp.getContractEndDate() != null && !emp.getContractEndDate().isAfter(yw.getEndDate())) {
                        int remainingDays = WeeklyAvailabilityPolicy.countWorkingDaysBetween(yw.getStartDate(), emp.getContractEndDate());
                        if (remainingDays == 0) {
                            availableHours = BigDecimal.ZERO;
                        } else if (remainingDays < 5) {
                            availableHours = availableHours.multiply(BigDecimal.valueOf(remainingDays))
                                    .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
                        }
                    }
                }

                // Tính Allocated Hours
                BigDecimal allocatedHours = allocationMap.getOrDefault(key, BigDecimal.ZERO);

                // Áp dụng QTN-12 qua Domain Policy
                boolean isOverloaded = WeeklyCapacityMatrixPolicy.isOverloaded(allocatedHours, availableHours);
                BigDecimal excessHours = WeeklyCapacityMatrixPolicy.calculateExcessHours(allocatedHours, availableHours);
                BigDecimal utilizationPercentage = WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(allocatedHours, availableHours);
                CapacityStatus status = WeeklyCapacityMatrixPolicy.determineStatus(allocatedHours, availableHours);

                BigDecimal remainingHours = availableHours.subtract(allocatedHours);

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

            BigDecimal empAvgUtilization = WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(empTotalAllocated, empTotalAvailable);
            if (empAvgUtilization == null && empTotalAllocated.compareTo(BigDecimal.ZERO) > 0) {
                empAvgUtilization = BigDecimal.valueOf(100.0);
            } else if (empAvgUtilization == null) {
                empAvgUtilization = BigDecimal.ZERO;
            }

            companyTotalAllocated = companyTotalAllocated.add(empTotalAllocated);
            companyTotalAvailable = companyTotalAvailable.add(empTotalAvailable);

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
        BigDecimal companyAvgUtilization = WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(companyTotalAllocated, companyTotalAvailable);
        if (companyAvgUtilization == null && companyTotalAllocated.compareTo(BigDecimal.ZERO) > 0) {
            companyAvgUtilization = BigDecimal.valueOf(100.0);
        } else if (companyAvgUtilization == null) {
            companyAvgUtilization = BigDecimal.ZERO;
        }

        CapacityMatrixSummaryResult summary = new CapacityMatrixSummaryResult(
                rows.size(),
                targetWeeks.size(),
                overloadedEmployeesCount,
                totalOverloadedCells,
                totalUnderutilizedCells,
                companyAvgUtilization
        );

        return new CompanyWeeklyCapacityMatrixResult(
                effectiveOrgUnitId,
                orgUnitName,
                fromYear,
                fromWeek,
                durationWeeks,
                weekHeaders,
                rows,
                summary
        );
    }

    private List<Employee> loadEmployeesInScope(Long effectiveOrgUnitId) {
        if (effectiveOrgUnitId != null) {
            // Lấy toàn bộ cây đơn vị con trực thuộc (nếu có) để bao phủ đầy đủ branch
            Optional<OrgUnit> unitOpt = loadOrgUnitPort.findById(new OrgUnitId(effectiveOrgUnitId));
            if (unitOpt.isPresent()) {
                List<OrgUnit> subTree = loadOrgUnitPort.findSubTree(unitOpt.get().getTreePath());
                List<Long> branchIds = subTree.stream().map(u -> u.getId().getValue()).toList();
                return loadEmployeePort.findActiveByOrgUnitIds(branchIds);
            }
            return loadEmployeePort.findActiveByOrgUnitId(effectiveOrgUnitId);
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
}