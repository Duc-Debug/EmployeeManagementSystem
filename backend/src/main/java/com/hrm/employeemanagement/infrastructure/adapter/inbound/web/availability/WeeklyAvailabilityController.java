package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.availability;

import com.hrm.employeemanagement.application.dto.availability.CalculateWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.dto.availability.DeclareWeeklyAvailabilityCommand;
import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;
import com.hrm.employeemanagement.application.port.inbound.availability.CalculateWeeklyCapacityUseCase;
import com.hrm.employeemanagement.application.port.inbound.availability.DeclareWeeklyAvailabilityUseCase;
import com.hrm.employeemanagement.application.port.inbound.availability.GetWeeklyAvailabilityUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.availability.dto.DeclareWeeklyAvailabilityRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
public class WeeklyAvailabilityController {

    private final DeclareWeeklyAvailabilityUseCase declareWeeklyAvailabilityUseCase;
    private final CalculateWeeklyCapacityUseCase calculateWeeklyCapacityUseCase;
    private final GetWeeklyAvailabilityUseCase getWeeklyAvailabilityUseCase;

    public WeeklyAvailabilityController(
            DeclareWeeklyAvailabilityUseCase declareWeeklyAvailabilityUseCase,
            CalculateWeeklyCapacityUseCase calculateWeeklyCapacityUseCase,
            GetWeeklyAvailabilityUseCase getWeeklyAvailabilityUseCase) {
        this.declareWeeklyAvailabilityUseCase = declareWeeklyAvailabilityUseCase;
        this.calculateWeeklyCapacityUseCase = calculateWeeklyCapacityUseCase;
        this.getWeeklyAvailabilityUseCase = getWeeklyAvailabilityUseCase;
    }

    /**
     * NCL-02-CN-003: Khai báo số giờ chuẩn một tuần cho nhân sự.
     * Vai trò: Nhân sự (VT-05) hoặc Quản trị viên (VT-06).
     */
    @PutMapping("/{employeeId}/availability")
    @PreAuthorize("hasAuthority('VT-05') or hasAuthority('VT-06') or hasAuthority('EMPLOYEE_UPDATE') or hasRole('VT-05') or hasRole('VT-06')")
    public ResponseEntity<WeeklyAvailabilityResult> declareAvailability(
            @PathVariable Long employeeId,
            @Valid @RequestBody DeclareWeeklyAvailabilityRequest request) {

        DeclareWeeklyAvailabilityCommand command = new DeclareWeeklyAvailabilityCommand(
                employeeId,
                request.year(),
                request.weekNumber(),
                request.standardHours()
        );

        WeeklyAvailabilityResult result = declareWeeklyAvailabilityUseCase.execute(command);
        return ResponseEntity.ok(result);
    }

    /**
     * Tính toán & Xem năng lực khả dụng theo tuần theo quy tắc QTN-10:
     * Khả dụng = Giờ chuẩn - Giờ lễ - Giờ nghỉ phép đã duyệt.
     * Vai trò: Quản lý nguồn lực (VT-03), Nhân sự (VT-05), Quản trị viên (VT-06).
     */
    @GetMapping("/{employeeId}/capacity")
    @PreAuthorize("hasAuthority('VT-03') or hasAuthority('VT-05') or hasAuthority('VT-06') or hasAuthority('EMPLOYEE_READ') or hasRole('VT-03') or hasRole('VT-05') or hasRole('VT-06')")
    public ResponseEntity<WeeklyAvailabilityResult> getWeeklyCapacity(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {

        CalculateWeeklyCapacityQuery query = new CalculateWeeklyCapacityQuery(employeeId, year, weekNumber);
        WeeklyAvailabilityResult result = calculateWeeklyCapacityUseCase.calculate(query);
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy thông tin giờ chuẩn / khả dụng tuần đã lưu của nhân sự.
     */
    @GetMapping("/{employeeId}/availability")
    @PreAuthorize("hasAuthority('VT-03') or hasAuthority('VT-05') or hasAuthority('VT-06') or hasAuthority('EMPLOYEE_READ') or hasRole('VT-03') or hasRole('VT-05') or hasRole('VT-06')")
    public ResponseEntity<WeeklyAvailabilityResult> getAvailability(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {

        WeeklyAvailabilityResult result = getWeeklyAvailabilityUseCase.getAvailability(employeeId, year, weekNumber);
        return ResponseEntity.ok(result);
    }
}
