package com.hrm.employeemanagement.domain.allocation.template;

import java.math.BigDecimal;

public class RoleAllocationSuggestionItem {
    private final Long roleId;
    private final String roleCode;
    private final String roleName;
    private final BigDecimal hoursPerWeek;
    private final Long suggestedEmployeeId;
    private final String suggestedEmployeeName;
    private final String suggestedEmployeeCode;
    private final boolean assigned;
    private final String warningMessage;

    public RoleAllocationSuggestionItem(
            Long roleId,
            String roleCode,
            String roleName,
            BigDecimal hoursPerWeek,
            Long suggestedEmployeeId,
            String suggestedEmployeeName,
            String suggestedEmployeeCode,
            boolean assigned,
            String warningMessage
    ) {
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.hoursPerWeek = hoursPerWeek;
        this.suggestedEmployeeId = suggestedEmployeeId;
        this.suggestedEmployeeName = suggestedEmployeeName;
        this.suggestedEmployeeCode = suggestedEmployeeCode;
        this.assigned = assigned;
        this.warningMessage = warningMessage;
    }

    public static RoleAllocationSuggestionItem matched(
            Long roleId,
            String roleCode,
            String roleName,
            BigDecimal hoursPerWeek,
            Long employeeId,
            String employeeName,
            String employeeCode
    ) {
        return new RoleAllocationSuggestionItem(
                roleId,
                roleCode,
                roleName,
                hoursPerWeek,
                employeeId,
                employeeName,
                employeeCode,
                true,
                null
        );
    }

    public static RoleAllocationSuggestionItem unassigned(
            Long roleId,
            String roleCode,
            String roleName,
            BigDecimal hoursPerWeek
    ) {
        String warning = "Cần bổ sung nhân sự cho vai trò " + (roleName != null ? roleName : roleCode);
        return new RoleAllocationSuggestionItem(
                roleId,
                roleCode,
                roleName,
                hoursPerWeek,
                null,
                null,
                null,
                false,
                warning
        );
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public BigDecimal getHoursPerWeek() {
        return hoursPerWeek;
    }

    public Long getSuggestedEmployeeId() {
        return suggestedEmployeeId;
    }

    public String getSuggestedEmployeeName() {
        return suggestedEmployeeName;
    }

    public String getSuggestedEmployeeCode() {
        return suggestedEmployeeCode;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public String getWarningMessage() {
        return warningMessage;
    }
}

