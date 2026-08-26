package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import com.hrm.employeemanagement.application.dto.skill.CreateSkillCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSkillRequest(
        @NotNull(message = "ID nhóm kỹ năng không được để trống")
        @Positive(message = "ID nhóm kỹ năng phải là số dương")
        Long groupId,

        @NotBlank(message = "Tên kỹ năng không được để trống")
        @Size(max = 100, message = "Tên kỹ năng không được vượt quá 100 ký tự")
        String name,

        @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
        String description
) {
    public CreateSkillCommand toCommand() {
        return new CreateSkillCommand(this.groupId, this.name, this.description);
    }
}
