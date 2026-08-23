package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.inbound.user.CreateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetUserListUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ToggleUserStatusUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.UpdateUserRoleUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.CreateUserRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.UpdateUserRoleRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAuthority('VT-06')")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final ToggleUserStatusUseCase toggleUserStatusUseCase;
    private final UpdateUserRoleUseCase updateUserRoleUseCase;
    private final GetUserListUseCase getUserListUseCase;

    public UserController(CreateUserUseCase createUserUseCase,
                          ToggleUserStatusUseCase toggleUserStatusUseCase,
                          UpdateUserRoleUseCase updateUserRoleUseCase,
                          GetUserListUseCase getUserListUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.toggleUserStatusUseCase = toggleUserStatusUseCase;
        this.updateUserRoleUseCase = updateUserRoleUseCase;
        this.getUserListUseCase = getUserListUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResult>> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(
                request.getUsername(),
                request.getPassword(),
                request.getRoleCode(),
                request.getEmployeeCode(),
                request.getFullName(),
                request.getOrgUnitId()
        );

        UserResult result = createUserUseCase.createUser(command);

        // Standard RESTful 201 Created with Location Header RFC 7231
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success("Tạo tài khoản thành công", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<UserResult>>> getUsers(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        PageResult<UserResult> users = getUserListUseCase.getUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài khoản thành công", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResult>> getUserById(@PathVariable Long id) {
        UserResult user = getUserListUseCase.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", user));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResult>> updateUserRole(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateUserRoleRequest request) {
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(id, request.getRoleCode(), request.getOrgUnitId());
        UserResult result = updateUserRoleUseCase.updateUserRole(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò và đơn vị tổ chức thành công", result));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResult>> toggleUserStatus(@PathVariable Long id,
                                                                     @RequestParam boolean lock) {
        UserResult result = toggleUserStatusUseCase.toggleUserStatus(id, lock);
        String actionMsg = lock ? "Khóa tài khoản thành công" : "Mở lại tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(actionMsg, result));
    }
}
