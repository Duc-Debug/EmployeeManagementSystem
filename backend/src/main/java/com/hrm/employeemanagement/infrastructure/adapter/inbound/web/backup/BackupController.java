package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.backup;

import com.hrm.employeemanagement.application.dto.backup.*;
import com.hrm.employeemanagement.application.port.inbound.*;
import com.hrm.employeemanagement.application.service.BackupService;
import com.hrm.employeemanagement.domain.backup.Backup;
import com.hrm.employeemanagement.domain.backup.BackupStatus;
import com.hrm.employeemanagement.domain.backup.BackupType;
import com.hrm.employeemanagement.domain.backup.exception.BackupAccessDeniedException;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import com.hrm.employeemanagement.infrastructure.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/backups")
@Validated
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    private static final java.util.regex.Pattern IPV4_PATTERN = java.util.regex.Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
    private static final java.util.regex.Pattern IPV6_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F:]+$");

    private static class CurrentUserInfo {
        Long id;
        String email;
        boolean isAdmin;
    }

    private CurrentUserInfo checkPermissionAndGetUserInfo(HttpServletRequest request, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CurrentUserInfo info = new CurrentUserInfo();
        String clientIp = resolveClientIp(request);

        if (auth == null || !auth.isAuthenticated()) {
            backupService.recordAccessDenied(null, "anonymous", action, "Người dùng chưa xác thực", clientIp);
            throw new BackupAccessDeniedException("Bạn cần đăng nhập với quyền Quản trị viên để thực hiện chức năng này.");
        }

        Object principal = auth.getPrincipal();
        boolean isAdminRole = false;
        boolean hasBackupPermission = hasAuthority(auth, "DATA_BACKUP_MANAGE");

        if (principal instanceof User user) {
            info.id = user.getIdValue();
            info.email = user.getEmail();
            String roleCode = user.getRole() != null && user.getRole().getCode() != null ? user.getRole().getCode().getCode() : "";
            isAdminRole = "VT-06".equalsIgnoreCase(roleCode)
                    || "ROLE_ADMIN".equalsIgnoreCase(roleCode)
                    || "ADMIN".equalsIgnoreCase(roleCode);
        } else if (principal instanceof UserPrincipal up) {
            info.id = up.getId();
            info.email = up.getUsername();
            String roleCode = up.getDomainUser() != null && up.getDomainUser().getRole() != null && up.getDomainUser().getRole().getCode() != null
                    ? up.getDomainUser().getRole().getCode().getCode() : "";
            isAdminRole = "VT-06".equalsIgnoreCase(roleCode)
                    || "ROLE_ADMIN".equalsIgnoreCase(roleCode)
                    || "ADMIN".equalsIgnoreCase(roleCode)
                    || hasAuthority(auth, "VT-06")
                    || hasAuthority(auth, "ROLE_ADMIN")
                    || hasAuthority(auth, "ADMIN");
        } else {
            info.email = auth.getName();
            isAdminRole = hasAuthority(auth, "VT-06")
                    || hasAuthority(auth, "ROLE_ADMIN")
                    || hasAuthority(auth, "ADMIN");
        }

        info.isAdmin = isAdminRole && hasBackupPermission;

        if (!info.isAdmin) {
            backupService.recordAccessDenied(info.id, info.email, action, "Người dùng không có quyền quản lý sao lưu (Yêu cầu vai trò Quản trị viên VT-06 và quyền DATA_BACKUP_MANAGE)", clientIp);
            throw new BackupAccessDeniedException("Truy cập bị từ chối: Chỉ Quản trị viên (VT-06) có quyền DATA_BACKUP_MANAGE mới có quyền truy cập module sao lưu và phục hồi dữ liệu.");
        }

        return info;
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        if (auth == null || auth.getAuthorities() == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga.getAuthority() != null && (ga.getAuthority().equalsIgnoreCase(authority) || ga.getAuthority().equalsIgnoreCase("ROLE_" + authority))) {
                return true;
            }
        }
        return false;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && isValidIpAddress(remoteAddr)) ? remoteAddr : "127.0.0.1";
    }

    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches() || "localhost".equalsIgnoreCase(ip);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BackupResponse>>> listBackups(
            @RequestParam(required = false) BackupType type,
            @RequestParam(required = false) BackupStatus status,
            @RequestParam(required = false) String search,
            HttpServletRequest request
    ) {
        checkPermissionAndGetUserInfo(request, "LIST_BACKUPS");
        List<BackupResponse> responses = backupService.getBackups(type, status, search).stream()
                .map(BackupResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bản sao lưu thành công", responses));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BackupSummaryResponse>> getSummary(HttpServletRequest request) {
        checkPermissionAndGetUserInfo(request, "GET_SUMMARY");
        List<Backup> allBackups = backupService.getBackups(null, null, null);
        long totalCount = allBackups.size();
        long totalBytes = allBackups.stream().mapToLong(Backup::getFileSizeBytes).sum();
        Backup latestCompleted = allBackups.stream()
                .filter(b -> b.getStatus() == BackupStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        BackupSummaryResponse summary = new BackupSummaryResponse(
                totalCount,
                totalBytes,
                BackupResponse.fromDomain(latestCompleted),
                BackupScheduleResponse.fromDomain(backupService.getSchedule())
        );

        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê sao lưu thành công", summary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BackupResponse>> getBackupById(@PathVariable Long id, HttpServletRequest request) {
        checkPermissionAndGetUserInfo(request, "GET_BACKUP_DETAIL");
        Backup backup = backupService.getBackupById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết bản sao lưu thành công", BackupResponse.fromDomain(backup)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BackupResponse>> createBackup(
            @Valid @RequestBody CreateBackupRequest body,
            HttpServletRequest request
    ) {
        CurrentUserInfo user = checkPermissionAndGetUserInfo(request, "CREATE_BACKUP");
        String ip = resolveClientIp(request);
        Backup created = backupService.createBackup(body, user.id, user.email, ip);
        return ResponseEntity.ok(ApiResponse.success("Tạo bản sao lưu thành công", BackupResponse.fromDomain(created)));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreBackup(
            @PathVariable Long id,
            @Valid @RequestBody RestoreBackupRequest body,
            HttpServletRequest request
    ) {
        CurrentUserInfo user = checkPermissionAndGetUserInfo(request, "RESTORE_BACKUP");
        String ip = resolveClientIp(request);
        backupService.restoreBackup(id, body, user.id, user.email, ip);
        return ResponseEntity.ok(ApiResponse.success("Phục hồi dữ liệu hệ thống thành công về thời điểm bản sao lưu", null));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadBackup(@PathVariable Long id, HttpServletRequest request) {
        CurrentUserInfo user = checkPermissionAndGetUserInfo(request, "DOWNLOAD_BACKUP");
        String ip = resolveClientIp(request);
        Backup backup = backupService.getBackupForDownload(id);
        InputStream inputStream = backupService.downloadBackup(id, user.id, user.email, ip);

        InputStreamResource resource = new InputStreamResource(inputStream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backup.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpServletRequest request
    ) {
        CurrentUserInfo user = checkPermissionAndGetUserInfo(request, "DELETE_BACKUP");
        String ip = resolveClientIp(request);
        backupService.deleteBackup(id, reason, user.id, user.email, ip);
        return ResponseEntity.ok(ApiResponse.success("Xóa bản sao lưu thành công", null));
    }

    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<BackupScheduleResponse>> getSchedule(HttpServletRequest request) {
        checkPermissionAndGetUserInfo(request, "GET_SCHEDULE");
        BackupScheduleResponse response = BackupScheduleResponse.fromDomain(backupService.getSchedule());
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình lịch sao lưu thành công", response));
    }

    @PutMapping("/schedule")
    public ResponseEntity<ApiResponse<BackupScheduleResponse>> updateSchedule(
            @Valid @RequestBody UpdateBackupScheduleRequest body,
            HttpServletRequest request
    ) {
        CurrentUserInfo user = checkPermissionAndGetUserInfo(request, "UPDATE_SCHEDULE");
        String ip = resolveClientIp(request);
        var updated = backupService.updateSchedule(body, user.id, user.email, ip);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình lịch sao lưu thành công", BackupScheduleResponse.fromDomain(updated)));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<BackupAuditLogResponse>>> getAuditLogs(HttpServletRequest request) {
        checkPermissionAndGetUserInfo(request, "GET_AUDIT_LOGS");
        List<BackupAuditLogResponse> logs = backupService.getAuditLogs().stream()
                .map(BackupAuditLogResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy nhật ký thao tác sao lưu thành công", logs));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BackupResponse>> uploadBackup(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            HttpServletRequest request
    ) {
        CurrentUserInfo user = checkPermissionAndGetUserInfo(request, "UPLOAD_BACKUP");
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tệp tải lên không có nội dung"));
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".json")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Hệ thống chỉ chấp nhận tệp sao lưu định dạng JSON Snapshot (.json)"));
        }
        String ip = resolveClientIp(request);
        try (InputStream is = file.getInputStream()) {
            Backup uploaded = backupService.uploadBackup(
                    originalFilename,
                    title,
                    description,
                    is,
                    file.getSize(),
                    user.id,
                    user.email,
                    ip
            );
            return ResponseEntity.ok(ApiResponse.success("Tải lên bản sao lưu thành công", BackupResponse.fromDomain(uploaded)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tải lên tệp: " + e.getMessage(), e);
        }
    }
}
