package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.backup.CreateBackupRequest;
import com.hrm.employeemanagement.application.dto.backup.RestoreBackupRequest;
import com.hrm.employeemanagement.application.dto.backup.UpdateBackupScheduleRequest;
import com.hrm.employeemanagement.application.service.BackupService;
import com.hrm.employeemanagement.domain.backup.Backup;
import com.hrm.employeemanagement.domain.backup.BackupSchedule;
import com.hrm.employeemanagement.domain.backup.BackupStatus;
import com.hrm.employeemanagement.domain.backup.BackupType;
import com.hrm.employeemanagement.domain.backup.exception.InvalidBackupStatusException;
import com.hrm.employeemanagement.domain.backup.exception.InvalidRestoreConfirmationException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackupController Tests (NCL-12-CN-003)")
class BackupControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BackupService backupService;

    @InjectMocks
    private BackupController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new BackupExceptionHandler())
                .build();
    }

    private void setSecurityContext(String roleCode, Long userId, String email) {
        Role role = new Role(new RoleId(1L), RoleCode.fromCode(roleCode), roleCode);
        User user = new User(
                new UserId(userId),
                "testuser",
                "hashed",
                role,
                UserStatus.ACTIVE,
                null,
                email,
                null,
                1L
        );
        UserPrincipal principal = new UserPrincipal(user, Collections.singletonList(new SimpleGrantedAuthority(roleCode)));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Admin (VT-06) lấy danh sách bản sao lưu thành công (HTTP 200)")
    void testListBackups_Admin_Success() throws Exception {
        setSecurityContext("VT-06", 1L, "admin@company.com");

        Backup backup = new Backup(
                1L, "BCK-001", "Bản sao lưu 1", "Mô tả", BackupType.FULL,
                "bck001.json", "path", 1024L, "chk1", BackupStatus.COMPLETED,
                false, 1L, "admin", LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(backupService.getBackups(any(), any(), any())).thenReturn(List.of(backup));

        mockMvc.perform(get("/api/v1/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].backupCode").value("BCK-001"))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Người dùng không phải Admin (VT-04) bị từ chối truy cập (HTTP 403) và ghi log")
    void testListBackups_NonAdmin_Forbidden() throws Exception {
        setSecurityContext("VT-04", 2L, "employee@company.com");

        mockMvc.perform(get("/api/v1/backups"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Tạo bản sao lưu thành công (HTTP 200)")
    void testCreateBackup_Admin_Success() throws Exception {
        setSecurityContext("VT-06", 1L, "admin@company.com");

        CreateBackupRequest request = new CreateBackupRequest("Sao lưu mới", "Ghi chú", BackupType.FULL);
        Backup created = new Backup(
                2L, "BCK-002", "Sao lưu mới", "Ghi chú", BackupType.FULL,
                "bck002.json", "path", 2048L, "chk2", BackupStatus.COMPLETED,
                false, 1L, "admin", LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(backupService.createBackup(any(), eq(1L), eq("testuser"), anyString())).thenReturn(created);

        mockMvc.perform(post("/api/v1/backups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.backupCode").value("BCK-002"));
    }

    @Test
    @DisplayName("Phục hồi bản sao lưu 2 bước thành công (HTTP 200)")
    void testRestoreBackup_Admin_Success() throws Exception {
        setSecurityContext("VT-06", 1L, "admin@company.com");

        RestoreBackupRequest request = new RestoreBackupRequest("RESTORE", "Phục hồi kế hoạch dự án tuần 38");

        mockMvc.perform(post("/api/v1/backups/1/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Phục hồi thất bại khi mã xác nhận sai (HTTP 400)")
    void testRestoreBackup_InvalidConfirmation_BadRequest() throws Exception {
        setSecurityContext("VT-06", 1L, "admin@company.com");

        RestoreBackupRequest request = new RestoreBackupRequest("WRONG", "Phục hồi kế hoạch dự án tuần 38");
        doThrow(new InvalidRestoreConfirmationException("Mã xác nhận không đúng"))
                .when(backupService).restoreBackup(eq(1L), any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/backups/1/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CONFIRMATION"));
    }

    @Test
    @DisplayName("Cập nhật lịch sao lưu tự động thành công (HTTP 200)")
    void testUpdateSchedule_Admin_Success() throws Exception {
        setSecurityContext("VT-06", 1L, "admin@company.com");

        UpdateBackupScheduleRequest request = new UpdateBackupScheduleRequest();
        request.setEnabled(true);
        request.setScheduledTime("02:00");

        BackupSchedule schedule = BackupSchedule.createDefault();
        schedule.setEnabled(true);

        when(backupService.updateSchedule(any(), any(), any(), any())).thenReturn(schedule);

        mockMvc.perform(put("/api/v1/backups/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @DisplayName("Người dùng có quyền DATA_BACKUP_MANAGE truy cập thành công (HTTP 200)")
    void testListBackups_Permission_Success() throws Exception {
        Role role = new Role(new RoleId(1L), RoleCode.fromCode("VT-01"), "VT-01");
        User user = new User(new UserId(5L), "user_with_perm", "hash", role, UserStatus.ACTIVE, null, "user@hrm.com", null, 1L);
        UserPrincipal principal = new UserPrincipal(user, List.of(new SimpleGrantedAuthority("DATA_BACKUP_MANAGE")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(backupService.getBackups(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Tải lên file sao lưu .json thành công (HTTP 200)")
    void testUploadBackup_Json_Success() throws Exception {
        setSecurityContext("VT-06", 1L, "admin@company.com");

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "backup.json", "application/json", "{\"tables\": {}}".getBytes()
        );

        Backup uploaded = new Backup(
                3L, "BCK-UPLOAD-1", "Upload", "Desc", BackupType.FULL,
                "BCK-UPLOAD-1.json", "path", 100L, "chk", BackupStatus.COMPLETED,
                false, 1L, "admin", LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(backupService.uploadBackup(anyString(), any(), any(), any(), any(), anyLong(), any(), any(), any()))
                .thenReturn(uploaded);

        mockMvc.perform(multipart("/api/v1/backups/upload")
                        .file(file)
                        .param("title", "My Upload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Tải lên file không phải .json bị từ chối (HTTP 400)")
    void testUploadBackup_Sql_BadRequest() throws Exception {
        setSecurityContext("VT-06", 1L, "admin@company.com");

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "dump.sql", "text/plain", "SELECT 1;".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/backups/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(".json")));
    }
}
