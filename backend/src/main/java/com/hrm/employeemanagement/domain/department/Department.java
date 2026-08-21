package com.hrm.employeemanagement.domain.department;

/**
 * Domain entity representing a Department in the organization.
 */
public class Department {

    private final DepartmentId id;
    private final String code;
    private final String name;
    private final DepartmentId parentId;

    public Department(DepartmentId id, String code, String name, DepartmentId parentId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.parentId = parentId;
    }

    public DepartmentId getId() {
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

    public DepartmentId getParentId() {
        return parentId;
    }

    public Long getParentIdValue() {
        return parentId != null ? parentId.value() : null;
    }
}
