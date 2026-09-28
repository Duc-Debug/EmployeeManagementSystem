package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.skill.DeclareEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.DeleteEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.UpdateEmployeeSkillCommand;
import com.hrm.employeemanagement.application.port.inbound.skill.DeclareEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.DeleteEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.GetMyEmployeeSkillsUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.UpdateEmployeeSkillUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.DeclareSkillRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.EmployeeSkillResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/employees/me/skills")
public class EmployeeSkillController {

    private final GetMyEmployeeSkillsUseCase getMyEmployeeSkillsUseCase;
    private final DeclareEmployeeSkillUseCase declareEmployeeSkillUseCase;
    private final UpdateEmployeeSkillUseCase updateEmployeeSkillUseCase;
    private final DeleteEmployeeSkillUseCase deleteEmployeeSkillUseCase;

    public EmployeeSkillController(
            GetMyEmployeeSkillsUseCase getMyEmployeeSkillsUseCase,
            DeclareEmployeeSkillUseCase declareEmployeeSkillUseCase,
            UpdateEmployeeSkillUseCase updateEmployeeSkillUseCase,
            DeleteEmployeeSkillUseCase deleteEmployeeSkillUseCase
    ) {
        this.getMyEmployeeSkillsUseCase = getMyEmployeeSkillsUseCase;
        this.declareEmployeeSkillUseCase = declareEmployeeSkillUseCase;
        this.updateEmployeeSkillUseCase = updateEmployeeSkillUseCase;
        this.deleteEmployeeSkillUseCase = deleteEmployeeSkillUseCase;
    }

    /**
     * API Lấy danh sách kỹ năng cá nhân đã khai báo của nhân viên đang đăng nhập.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('VT-04', 'VT-06', 'ROLE_VT-04', 'ROLE_VT-06', 'EMPLOYEE_SKILL_READ', 'EMPLOYEE_SKILL_DECLARE')")
    public ResponseEntity<ApiResponse<List<EmployeeSkillResponse>>> getMySkills() {
        List<EmployeeSkillResult> results = getMyEmployeeSkillsUseCase.execute();

        List<EmployeeSkillResponse> responses = results.stream()
                .map(EmployeeSkillResponse::fromResult)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kỹ năng cá nhân thành công", responses));
    }

    /**
     * API Khai báo kỹ năng cá nhân (Dành riêng cho Nhân viên chuyên môn VT-04 và Quản trị viên VT-06)
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('VT-04', 'VT-06', 'ROLE_VT-04', 'ROLE_VT-06', 'EMPLOYEE_SKILL_DECLARE')")
    public ResponseEntity<ApiResponse<EmployeeSkillResponse>> declareSkill(
            @Valid @RequestBody DeclareSkillRequest request
    ) {
        DeclareEmployeeSkillCommand command = new DeclareEmployeeSkillCommand(
                null, // EmployeeId được tự động phân giải dựa trên tài khoản đăng nhập trong Application Service
                request.getSkillId(),
                request.getProficiencyLevel(),
                request.getYearsOfExperience()
        );

        EmployeeSkillResult result = declareEmployeeSkillUseCase.execute(command);
        EmployeeSkillResponse response = EmployeeSkillResponse.fromResult(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khai báo kỹ năng thành công. Hồ sơ đang ở trạng thái chờ duyệt.", response));
    }

    /**
     * API Cập nhật mức thành thạo và số năm kinh nghiệm kỹ năng cá nhân
     */
    @PutMapping("/{skillId}")
    @PreAuthorize("hasAnyAuthority('VT-04', 'VT-06', 'ROLE_VT-04', 'ROLE_VT-06', 'EMPLOYEE_SKILL_DECLARE')")
    public ResponseEntity<ApiResponse<EmployeeSkillResponse>> updateSkill(
            @PathVariable Long skillId,
            @Valid @RequestBody DeclareSkillRequest request
    ) {
        Long targetSkillId = (skillId != null) ? skillId : request.getSkillId();

        UpdateEmployeeSkillCommand command = new UpdateEmployeeSkillCommand(
                null,
                targetSkillId,
                request.getProficiencyLevel(),
                request.getYearsOfExperience()
        );

        EmployeeSkillResult result = updateEmployeeSkillUseCase.execute(command);
        EmployeeSkillResponse response = EmployeeSkillResponse.fromResult(result);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật kỹ năng thành công. Hồ sơ đang ở trạng thái chờ duyệt.", response));
    }

    /**
     * API Xóa kỹ năng khỏi hồ sơ cá nhân của nhân viên đang đăng nhập.
     */
    @DeleteMapping("/{skillId}")
    @PreAuthorize("hasAnyAuthority('VT-04', 'VT-06', 'ROLE_VT-04', 'ROLE_VT-06', 'EMPLOYEE_SKILL_DECLARE')")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(
            @PathVariable Long skillId
    ) {
        DeleteEmployeeSkillCommand command = new DeleteEmployeeSkillCommand(null, skillId);
        deleteEmployeeSkillUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Xóa kỹ năng thành công", null));
    }
}
