package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.skill.DeclareEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.port.inbound.employee.GetEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.skill.DeclareEmployeeSkillUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.DeclareSkillRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.EmployeeSkillResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.security.UserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/employees/me/skills")
public class EmployeeSkillController {

    private final DeclareEmployeeSkillUseCase declareEmployeeSkillUseCase;
    private final GetEmployeeProfileUseCase getEmployeeProfileUseCase;

    public EmployeeSkillController(
            DeclareEmployeeSkillUseCase declareEmployeeSkillUseCase,
            GetEmployeeProfileUseCase getEmployeeProfileUseCase
    ) {
        this.declareEmployeeSkillUseCase = declareEmployeeSkillUseCase;
        this.getEmployeeProfileUseCase = getEmployeeProfileUseCase;
    }

    /**
     * API Khai báo kỹ năng cá nhân (Dành riêng cho Nhân viên chuyên môn VT-04)
     * Kịch bản TC-03: Kiểm tra phân quyền. Nếu user không có role VT-04 sẽ ném
     * AccessDeniedException -> Kích hoạt CustomAccessDeniedHandler trả về HTTP
     * 403 Forbidden và ghi Security Log.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('VT-04') or hasRole('VT-04') or hasAuthority('EMPLOYEE_SKILL_DECLARE')")
    public ResponseEntity<ApiResponse<EmployeeSkillResponse>> declareSkill(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody DeclareSkillRequest request
    ) {
        // Lấy ID hồ sơ nhân sự dựa trên ID tài khoản đang đăng nhập
        Long employeeId = getEmployeeProfileUseCase.getByUserId(currentUser.getId()).id();

        DeclareEmployeeSkillCommand command = new DeclareEmployeeSkillCommand(
                employeeId,
                request.getSkillId(),
                request.getProficiencyLevel(),
                request.getYearsOfExperience()
        );

        EmployeeSkillResult result = declareEmployeeSkillUseCase.execute(command);
        EmployeeSkillResponse response = EmployeeSkillResponse.fromResult(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khai báo kỹ năng thành công. Hồ sơ đang ở trạng thái chờ duyệt.", response));
    }
}
