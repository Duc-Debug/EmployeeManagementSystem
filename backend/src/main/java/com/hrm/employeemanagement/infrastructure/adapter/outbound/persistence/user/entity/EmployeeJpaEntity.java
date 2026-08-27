package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    @Column(name = "professional_role")
    private String professionalRole;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "is_outsourced")
    private Boolean isOutsourced;

    @Column(name = "standard_hours_per_week", nullable = false)
    private Integer standardHoursPerWeek;

    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public EmployeeJpaEntity() {
    }

    public EmployeeJpaEntity(Long id, Long userId, Long orgUnitId, String employeeCode, String fullName,
                              String professionalRole, LocalDate startDate, LocalDate contractEndDate,
                              Boolean isOutsourced, Integer standardHoursPerWeek, String status) {
        this(id, userId, orgUnitId, employeeCode, fullName, professionalRole, startDate,
                contractEndDate, isOutsourced, standardHoursPerWeek, status, null);
    }

    public EmployeeJpaEntity(Long id, Long userId, Long orgUnitId, String employeeCode, String fullName,
                              String professionalRole, LocalDate startDate, LocalDate contractEndDate,
                              Boolean isOutsourced, Integer standardHoursPerWeek, String status, Long version) {
        this.id = id;
        this.userId = userId;
        this.orgUnitId = orgUnitId;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.professionalRole = professionalRole;
        this.startDate = startDate;
        this.contractEndDate = contractEndDate;
        this.isOutsourced = isOutsourced;
        this.standardHoursPerWeek = standardHoursPerWeek;
        this.status = status;
        this.version = version;
    }

    public EmployeeJpaEntity(Long id, Long userId, Long orgUnitId, String employeeCode, String fullName, Boolean isOutsourced, Integer standardHoursPerWeek, String status) {
        this(id, userId, orgUnitId, employeeCode, fullName, null, null, null, isOutsourced, standardHoursPerWeek, status);
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProfessionalRole() { return professionalRole; }
    public void setProfessionalRole(String professionalRole) { this.professionalRole = professionalRole; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    public Boolean getIsOutsourced() { return isOutsourced; }
    public void setIsOutsourced(Boolean outsourced) { isOutsourced = outsourced; }

    public Integer getStandardHoursPerWeek() { return standardHoursPerWeek; }
    public void setStandardHoursPerWeek(Integer standardHoursPerWeek) { this.standardHoursPerWeek = standardHoursPerWeek; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
