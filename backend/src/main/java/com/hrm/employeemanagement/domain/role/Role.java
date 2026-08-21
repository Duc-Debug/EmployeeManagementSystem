package com.hrm.employeemanagement.domain.role;

import java.util.Objects;

public class Role {
    private final RoleId id;
    private final RoleCode code;
    private final String name;

    public Role(RoleId id, RoleCode code, String name) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "RoleCode không được null");
        this.name = name != null ? name : code.getName();
    }

    public RoleId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public RoleCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id) || code == role.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
