package com.hrm.employeemanagement.domain.model.employee;

public class Employee {
    private Long id;
    private Long userId;
    private Long departmentId;
    private String employeeCode;
    private String fullName;
    private Boolean isOutsourced;
    private Integer standardHoursPerWeek;
    private String status;

    public Employee(Long id, Long userId, Long departmentId, String employeeCode, String fullName, Boolean isOutsourced, Integer standardHoursPerWeek, String status) {
        this.id = id;
        this.userId = userId;
        this.departmentId = departmentId;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.isOutsourced = isOutsourced != null ? isOutsourced : false;
        this.standardHoursPerWeek = standardHoursPerWeek != null ? standardHoursPerWeek : 40;
        this.status = status != null ? status : "ACTIVE";
    }

    public static Employee createNew(Long userId, Long departmentId, String employeeCode, String fullName) {
        return new Employee(null, userId, departmentId, employeeCode, fullName, false, 40, "ACTIVE");
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
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

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
