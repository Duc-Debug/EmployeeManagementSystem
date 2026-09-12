package com.hrm.employeemanagement.domain.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A weekly capacity is an employee property, not a skill property. To keep the
 * recruitment report useful for multi-skilled employees without counting the
 * same hours more than once, capacity is distributed across their approved
 * skills in proportion to the declared proficiency level.
 *
 * <p>The same allocator is also used for role demand. Role-to-skill mappings
 * describe a combined role requirement, so its required hours are split across
 * the mapped skills rather than copied to every one of them.</p>
 */
public final class EmployeeCapacityAttributionPolicy {

    private EmployeeCapacityAttributionPolicy() {
    }

    public static Map<Long, BigDecimal> allocateHours(BigDecimal totalHours, Map<Long, Integer> weightsBySkillId) {
        if (totalHours == null || totalHours.signum() <= 0 || weightsBySkillId == null || weightsBySkillId.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> normalizedWeights = new TreeMap<>();
        weightsBySkillId.forEach((skillId, weight) -> {
            if (skillId != null) {
                normalizedWeights.merge(skillId, normalizeWeight(weight), Math::max);
            }
        });
        if (normalizedWeights.isEmpty()) {
            return Map.of();
        }

        long totalWeight = normalizedWeights.values().stream().mapToLong(Integer::longValue).sum();
        Map<Long, BigDecimal> allocation = new LinkedHashMap<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : normalizedWeights.entrySet()) {
            BigDecimal share = totalHours
                    .multiply(BigDecimal.valueOf(entry.getValue()))
                    .divide(BigDecimal.valueOf(totalWeight), 8, RoundingMode.HALF_UP);
            allocation.put(entry.getKey(), share);
            allocated = allocated.add(share);
        }

        // Assign the rounding remainder deterministically, so allocated hours
        // always equal the employee/role total exactly.
        Long firstSkillId = allocation.keySet().iterator().next();
        BigDecimal roundingRemainder = totalHours.subtract(allocated);
        allocation.computeIfPresent(firstSkillId, (ignored, share) -> share.add(roundingRemainder));
        return Map.copyOf(allocation);
    }

    public static Map<Long, Integer> equalWeights(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> weights = new LinkedHashMap<>();
        skillIds.stream().filter(java.util.Objects::nonNull).forEach(skillId -> weights.put(skillId, 1));
        return Map.copyOf(weights);
    }

    private static int normalizeWeight(Integer weight) {
        return weight == null || weight < 1 ? 1 : weight;
    }
}
