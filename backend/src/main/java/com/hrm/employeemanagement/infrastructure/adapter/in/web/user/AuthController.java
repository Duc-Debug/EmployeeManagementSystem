package com.hrm.employeemanagement.infrastructure.adapter.in.web.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;
import com.hrm.employeemanagement.infrastructure.adapter.in.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.adapter.in.web.user.dto.LoginRequest;
import com.hrm.employeemanagement.port.in.user.AuthenticateUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResult>> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.getUsername(), request.getPassword());
        AuthTokenResult result = authenticateUserUseCase.login(command);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", result));
    }
}
