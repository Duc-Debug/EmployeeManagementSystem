package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.inbound.user.CreateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetUserListUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ToggleUserStatusUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.UpdateUserRoleUseCase;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.exception.user.UserAlreadyLockedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.CreateUserRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.UpdateUserRoleRequest;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Mock
    private CreateUserUseCase createUserUseCase;

    @Mock
    private ToggleUserStatusUseCase toggleUserStatusUseCase;

    @Mock
    private UpdateUserRoleUseCase updateUserRoleUseCase;

    @Mock
    private GetUserListUseCase getUserListUseCase;

    @BeforeEach
    void setUp() {
        UserController userController =
                new UserController(
                        createUserUseCase,
                        toggleUserStatusUseCase,
                        updateUserRoleUseCase,
                        getUserListUseCase
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(
                        new UserExceptionHandler()
                )
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/users trả về 201 Created kèm Location Header khi tạo thành công")
    void testCreateUser_Returns201CreatedAndLocationHeader()
            throws Exception {

        CreateUserRequest request =
                new CreateUserRequest();

        request.setUsername("newuser");
        request.setPassword("password123");
        request.setRoleCode("VT-04");
        request.setEmployeeCode("EMP-099");
        request.setFullName("New User");
        request.setOrgUnitId(5L);

        UserResult mockResult =
                new UserResult(
                        99L,
                        "newuser",
                        "VT-04",
                        "Nhân viên chuyên môn",
                        UserStatus.ACTIVE,
                        10L,
                        "New User",
                        5L,
                        "OrgUnit 5"
                );

        when(
                createUserUseCase.createUser(
                        any(CreateUserCommand.class)
                )
        ).thenReturn(mockResult);

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        header().string(
                                "Location",
                                containsString(
                                        "/api/v1/users/99"
                                )
                        )
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.id")
                                .value(99)
                )
                .andExpect(
                        jsonPath("$.data.username")
                                .value("newuser")
                )
                .andExpect(
                        jsonPath("$.data.orgUnitId")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.data.orgUnitName")
                                .value("OrgUnit 5")
                );
    }

    @Test
    @DisplayName("POST /api/v1/users trả về 409 Conflict khi username đã tồn tại")
    void testCreateUser_DuplicateUsername_Returns409()
            throws Exception {

        CreateUserRequest request =
                new CreateUserRequest();

        request.setUsername("existing");
        request.setPassword("password123");
        request.setRoleCode("VT-04");
        request.setEmployeeCode("EMP-099");
        request.setFullName("Existing User");
        request.setOrgUnitId(5L);

        when(
                createUserUseCase.createUser(
                        any(CreateUserCommand.class)
                )
        ).thenThrow(
                new DuplicateUsernameException(
                        "Tên đăng nhập đã tồn tại: existing"
                )
        );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        containsString(
                                                "đã tồn tại"
                                        )
                                )
                );
    }

    @Test
    @DisplayName("GET /api/v1/users trả về 200 OK kèm thông tin phân trang PageResult")
    void testGetUsers_ReturnsPageResult()
            throws Exception {

        UserResult u1 =
                new UserResult(
                        1L,
                        "u1",
                        "VT-04",
                        "Staff",
                        UserStatus.ACTIVE,
                        10L,
                        "User 1",
                        5L,
                        "OrgUnit"
                );

        PageResult<UserResult> mockPage =
                new PageResult<>(
                        List.of(u1),
                        0,
                        20,
                        1L
                );

        when(
                getUserListUseCase.getUsers(
                        0,
                        20
                )
        ).thenReturn(mockPage);

        mockMvc.perform(
                        get(
                                "/api/v1/users?page=0&size=20"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$.data.content[0].username"
                        ).value("u1")
                )
                .andExpect(
                        jsonPath(
                                "$.data.totalElements"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.data.totalPages"
                        ).value(1)
                );
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status trả về 400 Bad Request khi Admin tự khóa chính mình")
    void testToggleUserStatus_SelfLocking_Returns400()
            throws Exception {

        when(
                toggleUserStatusUseCase.toggleUserStatus(
                        eq(1L),
                        eq(true)
                )
        ).thenThrow(
                new SelfLockingException(
                        "Bạn không thể tự khóa tài khoản của chính mình"
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/users/1/status?lock=true"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        containsString(
                                                "tự khóa"
                                        )
                                )
                );
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status trả về 400 Bad Request khi khóa tài khoản đã bị khóa")
    void testToggleUserStatus_AlreadyLocked_Returns400()
            throws Exception {

        when(
                toggleUserStatusUseCase.toggleUserStatus(
                        eq(2L),
                        eq(true)
                )
        ).thenThrow(
                new UserAlreadyLockedException(
                        "Tài khoản này hiện đã bị khóa"
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/v1/users/2/status?lock=true"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        containsString(
                                                "hiện đã bị khóa"
                                        )
                                )
                );
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/role trả về 200 OK khi cập nhật thành công")
    void testUpdateUserRole_Success()
            throws Exception {

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest();

        request.setRoleCode("VT-02");
        request.setOrgUnitId(10L);

        UserResult mockResult =
                new UserResult(
                        2L,
                        "user2",
                        "VT-02",
                        "Quản lý dự án",
                        UserStatus.ACTIVE,
                        20L,
                        "User Two",
                        10L,
                        "OrgUnit 10"
                );

        when(
                updateUserRoleUseCase.updateUserRole(
                        any(UpdateUserRoleCommand.class)
                )
        ).thenReturn(mockResult);

        mockMvc.perform(
                        put("/api/v1/users/2/role")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.data.roleCode")
                                .value("VT-02")
                )
                .andExpect(
                        jsonPath("$.data.orgUnitId")
                                .value(10)
                );
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} trả về 404 Not Found khi User không tồn tại")
    void testGetUserById_NotFound_Returns404()
            throws Exception {

        when(
                getUserListUseCase.getUserById(
                        999L
                )
        ).thenThrow(
                new UserNotFoundException(
                        "Không tìm thấy người dùng với ID: 999"
                )
        );

        mockMvc.perform(
                        get("/api/v1/users/999")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.success")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        containsString(
                                                "Không tìm thấy"
                                        )
                                )
                );
    }
}