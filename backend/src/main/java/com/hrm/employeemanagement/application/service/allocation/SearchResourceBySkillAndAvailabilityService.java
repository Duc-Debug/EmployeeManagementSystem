package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.EmployeeSkillCandidate;
import com.hrm.employeemanagement.application.dto.allocation.ResourceSearchResult;
import com.hrm.employeemanagement.application.dto.allocation.SearchResourceQuery;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyAvailableHoursResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.SearchResourceBySkillAndAvailabilityUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SearchResourcePort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class SearchResourceBySkillAndAvailabilityService implements SearchResourceBySkillAndAvailabilityUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final SearchResourcePort searchResourcePort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;

    public SearchResourceBySkillAndAvailabilityService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            SearchResourcePort searchResourcePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.searchResourcePort = Objects.requireNonNull(searchResourcePort, "SearchResourcePort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    @Override
    public List<ResourceSearchResult> search(SearchResourceQuery query) {
        // [TC-03] Kiểm tra quyền hạn
        Long currentUserId;
        try {
            currentUserId = authorizationService.require(PermissionCode.RESOURCE_SEARCH);
        } catch (PermissionDeniedException ex) {
            // [TC-03] Ghi nhật ký từ chối truy cập
            saveAuditLogPort.save(AuditLog.create(
                    null,
                    "ACCESS_DENIED",
                    "RESOURCE_SEARCH",
                    query.skillId()
            ));
            throw ex;
        }

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + currentUserId));

        List<YearWeek> targetWeeks = buildYearWeeksRange(query);

        // 1. Tải danh sách nhân sự có kỹ năng đã được duyệt và đạt mức thành thạo tối thiểu
        List<EmployeeSkillCandidate> candidates = searchResourcePort.findActiveEmployeesBySkill(
                query.skillId(),
                query.minProficiencyLevel()
        );

        // [TC-02] Nếu không có ai thỏa mãn, trả về danh sách rỗng
        if (candidates.isEmpty()) {
            recordSuccessAuditLog(currentUser, query, 0);
            return List.of();
        }

        Map<Long, String> orgUnitNames = loadOrgUnitPort.findAll().stream()
                .collect(Collectors.toMap(
                        u -> u.getId().getValue(),
                        OrgUnit::getUnitName,
                        (existing, replacing) -> existing
                ));

        List<ResourceSearchResult> results = new ArrayList<>();

        for (EmployeeSkillCandidate candidate : candidates) {
            Employee emp = candidate.employee();

            if (emp.getStatus() != EmployeeStatus.ACTIVE) {
                continue;
            }

            if (query.orgUnitId() != null && !query.orgUnitId().equals(emp.getOrgUnitId())) {
                continue;
            }

            if (!isOrgUnitInDataScope(currentUser, emp.getOrgUnitId())) {
                continue;
            }

            // 2. Tính toán số giờ rảnh từng tuần trong khoảng lọc
            BigDecimal totalRemainingAccumulated = BigDecimal.ZERO;
            List<WeeklyAvailableHoursResult> weeklyResults = new ArrayList<>();

            for (YearWeek yw : targetWeeks) {
                if (emp.getContractEndDate() != null && emp.getContractEndDate().isBefore(yw.getStartDate())) {
                    weeklyResults.add(new WeeklyAvailableHoursResult(
                            yw.year(), yw.weekNumber(), 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                    ));
                    continue;
                }

                int standardHours = emp.getStandardHoursPerWeek() != null ? emp.getStandardHoursPerWeek() : 40;
                Optional<WeeklyAvailability> availOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(emp.getIdValue(), yw);
                BigDecimal netAvailable = availOpt.map(WeeklyAvailability::getNetAvailableHours)
                        .orElse(BigDecimal.valueOf(standardHours));

                List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployee(emp.getIdValue(), yw);
                BigDecimal totalAllocated = allocations.stream()
                        .map(WeeklyProjectAllocation::getAllocatedHours)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Số giờ rảnh còn lại = Giờ khả dụng thực tế - Tổng giờ đã gán vào các dự án
                BigDecimal remaining = netAvailable.subtract(totalAllocated);
                if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                    remaining = BigDecimal.ZERO;
                }

                totalRemainingAccumulated = totalRemainingAccumulated.add(remaining);

                weeklyResults.add(new WeeklyAvailableHoursResult(
                        yw.year(),
                        yw.weekNumber(),
                        standardHours,
                        netAvailable,
                        totalAllocated,
                        remaining
                ));
            }

            String orgUnitName = emp.getOrgUnitId() != null ? orgUnitNames.getOrDefault(emp.getOrgUnitId(), "Chưa gán") : "Chưa gán";

            results.add(new ResourceSearchResult(
                    emp.getIdValue(),
                    emp.getEmployeeCode(),
                    emp.getFullName(),
                    emp.getOrgUnitId(),
                    orgUnitName,
                    emp.getProfessionalRole() != null ? emp.getProfessionalRole() : "Nhân viên",
                    candidate.skillId(),
                    candidate.skillName(),
                    candidate.proficiencyLevel(),
                    candidate.yearsOfExperience(),
                    weeklyResults,
                    totalRemainingAccumulated
            ));
        }

        // [TC-01] Sắp xếp theo tổng số giờ còn rảnh giảm dần
        results.sort(Comparator.comparing(ResourceSearchResult::totalRemainingHours).reversed());

        // [TC-04] Ghi lại lịch sử tìm kiếm thành công vào Audit Log
        recordSuccessAuditLog(currentUser, query, results.size());

        return results;
    }

    private boolean isOrgUnitInDataScope(User currentUser, Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        return switch (currentUser.getDataScope()) {
            case COMPANY ->
                true;
            case SELF ->
                false;
            case ORGANIZATION_BRANCH ->
                currentUser.getScopeOrgUnitId() != null
                && loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, currentUser.getScopeOrgUnitId());
        };
    }

    private List<YearWeek> buildYearWeeksRange(SearchResourceQuery query) {
        List<YearWeek> list = new ArrayList<>();
        LocalDate currentMonday = YearWeek.of(query.fromYear(), query.fromWeek()).getStartDate();
        LocalDate endMonday = YearWeek.of(query.toYear(), query.toWeek()).getStartDate();

        while (!currentMonday.isAfter(endMonday)) {
            int year = currentMonday.get(WeekFields.ISO.weekBasedYear());
            int week = currentMonday.get(WeekFields.ISO.weekOfWeekBasedYear());
            list.add(YearWeek.of(year, week));
            currentMonday = currentMonday.plusWeeks(1);
        }
        return list;
    }

    private void recordSuccessAuditLog(User user, SearchResourceQuery query, int resultCount) {
        String detail = String.format("Found %d resources", resultCount);
        saveAuditLogPort.save(AuditLog.createChange(
                user.getId().value(), // Dùng .value() thay vì .getValue()
                "SEARCH",
                "RESOURCE_MANAGEMENT",
                query.skillId(),
                null,
                detail
        ));
    }
}
