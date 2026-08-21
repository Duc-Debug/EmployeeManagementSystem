package com.hrm.employeemanagement.domain.employee;

import com.hrm.employeemanagement.domain.user.UserId;

public class Employee {
    private EmployeeId id;
    private UserId userId;
    private Long departmentId;
    private String employeeCode;
    private String fullName;
    private Boolean isOutsourced;
    private Integer standardHoursPerWeek;
    private String status;

    public Employee(EmployeeId id, UserId userId, Long departmentId, String employeeCode, String fullName, Boolean isOutsourced, Integer standardHoursPerWeek, String status) {
        this.id = id;
        this.userId = userId;
        this.departmentId = departmentId;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.isOutsourced = isOutsourced != null ? isOutsourced : false;
        this.standardHoursPerWeek = standardHoursPerWeek != null ? standardHoursPerWeek : 40;
        this.status = status != null ? status : "ACTIVE";
    }

    public static Employee createNew(UserId userId, Long departmentId, String employeeCode, String fullName) {
        return new Employee(null, userId, departmentId, employeeCode, fullName, false, 40, "ACTIVE");
    }

    public EmployeeId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public UserId getUserId() {
        return userId;
    }

    public Long getUserIdValue() {
        return userId != null ? userId.value() : null;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public Boolean getIsOutsourced() {
        return isOutsourced;
    }

    public Integer getStandardHoursPerWeek() {
        return standardHoursPerWeek;
    }

    public String getStatus() {
        return status;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
