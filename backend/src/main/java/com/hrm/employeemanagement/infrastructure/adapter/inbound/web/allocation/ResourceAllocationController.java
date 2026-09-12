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
import com.hrm.employeemanagement.application.dto.allocation.ResourceSearchResult;
import com.hrm.employeemanagement.application.dto.allocation.SearchResourceQuery;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.SearchResourceBySkillAndAvailabilityUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto.AllocateResourceRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/allocations")
@Validated
public class ResourceAllocationController {

    private final AllocateResourceUseCase allocateResourceUseCase;
    private final com.hrm.employeemanagement.application.port.inbound.allocation.BulkAllocateResourceUseCase bulkAllocateResourceUseCase;
    private final SearchResourceBySkillAndAvailabilityUseCase searchResourceUseCase;
    private final com.hrm.employeemanagement.application.port.inbound.allocation.GetCompanyWeeklyCapacityUseCase getCompanyWeeklyCapacityUseCase;

    public ResourceAllocationController(
            AllocateResourceUseCase allocateResourceUseCase,
            com.hrm.employeemanagement.application.port.inbound.allocation.BulkAllocateResourceUseCase bulkAllocateResourceUseCase,
            SearchResourceBySkillAndAvailabilityUseCase searchResourceUseCase,
            com.hrm.employeemanagement.application.port.inbound.allocation.GetCompanyWeeklyCapacityUseCase getCompanyWeeklyCapacityUseCase) {
        this.allocateResourceUseCase = allocateResourceUseCase;
        this.bulkAllocateResourceUseCase = bulkAllocateResourceUseCase;
        this.searchResourceUseCase = searchResourceUseCase;
        this.getCompanyWeeklyCapacityUseCase = getCompanyWeeklyCapacityUseCase;
    }

    /**
     * NCL-06-CN-002: Xem bảng năng lực theo tuần của công ty / bộ phận.
     */
    @GetMapping("/weekly-matrix")
    public ResponseEntity<ApiResponse<com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult>> getWeeklyCapacityMatrix(
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false, defaultValue = "8") @Min(1) @Max(16) Integer durationWeeks,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(50) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.hrm.employeemanagement.domain.allocation.CapacityStatus status) {

        com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityQuery query =
                new com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityQuery(
                        orgUnitId, fromYear, fromWeek, durationWeeks, page, size, search, status);

        com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult result =
                getCompanyWeeklyCapacityUseCase.getWeeklyCapacityMatrix(query);

        return ResponseEntity.ok(ApiResponse.success("Lấy bảng năng lực theo tuần thành công", result));
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
                request.allocatedHours(),
                request.allocationPercentage(),
                request.overloadReason()
        );

        WeeklyCapacityResult result = allocateResourceUseCase.allocateResource(command);
        return ResponseEntity.ok(ApiResponse.success("Phân bổ nhân sự vào dự án theo tuần thành công", result));
    }

    /**
     * NCL-06-CN-006: Phân bổ hàng loạt cho nhiều tuần trong một thao tác.
     */
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<com.hrm.employeemanagement.application.dto.allocation.BulkAllocationResult>> bulkAllocateResource(
            @Valid @RequestBody com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto.BulkAllocateResourceRequest request) {

        com.hrm.employeemanagement.application.dto.allocation.BulkAllocateResourceCommand command =
                new com.hrm.employeemanagement.application.dto.allocation.BulkAllocateResourceCommand(
                        request.employeeId(),
                        request.projectId(),
                        request.fromYear(),
                        request.fromWeek(),
                        request.toYear(),
                        request.toWeek(),
                        request.allocatedHoursPerWeek(),
                        request.allocationPercentagePerWeek()
                );

        com.hrm.employeemanagement.application.dto.allocation.BulkAllocationResult result =
                bulkAllocateResourceUseCase.bulkAllocateResource(command);

        String message = result.blockedCount() == 0
                ? "Phân bổ hàng loạt cho nhiều tuần thành công"
                : "Phân bổ hàng loạt hoàn tất với " + result.blockedCount() + " tuần bị vướng ràng buộc";

        return ResponseEntity.ok(ApiResponse.success(message, result));
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

    /**
     * NCL-02-CN-004: Tìm kiếm nhân sự theo kỹ năng và độ rảnh trong khoảng tuần.
     *
     * @param minProficiencyLevel Mức độ thành thạo tối thiểu (chuẩn mới).
     * @param minLevel Tham số cũ (đã deprecated, dùng minProficiencyLevel thay thế).
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ResourceSearchResult>>> searchResources(
            @RequestParam Long skillId,
            @RequestParam(name = "minProficiencyLevel", required = false) Integer minProficiencyLevel,
            @Deprecated @RequestParam(name = "minLevel", required = false) Integer minLevel,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam Integer fromYear,
            @RequestParam Integer fromWeek,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toWeek,
            @RequestParam(required = false) Integer durationWeeks,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (minProficiencyLevel != null && minLevel != null && !minProficiencyLevel.equals(minLevel)) {
            throw new IllegalArgumentException("Không được truyền đồng thời cả minProficiencyLevel và minLevel với giá trị khác nhau");
        }

        boolean hasToYear = toYear != null;
        boolean hasToWeek = toWeek != null;
        if (hasToYear != hasToWeek) {
            throw new IllegalArgumentException("toYear và toWeek phải được cung cấp cùng nhau");
        }

        if (!hasToYear) {
            int duration = (durationWeeks != null && durationWeeks > 0) ? durationWeeks : 4;
            java.time.LocalDate endMonday = com.hrm.employeemanagement.domain.availability.YearWeek
                    .of(fromYear, fromWeek).getStartDate().plusWeeks(duration - 1);
            com.hrm.employeemanagement.domain.availability.YearWeek endWeek = 
                    com.hrm.employeemanagement.domain.availability.YearWeek.from(endMonday);
            toYear = endWeek.year();
            toWeek = endWeek.weekNumber();
        }

        Integer effectiveMinLevel = minProficiencyLevel != null ? minProficiencyLevel : (minLevel != null ? minLevel : 1);
        SearchResourceQuery query = new SearchResourceQuery(
                skillId,
                effectiveMinLevel,
                orgUnitId,
                fromYear,
                fromWeek,
                toYear,
                toWeek,
                page,
                size
        );
        List<ResourceSearchResult> results = searchResourceUseCase.search(query);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm nhân sự theo kỹ năng và mức độ rảnh thành công", results));
    }
}
