package com.hrm.employeemanagement.domain.allocation.template;

import java.math.BigDecimal;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.allocation.InvalidRoleAllocationTemplateException;

public class ProjectRoleAllocationTemplateItem {
    private Long id;
    private Long templateId;
    private Long roleId;
    private BigDecimal hoursPerWeek;

    public ProjectRoleAllocationTemplateItem(Long id, Long templateId, Long roleId, BigDecimal hoursPerWeek) {
        validate(roleId, hoursPerWeek);
        this.id = id;
        this.templateId = templateId;
        this.roleId = roleId;
        this.hoursPerWeek = hoursPerWeek;
    }

    public static ProjectRoleAllocationTemplateItem create(Long roleId, BigDecimal hoursPerWeek) {
        return new ProjectRoleAllocationTemplateItem(null, null, roleId, hoursPerWeek);
    }

    public static ProjectRoleAllocationTemplateItem create(Long templateId, Long roleId, BigDecimal hoursPerWeek) {
        return new ProjectRoleAllocationTemplateItem(null, templateId, roleId, hoursPerWeek);
    }

    private void validate(Long roleId, BigDecimal hoursPerWeek) {
        if (roleId == null || roleId <= 0) {
            throw new InvalidRoleAllocationTemplateException("Vai trò dự án không hợp lệ");
        }
        if (hoursPerWeek == null || hoursPerWeek.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRoleAllocationTemplateException("Số giờ mỗi tuần phải lớn hơn 0");
        }
        if (hoursPerWeek.compareTo(BigDecimal.valueOf(168)) > 0) {
            throw new InvalidRoleAllocationTemplateException("Số giờ mỗi tuần không được vượt quá 168 giờ");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public BigDecimal getHoursPerWeek() {
        return hoursPerWeek;
    }

    public void setHoursPerWeek(BigDecimal hoursPerWeek) {
        validate(this.roleId, hoursPerWeek);
        this.hoursPerWeek = hoursPerWeek;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectRoleAllocationTemplateItem that = (ProjectRoleAllocationTemplateItem) o;
        return Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId);
    }
}

