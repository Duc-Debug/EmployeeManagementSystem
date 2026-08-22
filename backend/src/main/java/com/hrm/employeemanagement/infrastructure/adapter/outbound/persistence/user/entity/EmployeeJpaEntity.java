package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class EmployeeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "is_outsourced")
    private Boolean isOutsourced;

    @Column(name = "standard_hours_per_week")
    private Integer standardHoursPerWeek;

    private String status;

    public EmployeeJpaEntity() {
    }

    public EmployeeJpaEntity(Long id, Long userId, Long orgUnitId, String employeeCode, String fullName, Boolean isOutsourced, Integer standardHoursPerWeek, String status) {
        this.id = id;
        this.userId = userId;
        this.orgUnitId = orgUnitId;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.isOutsourced = isOutsourced;
        this.standardHoursPerWeek = standardHoursPerWeek;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getIsOutsourced() {
        return isOutsourced;
    }

    public void setIsOutsourced(Boolean outsourced) {
        isOutsourced = outsourced;
    }

    public Integer getStandardHoursPerWeek() {
        return standardHoursPerWeek;
    }

    public void setStandardHoursPerWeek(Integer standardHoursPerWeek) {
        this.standardHoursPerWeek = standardHoursPerWeek;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
