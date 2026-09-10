package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult;
import com.hrm.employeemanagement.application.port.inbound.skill.GetDepartmentSkillMatrixUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.skill.dto.DepartmentSkillMatrixResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/skills/matrix")
@Validated
public class DepartmentSkillMatrixController {

    private final GetDepartmentSkillMatrixUseCase getDepartmentSkillMatrixUseCase;

    public DepartmentSkillMatrixController(GetDepartmentSkillMatrixUseCase getDepartmentSkillMatrixUseCase) {
        this.getDepartmentSkillMatrixUseCase = getDepartmentSkillMatrixUseCase;
    }

    /**
     * API xem ma trận kỹ năng của bộ phận (NCL-02-CN-007).
     * Hỗ trợ gọi qua query param: GET /api/v1/skills/matrix?orgUnitId={id}
     * Hỗ trợ các vai trò quản lý (VT-01, VT-02, VT-03, VT-05, VT-06 hoặc quyền EMPLOYEE_SKILL_READ).
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('VT-01', 'VT-02', 'VT-03', 'VT-05', 'VT-06', 'ROLE_VT-01', 'ROLE_VT-02', 'ROLE_VT-03', 'ROLE_VT-05', 'ROLE_VT-06', 'EMPLOYEE_SKILL_READ', 'SKILL_READ')")
    public ResponseEntity<ApiResponse<DepartmentSkillMatrixResponse>> getDepartmentSkillMatrix(
            @RequestParam @NotNull(message = "ID bộ phận (orgUnitId) không được để trống")
            @Positive(message = "ID bộ phận phải là số dương") Long orgUnitId
    ) {
        DepartmentSkillMatrixResult result = getDepartmentSkillMatrixUseCase.execute(orgUnitId);
        DepartmentSkillMatrixResponse response = DepartmentSkillMatrixResponse.fromResult(result);
        return ResponseEntity.ok(ApiResponse.success("Lấy ma trận kỹ năng bộ phận thành công", response));
    }

    /**
     * Hỗ trợ gọi qua path variable: GET /api/v1/skills/matrix/{orgUnitId}
     */
    @GetMapping("/{orgUnitId}")
    @PreAuthorize("hasAnyAuthority('VT-01', 'VT-02', 'VT-03', 'VT-05', 'VT-06', 'ROLE_VT-01', 'ROLE_VT-02', 'ROLE_VT-03', 'ROLE_VT-05', 'ROLE_VT-06', 'EMPLOYEE_SKILL_READ', 'SKILL_READ')")
    public ResponseEntity<ApiResponse<DepartmentSkillMatrixResponse>> getDepartmentSkillMatrixByPath(
            @PathVariable @NotNull(message = "ID bộ phận không được để trống")
            @Positive(message = "ID bộ phận phải là số dương") Long orgUnitId
    ) {
        DepartmentSkillMatrixResult result = getDepartmentSkillMatrixUseCase.execute(orgUnitId);
        DepartmentSkillMatrixResponse response = DepartmentSkillMatrixResponse.fromResult(result);
        return ResponseEntity.ok(ApiResponse.success("Lấy ma trận kỹ năng bộ phận thành công", response));
    }
}