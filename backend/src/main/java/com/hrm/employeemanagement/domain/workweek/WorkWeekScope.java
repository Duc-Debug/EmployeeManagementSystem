package com.hrm.employeemanagement.domain.workweek;

import com.hrm.employeemanagement.domain.exception.workweek.InvalidStandardWorkWeekException;

import java.util.Objects;

public record WorkWeekScope(ScopeType scopeType, Long orgUnitId) {

    public enum ScopeType {
        COMPANY,
        ORG_UNIT
    }

    public WorkWeekScope {
        Objects.requireNonNull(scopeType, "Phạm vi cấu hình (scopeType) không được null");
        if (scopeType == ScopeType.ORG_UNIT && (orgUnitId == null || orgUnitId <= 0)) {
            throw new InvalidStandardWorkWeekException("Đơn vị phòng ban không hợp lệ cho cấu hình cấp ORG_UNIT");
        }
        if (scopeType == ScopeType.COMPANY && orgUnitId != null) {
            throw new InvalidStandardWorkWeekException("Cấu hình cấp công ty không được có orgUnitId");
        }
    }

    public static WorkWeekScope company() {
        return new WorkWeekScope(ScopeType.COMPANY, null);
    }

    public static WorkWeekScope orgUnit(Long orgUnitId) {
        return new WorkWeekScope(ScopeType.ORG_UNIT, orgUnitId);
    }

    public String toScopeKey() {
        if (scopeType == ScopeType.COMPANY) {
            return "COMPANY:DEFAULT";
        }
        return "ORG_UNIT:" + orgUnitId;
    }

    public boolean isCompany() {
        return scopeType == ScopeType.COMPANY;
    }

    public boolean isOrgUnit() {
        return scopeType == ScopeType.ORG_UNIT;
    }
}

