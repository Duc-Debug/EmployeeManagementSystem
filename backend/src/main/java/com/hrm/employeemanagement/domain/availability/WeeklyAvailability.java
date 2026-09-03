package com.hrm.employeemanagement.domain.availability;

import java.math.BigDecimal;
import java.util.Objects;

public class WeeklyAvailability {

    private Long id;
    private Long employeeId;
    private YearWeek yearWeek;
    private int standardHours;
    private int holidayHours;
    private BigDecimal approvedLeaveHours;
    private BigDecimal netAvailableHours;

    public WeeklyAvailability(Long id, Long employeeId, YearWeek yearWeek, int standardHours,
                              int holidayHours, BigDecimal approvedLeaveHours, BigDecimal netAvailableHours) {
        this.id = id;
        this.employeeId = Objects.requireNonNull(employeeId, "EmployeeId không được null");
        this.yearWeek = Objects.requireNonNull(yearWeek, "YearWeek không được null");
        WeeklyAvailabilityPolicy.validateStandardHours(standardHours);
        this.standardHours = standardHours;
        this.holidayHours = Math.max(0, holidayHours);
        this.approvedLeaveHours = approvedLeaveHours != null ? approvedLeaveHours : BigDecimal.ZERO;
        this.netAvailableHours = netAvailableHours != null ? netAvailableHours :
                WeeklyAvailabilityPolicy.calculateNetAvailableHours(this.standardHours, this.holidayHours, this.approvedLeaveHours);
    }

    public static WeeklyAvailability createCalculated(Long employeeId, YearWeek yearWeek, int standardHours,
                                                      int holidayHours, BigDecimal approvedLeaveHours) {
        BigDecimal net = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, approvedLeaveHours);
        return new WeeklyAvailability(null, employeeId, yearWeek, standardHours, holidayHours, approvedLeaveHours, net);
    }

    public void updateStandardHours(int newStandardHours) {
        WeeklyAvailabilityPolicy.validateStandardHours(newStandardHours);
        this.standardHours = newStandardHours;
        recalculate();
    }

    public void recalculate() {
        this.netAvailableHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(
                this.standardHours, this.holidayHours, this.approvedLeaveHours);
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public YearWeek getYearWeek() {
        return yearWeek;
    }

    public int getYear() {
        return yearWeek.year();
    }

    public int getWeekNumber() {
        return yearWeek.weekNumber();
    }

    public int getStandardHours() {
        return standardHours;
    }

    public int getHolidayHours() {
        return holidayHours;
    }

    public BigDecimal getApprovedLeaveHours() {
        return approvedLeaveHours;
    }

    public BigDecimal getNetAvailableHours() {
        return netAvailableHours;
    }
}
