package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar;

import com.hrm.employeemanagement.application.dto.calendar.HolidayResult;
import com.hrm.employeemanagement.application.port.inbound.calendar.CreateHolidayUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.DeleteHolidayUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.GetHolidaysUseCase;
import com.hrm.employeemanagement.application.port.inbound.calendar.UpdateHolidayUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto.CreateHolidayRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto.HolidayResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.calendar.dto.UpdateHolidayRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/holidays")
@Validated
public class HolidayController {

    private final GetHolidaysUseCase getHolidaysUseCase;
    private final CreateHolidayUseCase createHolidayUseCase;
    private final UpdateHolidayUseCase updateHolidayUseCase;
    private final DeleteHolidayUseCase deleteHolidayUseCase;

    public HolidayController(
            GetHolidaysUseCase getHolidaysUseCase,
            CreateHolidayUseCase createHolidayUseCase,
            UpdateHolidayUseCase updateHolidayUseCase,
            DeleteHolidayUseCase deleteHolidayUseCase) {
        this.getHolidaysUseCase = Objects.requireNonNull(getHolidaysUseCase, "getHolidaysUseCase must not be null");
        this.createHolidayUseCase = Objects.requireNonNull(createHolidayUseCase, "createHolidayUseCase must not be null");
        this.updateHolidayUseCase = Objects.requireNonNull(updateHolidayUseCase, "updateHolidayUseCase must not be null");
        this.deleteHolidayUseCase = Objects.requireNonNull(deleteHolidayUseCase, "deleteHolidayUseCase must not be null");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getHolidays(
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null && year > 0) ? year : LocalDate.now().getYear();
        List<HolidayResult> results = getHolidaysUseCase.getHolidaysByYear(targetYear);
        List<HolidayResponse> responses = results.stream()
                .map(HolidayResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ngày lễ thành công", responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HolidayResponse>> createHoliday(
            @Valid @RequestBody CreateHolidayRequest request) {
        HolidayResult result = createHolidayUseCase.createHoliday(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khai báo ngày lễ thành công", HolidayResponse.from(result)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayResponse>> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHolidayRequest request) {
        HolidayResult result = updateHolidayUseCase.updateHoliday(request.toCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin ngày lễ thành công", HolidayResponse.from(result)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        deleteHolidayUseCase.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa ngày lễ khỏi lịch thành công", null));
    }
}
