package com.hrm.employeemanagement.domain.orgunit;

import java.util.Objects;

public final class OrgUnitId {
    private final Long value;

    public OrgUnitId(Long value) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("OrgUnitId  phải lớn hơn 0");
        }
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrgUnitId orgUnitId = (OrgUnitId) o;

        return Objects.equals(value, orgUnitId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
