package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto.AllocateResourceRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/allocations")
public class ResourceAllocationController {

    private final AllocateResourceUseCase allocateResourceUseCase;

    public ResourceAllocationController(AllocateResourceUseCase allocateResourceUseCase) {
        this.allocateResourceUseCase = allocateResourceUseCase;
    }

    /**
     * NCL-06-CN-001: Phân bổ nhân sự vào dự án theo tuần. Quản lý nguồn lực
     * nhập thông tin phân bổ cho 1 nhân sự.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WeeklyCapacityResult>> allocateResource(
            @Valid @RequestBody AllocateResourceRequest request) {

        AllocateResourceCommand command = new AllocateResourceCommand(
                request.employeeId(),
                request.projectId(),
                request.year(),
                request.weekNumber(),
                request.allocatedHours()
        );

        WeeklyCapacityResult result = allocateResourceUseCase.allocateResource(command);
        return ResponseEntity.ok(ApiResponse.success("Phân bổ nhân sự vào dự án theo tuần thành công", result));
    }

    /**
     * NCL-06-CN-001: Lấy danh sách công suất và số giờ còn rảnh của nhân sự
     * theo tuần.
     */
    @GetMapping("/capacity")
    public ResponseEntity<ApiResponse<List<WeeklyCapacityResult>>> getWeeklyCapacities(
            @RequestParam List<Long> employeeIds,
            @RequestParam Integer year,
            @RequestParam Integer weekNumber) {

        List<WeeklyCapacityResult> results = allocateResourceUseCase.getWeeklyCapacities(employeeIds, year, weekNumber);
        // Đã bổ sung chuỗi thông báo "Lấy danh sách năng lực nhân sự theo tuần thành công"
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách năng lực nhân sự theo tuần thành công", results));
    }
}
