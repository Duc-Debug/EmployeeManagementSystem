package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar;

import com.hrm.employeemanagement.application.dto.calendar.CompanyWorkingCalendarResult;
import com.hrm.employeemanagement.application.port.inbound.calendar.GetWorkingCalendarUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.UpdateWorkingCalendarUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto.UpdateWorkingCalendarRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto.WorkingCalendarResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/working-calendar")
@Validated
public class WorkingCalendarController {

    private final GetWorkingCalendarUseCase getWorkingCalendarUseCase;
    private final UpdateWorkingCalendarUseCase updateWorkingCalendarUseCase;

    public WorkingCalendarController(
            GetWorkingCalendarUseCase getWorkingCalendarUseCase,
            UpdateWorkingCalendarUseCase updateWorkingCalendarUseCase) {
        this.getWorkingCalendarUseCase = Objects.requireNonNull(getWorkingCalendarUseCase, "getWorkingCalendarUseCase must not be null");
        this.updateWorkingCalendarUseCase = Objects.requireNonNull(updateWorkingCalendarUseCase, "updateWorkingCalendarUseCase must not be null");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WorkingCalendarResponse>> getWorkingCalendar() {
        CompanyWorkingCalendarResult result = getWorkingCalendarUseCase.getWorkingCalendar();
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy cấu hình lịch làm việc thành công",
                WorkingCalendarResponse.from(result)
        ));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<WorkingCalendarResponse>> updateWorkingCalendar(
            @Valid @RequestBody UpdateWorkingCalendarRequest request) {
        CompanyWorkingCalendarResult result = updateWorkingCalendarUseCase.updateWorkingCalendar(request.toDtos());
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật cấu hình lịch làm việc thành công",
                WorkingCalendarResponse.from(result)
        ));
    }
}
