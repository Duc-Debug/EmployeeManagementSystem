package com.hrm.employeemanagement.domain.employee;

import com.hrm.employeemanagement.domain.exception.employee.InvalidEmployeeDataException;
import com.hrm.employeemanagement.domain.user.UserId;

import java.time.LocalDate;
import java.util.Objects;

public class Employee {
    private EmployeeId id;
    private UserId userId;
    private Long orgUnitId;
    private String employeeCode;
    private String fullName;
    private String professionalRole;
    private LocalDate startDate;
    private LocalDate contractEndDate;
    private Boolean isOutsourced;
    private Integer standardHoursPerWeek;
    private EmployeeStatus status;

    // 11-argument constructor
    public Employee(EmployeeId id, UserId userId, Long orgUnitId, String employeeCode, String fullName,
                    String professionalRole, LocalDate startDate, LocalDate contractEndDate,
                    Boolean isOutsourced, Integer standardHoursPerWeek, EmployeeStatus status) {
        this.id = id;
        this.userId = userId;
        this.orgUnitId = orgUnitId;
        this.employeeCode = Objects.requireNonNull(employeeCode, "EmployeeCode không được null");
        this.fullName = Objects.requireNonNull(fullName, "FullName không được null");
        this.professionalRole = professionalRole;
        this.startDate = startDate;
        this.contractEndDate = contractEndDate;
        this.isOutsourced = isOutsourced != null ? isOutsourced : false;
        this.status = status != null ? status : EmployeeStatus.ACTIVE;
        setStandardHoursPerWeek(standardHoursPerWeek != null ? standardHoursPerWeek : 40);
        validateContractDates(startDate, contractEndDate);
    }

    // 8-argument constructor for backwards compatibility
    public Employee(EmployeeId id, UserId userId, Long orgUnitId, String employeeCode, String fullName,
                    Boolean isOutsourced, Integer standardHoursPerWeek, EmployeeStatus status) {
        this(id, userId, orgUnitId, employeeCode, fullName, null, null, null, isOutsourced, standardHoursPerWeek, status);
    }

    // Static Factory Method for UserService (4 parameters)
    public static Employee createNew(UserId userId, Long orgUnitId, String employeeCode, String fullName) {
        return new Employee(null, userId, orgUnitId, employeeCode, fullName, null, null, null, false, 40, EmployeeStatus.ACTIVE);
    }

    // Static Factory Method for EmployeeProfileService (8 parameters)
    public static Employee createNewProfile(UserId userId, Long orgUnitId, String employeeCode, String fullName,
                                           String professionalRole, LocalDate startDate, LocalDate contractEndDate,
                                           Integer standardHoursPerWeek) {
        return new Employee(null, userId, orgUnitId, employeeCode, fullName, professionalRole, startDate, contractEndDate, false, standardHoursPerWeek, EmployeeStatus.ACTIVE);
    }

    public void updateProfile(String fullName, Long orgUnitId, String professionalRole,
                              LocalDate startDate, LocalDate contractEndDate, Integer standardHoursPerWeek) {
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = fullName;
        }
        this.orgUnitId = orgUnitId;
        this.professionalRole = professionalRole;
        validateContractDates(startDate, contractEndDate);
        this.startDate = startDate;
        this.contractEndDate = contractEndDate;
        if (standardHoursPerWeek != null) {
            setStandardHoursPerWeek(standardHoursPerWeek);
        }
    }

    private void setStandardHoursPerWeek(Integer hours) {
        if (hours == null || hours <= 0 || hours > 168) {
            throw new InvalidEmployeeDataException("Số giờ làm việc chuẩn mỗi tuần phải lớn hơn 0 và nhỏ hơn hoặc bằng 168");
        }
        this.standardHoursPerWeek = hours;
    }

    private void validateContractDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new InvalidEmployeeDataException("Ngày kết thúc hợp đồng lao động không được nhỏ hơn ngày vào làm");
        }
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

    public String getProfessionalRole() {
        return professionalRole;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
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
