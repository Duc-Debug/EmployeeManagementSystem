package com.hrm.employeemanagement.application.service.importdata;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


import com.hrm.employeemanagement.application.dto.importdata.ConfirmEmployeeImportCommand;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeePreviewResult;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeeRowDto;
import com.hrm.employeemanagement.application.dto.importdata.ImportExecutionResult;
import com.hrm.employeemanagement.application.port.inbound.importdata.ConfirmEmployeeImportUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.GenerateImportTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.PreviewEmployeeImportUseCase;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeDataFileParser;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeImportTemplateGenerator;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.importdata.DataImportException;
import com.hrm.employeemanagement.domain.importdata.RawEmployeeImportRow;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Ứng dụng nghiệp vụ chính (Application Service) cho tính năng Nhập dữ liệu nhân sự (NCL-12-CN-004).
 * Tuân thủ Clean Architecture: Hoàn toàn không phụ thuộc trực tiếp vào thư viện đọc tệp bên ngoài (Apache POI).
 * Tương tác với tầng hạ tầng thông qua Outbound Ports (EmployeeDataFileParser, EmployeeImportTemplateGenerator).
 */
public class EmployeeImportService implements PreviewEmployeeImportUseCase, ConfirmEmployeeImportUseCase, GenerateImportTemplateUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadRolePort loadRolePort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveEmployeePort saveEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final PasswordEncoderPort passwordEncoder;
    private final SaveAuditLogPort saveAuditLogPort;
    private final List<EmployeeDataFileParser> fileParsers;
    private final List<EmployeeImportTemplateGenerator> templateGenerators;

    private static final String DEFAULT_INITIAL_PASSWORD = "Password@123";

    public EmployeeImportService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            LoadRolePort loadRolePort,
            LoadEmployeePort loadEmployeePort,
            SaveEmployeePort saveEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            PasswordEncoderPort passwordEncoder,
            SaveAuditLogPort saveAuditLogPort,
            List<EmployeeDataFileParser> fileParsers,
            List<EmployeeImportTemplateGenerator> templateGenerators
    ) {
        this.authorizationService = authorizationService;
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.loadRolePort = loadRolePort;
        this.loadEmployeePort = loadEmployeePort;
        this.saveEmployeePort = saveEmployeePort;
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.passwordEncoder = passwordEncoder;
        this.saveAuditLogPort = saveAuditLogPort;
        this.fileParsers = fileParsers != null ? fileParsers : List.of();
        this.templateGenerators = templateGenerators != null ? templateGenerators : List.of();
    }

    @Override
    public ImportEmployeePreviewResult preview(InputStream inputStream, String filename) {
        authorizationService.require(PermissionCode.DATA_IMPORT);

        EmployeeDataFileParser parser = fileParsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst()
                .orElseThrow(() -> new DataImportException(
                        "Định dạng tệp không được hỗ trợ: " + filename + ". Vui lòng tải lên tệp Excel (.xlsx, .xls)."
                ));

        List<RawEmployeeImportRow> rawRows = parser.parse(inputStream);
        return validateAndBuildPreview(rawRows);
    }

    private ImportEmployeePreviewResult validateAndBuildPreview(List<RawEmployeeImportRow> rawRows) {
        List<OrgUnit> allOrgUnits = loadOrgUnitPort.findAllActive();
        Map<String, OrgUnit> orgUnitLookup = new HashMap<>();
        for (OrgUnit u : allOrgUnits) {
            orgUnitLookup.put(u.getUnitName().trim().toLowerCase(), u);
            if (u.getUnitCode() != null) {
                orgUnitLookup.put(u.getUnitCode().trim().toLowerCase(), u);
            }
            if (u.getId() != null) {
                orgUnitLookup.put(String.valueOf(u.getId().getValue()), u);
            }
        }

        Set<String> seenEmployeeCodes = new HashSet<>();
        Set<String> seenUsernames = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();

        List<ImportEmployeeRowDto> validatedRows = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        for (RawEmployeeImportRow raw : rawRows) {
            List<String> errors = new ArrayList<>();

            // 1. Mã nhân viên
            String employeeCode = cleanString(raw.employeeCode());
            if (employeeCode == null || employeeCode.isBlank()) {
                errors.add("Mã nhân viên không được để trống");
            } else {
                String codeKey = employeeCode.toLowerCase();
                if (seenEmployeeCodes.contains(codeKey)) {
                    errors.add("Mã nhân viên '" + employeeCode + "' bị trùng lặp trong tệp");
                } else if (loadEmployeePort.existsByEmployeeCode(employeeCode)) {
                    errors.add("Mã nhân viên '" + employeeCode + "' đã tồn tại trong hệ thống");
                } else {
                    seenEmployeeCodes.add(codeKey);
                }
            }

            // 2. Họ và tên
            String fullName = cleanString(raw.fullName());
            if (fullName == null || fullName.isBlank()) {
                errors.add("Họ và tên không được để trống");
            } else if (fullName.length() < 2 || fullName.length() > 100) {
                errors.add("Họ và tên phải từ 2 đến 100 ký tự");
            }

            // 3. Tên đăng nhập
            String username = cleanString(raw.username());
            String email = cleanString(raw.email());

            if (username == null || username.isBlank()) {
                if (email != null && !email.isBlank()) {
                    username = email;
                } else {
                    errors.add("Tên đăng nhập không được để trống");
                }
            }

            if (username != null && !username.isBlank()) {
                String userKey = username.toLowerCase();
                if (seenUsernames.contains(userKey)) {
                    errors.add("Tên đăng nhập '" + username + "' bị trùng lặp trong tệp");
                } else if (loadUserPort.existsByUsername(username)) {
                    errors.add("Tên đăng nhập '" + username + "' đã tồn tại trong hệ thống");
                } else if (loadUserPort.existsByEmail(username)) {
                    errors.add("Tên đăng nhập '" + username + "' xung đột với email của tài khoản khác");
                } else {
                    seenUsernames.add(userKey);
                }
            }

            // 4. Email
            if (email != null && !email.isBlank()) {
                String normalizedEmail = email.toLowerCase();
                if (!normalizedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    errors.add("Email '" + email + "' không đúng định dạng chuẩn");
                } else if (seenEmails.contains(normalizedEmail)) {
                    errors.add("Email '" + email + "' bị trùng lặp trong tệp");
                } else if (loadUserPort.existsByEmail(normalizedEmail)) {
                    errors.add("Email '" + email + "' đã tồn tại trong hệ thống");
                } else if (loadUserPort.existsByUsername(normalizedEmail)) {
                    errors.add("Email '" + email + "' xung đột với tên đăng nhập của tài khoản khác");
                } else {
                    seenEmails.add(normalizedEmail);
                }
            }

            // 5. Phòng ban
            String orgUnitIdentifier = cleanString(raw.orgUnitIdentifier());
            Long resolvedOrgUnitId = null;
            String resolvedOrgUnitName = null;

            if (orgUnitIdentifier == null || orgUnitIdentifier.isBlank()) {
                errors.add("Phòng ban / Đơn vị không được để trống");
            } else {
                OrgUnit matchedUnit = orgUnitLookup.get(orgUnitIdentifier.trim().toLowerCase());
                if (matchedUnit == null) {
                    errors.add("Phòng ban / Đơn vị '" + orgUnitIdentifier + "' không tồn tại trong hệ thống");
                } else {
                    resolvedOrgUnitId = matchedUnit.getId().getValue();
                    resolvedOrgUnitName = matchedUnit.getUnitName();
                }
            }

            // 6. Vai trò hệ thống
            String roleCode = cleanString(raw.roleCode());
            if (roleCode == null || roleCode.isBlank()) {
                roleCode = "VT-04";
            } else {
                try {
                    roleCode = RoleCode.fromCode(roleCode).getCode();
                } catch (Exception e) {
                    errors.add("Mã vai trò '" + roleCode + "' không hợp lệ (hỗ trợ VT-01 đến VT-06)");
                }
            }

            // 7. Chức danh chuyên môn
            String professionalRole = cleanString(raw.professionalRole());

            // 8. Giờ chuẩn / Tuần
            Integer standardHours = raw.standardHoursPerWeek();
            if (standardHours == null) {
                standardHours = 40;
            } else if (standardHours <= 0 || standardHours > 168) {
                errors.add("Giờ làm việc chuẩn (" + standardHours + "h) phải lớn hơn 0 và không vượt quá 168h/tuần");
            }

            // 9. Ngày bắt đầu & Ngày kết thúc HĐ
            LocalDate startDate = raw.startDate();
            LocalDate contractEndDate = raw.contractEndDate();
            if (startDate != null && contractEndDate != null && contractEndDate.isBefore(startDate)) {
                errors.add("Ngày kết thúc hợp đồng (" + contractEndDate + ") không được trước ngày bắt đầu (" + startDate + ")");
            }

            boolean isOutsourced = Boolean.TRUE.equals(raw.isOutsourced());
            boolean isValid = errors.isEmpty();

            if (isValid) {
                validCount++;
            } else {
                invalidCount++;
            }

            validatedRows.add(new ImportEmployeeRowDto(
                    raw.rowNumber(),
                    employeeCode,
                    fullName,
                    username,
                    email,
                    orgUnitIdentifier,
                    resolvedOrgUnitId,
                    resolvedOrgUnitName,
                    roleCode,
                    professionalRole,
                    standardHours,
                    startDate,
                    contractEndDate,
                    isOutsourced,
                    isValid,
                    errors
            ));
        }

        String message = validCount > 0
                ? "Kiểm tra thành công: " + validCount + " dòng hợp lệ, " + invalidCount + " dòng lỗi"
                : "Tất cả " + invalidCount + " dòng trong tệp đều có lỗi, vui lòng kiểm tra và sửa lại";

        return new ImportEmployeePreviewResult(
                validatedRows.size(),
                validCount,
                invalidCount,
                validatedRows,
                validCount > 0,
                message
        );
    }

    @Override
    public ImportExecutionResult confirm(ConfirmEmployeeImportCommand command) {
        Long currentAdminId = authorizationService.require(PermissionCode.DATA_IMPORT);

        if (command == null || command.rows() == null || command.rows().isEmpty()) {
            return new ImportExecutionResult(0, 0, List.of(), "Không có dòng dữ liệu hợp lệ nào để nhập", LocalDateTime.now());
        }

        List<ImportEmployeeRowDto> targetRows = command.rows().stream()
                .filter(ImportEmployeeRowDto::valid)
                .toList();

        if (targetRows.isEmpty()) {
            return new ImportExecutionResult(0, command.rows().size(), List.of(), "Không có dòng dữ liệu nào đạt trạng thái hợp lệ", LocalDateTime.now());
        }

        int importedCount = 0;
        int skippedCount = 0;
        List<String> executionErrors = new ArrayList<>();
        String encodedDefaultPassword = passwordEncoder.encode(DEFAULT_INITIAL_PASSWORD);

        for (ImportEmployeeRowDto row : targetRows) {
            try {
                if (loadUserPort.existsByUsername(row.username()) || loadEmployeePort.existsByEmployeeCode(row.employeeCode())) {
                    skippedCount++;
                    executionErrors.add("Dòng " + row.rowNumber() + " (" + row.employeeCode() + "): Tài khoản hoặc mã NV đã tồn tại.");
                    continue;
                }

                RoleCode roleCode = RoleCode.fromCode(row.roleCode() != null ? row.roleCode() : "VT-04");
                Role role = loadRolePort.findByCode(roleCode)
                        .orElseThrow(() -> new DataImportException("Không tìm thấy vai trò hệ thống: " + roleCode));

                Long scopeOrgUnitId = (roleCode == RoleCode.VT_03) ? row.resolvedOrgUnitId() : null;
                User newUser = User.createNew(
                        row.username(),
                        encodedDefaultPassword,
                        role,
                        null,
                        row.email(),
                        scopeOrgUnitId
                );
                User savedUser = saveUserPort.save(newUser);

                Employee employee = new Employee(
                        null,
                        savedUser.getId(),
                        row.resolvedOrgUnitId(),
                        row.employeeCode(),
                        row.fullName(),
                        row.professionalRole(),
                        row.startDate(),
                        row.contractEndDate(),
                        row.isOutsourced(),
                        row.standardHoursPerWeek() != null ? row.standardHoursPerWeek() : 40,
                        EmployeeStatus.ACTIVE
                );
                Employee savedEmployee = saveEmployeePort.save(employee);

                savedUser.linkEmployee(savedEmployee.getId());
                saveUserPort.save(savedUser);

                importedCount++;
            } catch (Exception e) {
                skippedCount++;
                executionErrors.add("Dòng " + row.rowNumber() + " (" + row.employeeCode() + "): " + e.getMessage());
            }
        }

        // Ghi Audit Log
        if (saveAuditLogPort != null) {
            try {
                saveAuditLogPort.save(AuditLog.createChange(
                        currentAdminId,
                        "DATA_IMPORT_EMPLOYEES",
                        "employees",
                        null,
                        null,
                        "Nhập dữ liệu nhân sự từ tệp: Thành công " + importedCount + " hồ sơ, Bỏ qua " + skippedCount + " dòng"
                ));
            } catch (Exception ignored) {
            }
        }

        String msg = "Đã nhập thành công " + importedCount + " hồ sơ nhân sự vào hệ thống"
                + (skippedCount > 0 ? " (bỏ qua " + skippedCount + " dòng lỗi)" : "");

        return new ImportExecutionResult(
                importedCount,
                skippedCount,
                executionErrors,
                msg,
                LocalDateTime.now()
        );
    }

    @Override
    public byte[] generateEmployeeTemplate(String format) {
        EmployeeImportTemplateGenerator generator = templateGenerators.stream()
                .filter(g -> g.supports(format))
                .findFirst()
                .orElseThrow(() -> new DataImportException("Không tìm thấy bộ tạo biểu mẫu cho định dạng: " + format));

        return generator.generateTemplate();
    }

    private String cleanString(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
