package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.ResourceCandidate;
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
        // [TC-03] Kiểm tra quyền hạn chức năng và phạm vi phòng ban
        Long currentUserId;
        User currentUser;
        try {
            currentUserId = authorizationService.require(PermissionCode.RESOURCE_SEARCH);
            currentUser = loadUserPort.findById(new UserId(currentUserId))
                    .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + currentUserId));

            if (query.orgUnitId() != null) {
                validateOrgUnitScopeAccess(currentUser, query.orgUnitId());
            }
        } catch (PermissionDeniedException ex) {
            // [TC-03] Ghi nhật ký từ chối truy cập (bao gồm cả khi thiếu quyền hoặc truy cập ngoài scope)
            saveAuditLogPort.save(AuditLog.create(
                    null,
                    "ACCESS_DENIED",
                    "RESOURCE_SEARCH",
                    query.skillId()
            ));
            throw ex;
        }

        List<YearWeek> targetWeeks = buildYearWeeksRange(query);

        // 1. Tải danh sách ứng viên đạt yêu cầu kỹ năng từ port
        List<ResourceCandidate> candidates = searchResourcePort.findActiveEmployeesBySkill(
                query.skillId(),
                query.minProficiencyLevel()
        );

        // [TC-02] Nếu không có ai thỏa mãn, trả về danh sách rỗng
        if (candidates.isEmpty()) {
            recordSuccessAuditLog(currentUser, query, 0);
            return List.of();
        }

        // Lọc ứng viên theo DataScope (dùng cache để tránh gọi N queries branch-check) và orgUnitId (nếu có)
        Map<Long, Boolean> orgUnitScopeCache = new java.util.HashMap<>();
        List<ResourceCandidate> filteredCandidates = candidates.stream()
                .filter(candidate -> isCandidateInDataScope(currentUser, candidate, orgUnitScopeCache))
                .filter(candidate -> query.orgUnitId() == null || query.orgUnitId().equals(candidate.orgUnitId()))
                .toList();

        if (filteredCandidates.isEmpty()) {
            recordSuccessAuditLog(currentUser, query, 0);
            return List.of();
        }

        List<Long> employeeIds = filteredCandidates.stream()
                .map(ResourceCandidate::employeeId)
                .distinct()
                .toList();

        // 2. Batch load tính khả dụng (WeeklyAvailability) và phân bổ (WeeklyProjectAllocation)
        List<WeeklyAvailability> allAvailabilities = loadWeeklyAvailabilityPort
                .loadAvailabilityForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<EmployeeWeekKey, WeeklyAvailability> availabilityMap = allAvailabilities.stream()
                .collect(Collectors.toMap(
                        a -> new EmployeeWeekKey(a.getEmployeeId(), a.getYearWeek()),
                        Function.identity(),
                        (existing, replacing) -> existing
                ));

        List<WeeklyProjectAllocation> allAllocations = loadAllocationPort
                .loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<EmployeeWeekKey, BigDecimal> allocationMap = allAllocations.stream()
                .collect(Collectors.groupingBy(
                        a -> new EmployeeWeekKey(a.getEmployeeId(), a.getYearWeek()),
                        Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
                ));

        // 3. Chỉ tải tên các phòng ban thực sự có trong danh sách candidates thay vì load toàn bộ
        List<Long> orgUnitIds = filteredCandidates.stream()
                .map(ResourceCandidate::orgUnitId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> orgUnitNames = orgUnitIds.isEmpty() ? Map.of() : loadOrgUnitPort.findAllByIdIn(orgUnitIds).stream()
                .collect(Collectors.toMap(
                        u -> u.getId().getValue(),
                        OrgUnit::getUnitName,
                        (existing, replacing) -> existing
                ));

        List<ResourceSearchResult> results = new ArrayList<>();

        for (ResourceCandidate candidate : filteredCandidates) {
            BigDecimal totalRemainingAccumulated = BigDecimal.ZERO;
            List<WeeklyAvailableHoursResult> weeklyResults = new ArrayList<>();

            for (YearWeek yw : targetWeeks) {
                // Kiểm tra hợp đồng hết hạn trước tuần mục tiêu
                if (candidate.contractEndDate() != null && candidate.contractEndDate().isBefore(yw.getStartDate())) {
                    weeklyResults.add(new WeeklyAvailableHoursResult(
                            yw.year(), yw.weekNumber(), 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                    ));
                    continue;
                }

                int standardHours = candidate.standardHoursPerWeek() != null ? candidate.standardHoursPerWeek() : 40;
                WeeklyAvailability avail = availabilityMap.get(new EmployeeWeekKey(candidate.employeeId(), yw));
                BigDecimal netAvailable = avail != null ? avail.getNetAvailableHours() : BigDecimal.valueOf(standardHours);

                BigDecimal totalAllocated = allocationMap.getOrDefault(
                        new EmployeeWeekKey(candidate.employeeId(), yw), BigDecimal.ZERO);

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

            String orgUnitName = candidate.orgUnitId() != null
                    ? orgUnitNames.getOrDefault(candidate.orgUnitId(), "Chưa gán")
                    : "Chưa gán";

            results.add(new ResourceSearchResult(
                    candidate.employeeId(),
                    candidate.employeeCode(),
                    candidate.fullName(),
                    candidate.orgUnitId(),
                    orgUnitName,
                    candidate.professionalRole() != null ? candidate.professionalRole() : "Nhân viên",
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

    private void validateOrgUnitScopeAccess(User currentUser, Long requestedOrgUnitId) {
        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH -> currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(requestedOrgUnitId, currentUser.getScopeOrgUnitId());
            case SELF -> false;
        };

        if (!allowed) {
            throw new PermissionDeniedException(PermissionCode.RESOURCE_SEARCH);
        }
    }

    private boolean isCandidateInDataScope(User currentUser, ResourceCandidate candidate, Map<Long, Boolean> orgUnitScopeCache) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue() != null && currentUser.getIdValue().equals(candidate.userId());
            case ORGANIZATION_BRANCH -> {
                if (candidate.orgUnitId() == null || currentUser.getScopeOrgUnitId() == null) {
                    yield false;
                }
                yield orgUnitScopeCache.computeIfAbsent(candidate.orgUnitId(), id ->
                        loadOrgUnitPort.existsInOrgUnitBranch(id, currentUser.getScopeOrgUnitId()));
            }
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
        String detail = String.format(
                "Search resources: skillId=%d, minLevel=%d, orgUnitId=%s, range=%d-W%02d..%d-W%02d, found=%d",
                query.skillId(),
                query.minProficiencyLevel(),
                query.orgUnitId() != null ? query.orgUnitId() : "all",
                query.fromYear(), query.fromWeek(),
                query.toYear(), query.toWeek(),
                resultCount
        );
        saveAuditLogPort.save(AuditLog.createChange(
                user.getId().value(),
                "SEARCH",
                "RESOURCE_MANAGEMENT",
                query.skillId(),
                null,
                detail
        ));
    }

    private record EmployeeWeekKey(Long employeeId, YearWeek yearWeek) {
    }
}
