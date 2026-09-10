package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.skill.ApproveEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.PendingEmployeeSkillItemResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.skill.ApproveEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.GetPendingEmployeeSkillsUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.ApproveEmployeeSkillRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.EmployeeSkillResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/employee-skills")
public class EmployeeSkillApprovalController {

    private final ApproveEmployeeSkillUseCase approveEmployeeSkillUseCase;
    private final GetPendingEmployeeSkillsUseCase getPendingEmployeeSkillsUseCase;

    public EmployeeSkillApprovalController(
            ApproveEmployeeSkillUseCase approveEmployeeSkillUseCase,
            GetPendingEmployeeSkillsUseCase getPendingEmployeeSkillsUseCase
    ) {
        this.approveEmployeeSkillUseCase = approveEmployeeSkillUseCase;
        this.getPendingEmployeeSkillsUseCase = getPendingEmployeeSkillsUseCase;
    }

    /**
     * API xem các kỹ năng đang chờ xác nhận (Dành cho Quản lý nguồn lực VT-03)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('VT-03') or hasRole('VT-03') or hasAuthority('EMPLOYEE_SKILL_APPROVE') or hasAuthority('VT-06') or hasRole('VT-06')")
    public ResponseEntity<ApiResponse<?>> getPendingSkills(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 10;
            PageResult<PendingEmployeeSkillItemResult> result = getPendingEmployeeSkillsUseCase.execute(keyword, p, s);
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kỹ năng chờ xác nhận thành công", result));
        }

        List<PendingEmployeeSkillItemResult> results = getPendingEmployeeSkillsUseCase.execute(keyword);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kỹ năng chờ xác nhận thành công", results));
    }

    /**
     * API xác nhận giữ nguyên hoặc điều chỉnh mức thành thạo kèm ghi chú (NCL-02-CN-006)
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('VT-03') or hasRole('VT-03') or hasAuthority('EMPLOYEE_SKILL_APPROVE') or hasAuthority('VT-06') or hasRole('VT-06')")
    public ResponseEntity<ApiResponse<EmployeeSkillResponse>> approveSkill(
            @PathVariable Long id,
            @Valid @RequestBody ApproveEmployeeSkillRequest request
    ) {
        ApproveEmployeeSkillCommand command = new ApproveEmployeeSkillCommand(
                id,
                request.getAdjustedProficiencyLevel(),
                request.getReviewNotes()
        );

        EmployeeSkillResult result = approveEmployeeSkillUseCase.execute(command);
        EmployeeSkillResponse response = EmployeeSkillResponse.fromResult(result);

        return ResponseEntity.ok(ApiResponse.success("Xác nhận mức thành thạo thành công", response));
    }
    /**
     * API từ chối yêu cầu khai báo kỹ năng kèm lý do (NCL-02-CN-006)
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('VT-03') or hasRole('VT-03') or hasAuthority('EMPLOYEE_SKILL_APPROVE') or hasAuthority('VT-06') or hasRole('VT-06')")
    public ResponseEntity<ApiResponse<EmployeeSkillResponse>> rejectSkill(
            @PathVariable Long id,
            @RequestBody(required = false) com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.RejectEmployeeSkillRequest request
    ) {
        String reason = request != null ? request.getRejectionReason() : null;
        com.hrm.employeemanagement.application.dto.skill.RejectEmployeeSkillCommand command =
                new com.hrm.employeemanagement.application.dto.skill.RejectEmployeeSkillCommand(id, reason);

        EmployeeSkillResult result = approveEmployeeSkillUseCase.reject(command);
        EmployeeSkillResponse response = EmployeeSkillResponse.fromResult(result);

        return ResponseEntity.ok(ApiResponse.success("Từ chối kỹ năng thành công", response));
    }
}
