package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto;

import com.hrm.employeemanagement.application.dto.calendar.UpdateHolidayCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateHolidayRequest(
        @NotNull(message = "Ngày lễ không được để trống")
        LocalDate holidayDate,

        @NotBlank(message = "Tên ngày lễ không được để trống")
        @Size(max = 255, message = "Tên ngày lễ không được vượt quá 255 ký tự")
        String name,

        @Min(value = 1, message = "Số giờ khấu trừ phải từ 1 đến 24 giờ")
        @Max(value = 24, message = "Số giờ khấu trừ phải từ 1 đến 24 giờ")
        Integer workingHoursDeducted
) {
    public UpdateHolidayCommand toCommand(Long id) {
        return new UpdateHolidayCommand(id, holidayDate, name, workingHoursDeducted != null ? workingHoursDeducted : 8);
    }
}
