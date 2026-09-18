package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityConflictCheckResult;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityConflictCheckResult.ConflictingAllocationInfo;
import com.hrm.employeemanagement.application.port.inbound.unavailability.CheckUnavailabilityConflictUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityDeclarationNotFoundException;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * NCL-13-CN-003: Kiểm tra xung đột với phân bổ hiện có trước khi quản lý duyệt (TC-02).
 */
public class CheckUnavailabilityConflictService implements CheckUnavailabilityConflictUseCase {

    private final LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final AuthorizationService authorizationService;

    public CheckUnavailabilityConflictService(
            LoadUnavailabilityDeclarationPort loadUnavailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            AuthorizationService authorizationService
    ) {
        this.loadUnavailabilityPort = Objects.requireNonNull(loadUnavailabilityPort, "loadUnavailabilityPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "loadAllocationPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    @Override
    public UnavailabilityConflictCheckResult checkConflict(Long declarationId) {
        authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE);

        UnavailabilityDeclaration declaration = loadUnavailabilityPort.findById(declarationId)
                .orElseThrow(() -> new UnavailabilityDeclarationNotFoundException(
                        "Không tìm thấy khai báo thời gian không sẵn sàng với mã: " + declarationId));

        Set<YearWeek> affectedWeeks = extractAffectedWeeks(declaration.getStartDate(), declaration.getEndDate());
        List<ConflictingAllocationInfo> conflicts = new ArrayList<>();
        BigDecimal totalHours = BigDecimal.ZERO;

        for (YearWeek yw : affectedWeeks) {
            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployee(
                    declaration.getEmployeeId(), yw);
            if (allocations != null) {
                for (WeeklyProjectAllocation alloc : allocations) {
                    if (alloc.getAllocatedHours() != null && alloc.getAllocatedHours().compareTo(BigDecimal.ZERO) > 0) {
                        conflicts.add(new ConflictingAllocationInfo(
                                alloc.getId(),
                                alloc.getProjectId(),
                                alloc.getYear(),
                                alloc.getWeekNumber(),
                                alloc.getAllocatedHours()
                        ));
                        totalHours = totalHours.add(alloc.getAllocatedHours());
                    }
                }
            }
        }

        boolean hasConflict = !conflicts.isEmpty();
        String warningMessage = hasConflict
                ? String.format("Cảnh báo: Khoảng thời gian khai báo trùng với %d phân bổ dự án đã có trong tuần (tổng %s giờ).",
                conflicts.size(), totalHours)
                : null;

        return new UnavailabilityConflictCheckResult(
                hasConflict,
                conflicts.size(),
                totalHours,
                conflicts,
                warningMessage
        );
    }

    private Set<YearWeek> extractAffectedWeeks(LocalDate startDate, LocalDate endDate) {
        Set<YearWeek> weeks = new LinkedHashSet<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            weeks.add(YearWeek.from(curr));
            curr = curr.plusDays(1);
        }
        return weeks;
    }
}
