package com.hrm.employeemanagement.domain.employee;

import com.hrm.employeemanagement.domain.user.UserId;

import java.util.Objects;

public class Employee {
    private EmployeeId id;
    private UserId userId;
    private Long orgUnitId;
    private String employeeCode;
    private String fullName;
    private Boolean isOutsourced;
    private Integer standardHoursPerWeek;
    private EmployeeStatus status;

    public Employee(EmployeeId id, UserId userId, Long orgUnitId, String employeeCode, String fullName, Boolean isOutsourced, Integer standardHoursPerWeek, EmployeeStatus status) {
        this.id = id;
        this.userId = userId;
        this.orgUnitId = orgUnitId;
        this.employeeCode = Objects.requireNonNull(employeeCode, "EmployeeCode không được null");
        this.fullName = Objects.requireNonNull(fullName, "FullName không được null");
        this.isOutsourced = isOutsourced != null ? isOutsourced : false;
        this.standardHoursPerWeek = standardHoursPerWeek != null ? standardHoursPerWeek : 40;
        this.status = status != null ? status : EmployeeStatus.ACTIVE;
    }

    public static Employee createNew(UserId userId, Long orgUnitId, String employeeCode, String fullName) {
        return new Employee(null, userId, orgUnitId, employeeCode, fullName, false, 40, EmployeeStatus.ACTIVE);
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

    public Long getOrgUnitId() {
        return orgUnitId;
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

    public EmployeeStatus getStatus() {
        return status;
    }

    public String getStatusValue() {
        return status != null ? status.name() : EmployeeStatus.ACTIVE.name();
    }

    public void linkUser(UserId userId) {
        this.userId = Objects.requireNonNull(userId, "UserId không được null");
    }

    public void assignToOrgUnit(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public void changeStatus(EmployeeStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "Trạng thái nhân viên không được null");
    }
}
