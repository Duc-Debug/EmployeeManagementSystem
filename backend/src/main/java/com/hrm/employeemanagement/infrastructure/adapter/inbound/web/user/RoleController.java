package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.port.inbound.user.GetRoleListUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.RoleResponse;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final GetRoleListUseCase getRoleListUseCase;

    public RoleController(GetRoleListUseCase getRoleListUseCase) {
        this.getRoleListUseCase = getRoleListUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        List<RoleResponse> roles = getRoleListUseCase.getRoles().stream()
                .map(RoleResponse::fromResult)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách vai trò thành công", roles));
    }
}
