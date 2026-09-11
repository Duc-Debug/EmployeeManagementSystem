package com.hrm.employeemanagement.domain.project.demand;

import java.util.Objects;

public class ProjectRole {

    private final ProjectRoleId id;
    private final String code;
    private final String name;
    private final String description;

    public ProjectRole(ProjectRoleId id, String code, String name, String description) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "code không được null");
        this.name = Objects.requireNonNull(name, "name không được null");
        this.description = description;
    }

    public ProjectRoleId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
