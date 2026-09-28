package com.hrm.employeemanagement.domain.allocation.template;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class RoleAllocationSuggestionPolicy {

    private RoleAllocationSuggestionPolicy() {
    }

    public record CandidateAvailability(
            Long employeeId,
            String employeeName,
            String employeeCode,
            BigDecimal minRemainingHoursAcrossWeeks
    ) {}

    public static RoleAllocationSuggestionItem suggestCandidateForRole(
            Long roleId,
            String roleCode,
            String roleName,
            BigDecimal hoursPerWeek,
            List<CandidateAvailability> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return RoleAllocationSuggestionItem.unassigned(roleId, roleCode, roleName, hoursPerWeek);
        }

        // Tìm ứng viên có minRemainingHoursAcrossWeeks >= hoursPerWeek
        // Sắp xếp ưu tiên ứng viên có nhiều giờ rảnh nhất
        return candidates.stream()
                .filter(c -> c.minRemainingHoursAcrossWeeks() != null && c.minRemainingHoursAcrossWeeks().compareTo(hoursPerWeek) >= 0)
                .max(Comparator.comparing(CandidateAvailability::minRemainingHoursAcrossWeeks))
                .map(selected -> RoleAllocationSuggestionItem.matched(
                        roleId,
                        roleCode,
                        roleName,
                        hoursPerWeek,
                        selected.employeeId(),
                        selected.employeeName(),
                        selected.employeeCode()
                ))
                .orElseGet(() -> RoleAllocationSuggestionItem.unassigned(roleId, roleCode, roleName, hoursPerWeek));
    }
}

