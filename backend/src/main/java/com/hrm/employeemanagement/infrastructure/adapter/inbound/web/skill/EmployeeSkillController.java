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
import com.hrm.employeemanagement.application.port.inbound.skill.DeclareEmployeeSkillUseCase;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.DeclareSkillRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.EmployeeSkillResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/employees/me/skills")
public class EmployeeSkillController {

    private final DeclareEmployeeSkillUseCase declareEmployeeSkillUseCase;
    private final LoadEmployeePort loadEmployeePort;

    public EmployeeSkillController(
            DeclareEmployeeSkillUseCase declareEmployeeSkillUseCase,
            LoadEmployeePort loadEmployeePort
    ) {
        this.declareEmployeeSkillUseCase = declareEmployeeSkillUseCase;
        this.loadEmployeePort = loadEmployeePort;
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
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DeclareSkillRequest request
    ) {
        if (currentUser == null || currentUser.getIdValue() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn cần đăng nhập để thực hiện chức năng này"));
        }

        // Lấy hồ sơ nhân sự cá nhân dựa trên ID tài khoản đang đăng nhập
        Employee employee = loadEmployeePort.findByUserId(currentUser.getId())
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự của tài khoản đang đăng nhập"));

        DeclareEmployeeSkillCommand command = new DeclareEmployeeSkillCommand(
                employee.getIdValue(),
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


