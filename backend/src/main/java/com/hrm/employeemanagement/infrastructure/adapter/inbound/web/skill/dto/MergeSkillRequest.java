package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.hrm.employeemanagement.application.dto.skill.MergeSkillCommand;

public record MergeSkillRequest(
        @NotNull(message = "ID kỹ năng đích không được để trống")
        @Positive(message = "ID kỹ năng đích phải là số dương")
        Long targetSkillId,

        @NotEmpty(message = "Danh sách ID kỹ năng nguồn không được rỗng")
        List<@NotNull(message = "ID kỹ năng nguồn không được null") @Positive(message = "ID kỹ năng nguồn phải là số dương") Long> sourceSkillIds
) {
    public MergeSkillCommand toCommand() {
        return new MergeSkillCommand(this.targetSkillId, this.sourceSkillIds);
    }
}
