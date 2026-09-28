package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.scenario.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateScenarioRequest(
        @Size(max = 50, message = "Mã kịch bản không được vượt quá 50 ký tự")
        String code,

        @NotBlank(message = "Tên kịch bản không được để trống")
        @Size(max = 255, message = "Tên kịch bản không được vượt quá 255 ký tự")
        String name,

        String description,

        Long orgUnitId,

        @Min(value = 2000, message = "Năm bắt đầu không hợp lệ")
        Integer fromYear,

        @Min(value = 1, message = "Tuần bắt đầu phải từ 1 đến 53")
        @Max(value = 53, message = "Tuần bắt đầu phải từ 1 đến 53")
        Integer fromWeek,

        @NotNull(message = "Số tuần mô phỏng không được để trống")
        @Min(value = 1, message = "Số tuần mô phỏng tối thiểu là 1 tuần")
        @Max(value = 16, message = "Số tuần mô phỏng tối đa là 16 tuần")
        Integer durationWeeks
) {
    @AssertTrue(message = "fromYear và fromWeek phải cùng được cung cấp hoặc cùng để trống")
    public boolean isYearWeekPairValid() {
        return (fromYear == null && fromWeek == null) || (fromYear != null && fromWeek != null);
    }
}
