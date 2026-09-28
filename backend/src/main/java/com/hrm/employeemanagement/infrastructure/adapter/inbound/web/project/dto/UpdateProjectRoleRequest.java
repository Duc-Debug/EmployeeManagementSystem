package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto;

import com.hrm.employeemanagement.application.dto.project.demand.UpdateProjectRoleCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProjectRoleRequest(
        @NotBlank(message = "Tên vai trò chuyên môn không được để trống")
        @Size(max = 100, message = "Tên vai trò chuyên môn tối đa 100 ký tự")
        String name,

        @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
        String description,

        @NotNull(message = "Nhóm kỹ năng không được để trống")
        Long skillGroupId
) {
    public UpdateProjectRoleCommand toCommand(Long id) {
        return new UpdateProjectRoleCommand(id, name, description, skillGroupId);
    }
}
