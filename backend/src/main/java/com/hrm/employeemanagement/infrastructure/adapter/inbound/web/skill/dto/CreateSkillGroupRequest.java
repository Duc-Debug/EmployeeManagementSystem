package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import com.hrm.employeemanagement.application.dto.skill.CreateSkillGroupCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSkillGroupRequest(
        @NotBlank(message = "Tên nhóm kỹ năng không được để trống")
        @Size(max = 100, message = "Tên nhóm kỹ năng không được vượt quá 100 ký tự")
        String name,

        @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
        String description
) {
    public CreateSkillGroupCommand toCommand() {
        return new CreateSkillGroupCommand(this.name, this.description);
    }
}
