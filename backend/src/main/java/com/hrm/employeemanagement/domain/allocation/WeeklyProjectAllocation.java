package com.hrm.employeemanagement.domain.allocation;

import java.math.BigDecimal;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationHoursException;

/**
 * Domain Entity đại diện cho thông tin phân bổ số giờ làm việc của 1 nhân sự
 * cho 1 dự án cụ thể trong 1 tuần.
 */
public class WeeklyProjectAllocation {

    private Long id;
    private Long employeeId;
    private Long projectId;
    private YearWeek yearWeek;
    private BigDecimal allocatedHours;
    private Long version;

    public WeeklyProjectAllocation(Long id, Long employeeId, Long projectId, YearWeek yearWeek, BigDecimal allocatedHours) {
        this(id, employeeId, projectId, yearWeek, allocatedHours, 0L);
    }

    public WeeklyProjectAllocation(Long id, Long employeeId, Long projectId, YearWeek yearWeek,
            BigDecimal allocatedHours, Long version) {
        this.id = id;
        this.employeeId = Objects.requireNonNull(employeeId, "ID nhân sự không được null");
        this.projectId = Objects.requireNonNull(projectId, "ID dự án không được null");
        this.yearWeek = Objects.requireNonNull(yearWeek, "Tuần/Năm (YearWeek) không được null");
        setAllocatedHours(allocatedHours);
        this.version = version != null ? version : 0L;
    }

    /**
     * Phương thức khởi tạo một bản ghi phân bổ mới chưa có ID.
     */
    public static WeeklyProjectAllocation createNew(Long employeeId, Long projectId, YearWeek yearWeek, BigDecimal allocatedHours) {
        return new WeeklyProjectAllocation(null, employeeId, projectId, yearWeek, allocatedHours);
    }

    /**
     * Cập nhật số giờ phân bổ mới. Tự động kiểm tra ràng buộc số giờ âm (Đáp
     * ứng TC-03).
     */
    public void updateAllocatedHours(BigDecimal newAllocatedHours) {
        setAllocatedHours(newAllocatedHours);
    }

    private void setAllocatedHours(BigDecimal hours) {
        if (hours == null) {
            this.allocatedHours = BigDecimal.ZERO;
            return;
        }

        // [TC-03] Kiểm tra số giờ phân bổ không được là số âm
        if (hours.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAllocationHoursException("Số giờ phân bổ cho dự án không được là số âm: " + hours);
        }

        // Ràng buộc bảo vệ: 1 tuần tối đa có 168 giờ
        if (hours.compareTo(BigDecimal.valueOf(168)) > 0) {
            throw new InvalidAllocationHoursException("Số giờ phân bổ cho 1 dự án trong tuần không được vượt quá 168 giờ");
        }

        this.allocatedHours = hours;
    }

    // Các phương thức Getters
    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getProjectId() {
        return projectId;
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

    public BigDecimal getAllocatedHours() {
        return allocatedHours;
    }

    public Long getVersion() {
        return version;
    }
}
