package com.hrm.employeemanagement.application.service.importdata;

import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hrm.employeemanagement.application.dto.importdata.ConfirmEmployeeImportCommand;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeePreviewResult;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeeRowDto;
import com.hrm.employeemanagement.application.dto.importdata.ImportExecutionResult;
import com.hrm.employeemanagement.application.port.inbound.importdata.ConfirmEmployeeImportUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.GenerateImportTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.PreviewEmployeeImportUseCase;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeDataFileParser;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeImportTemplateGenerator;
import com.hrm.employeemanagement.application.port.outbound.importdata.SingleRowEmployeeImportPort;
import com.hrm.employeemanagement.application.port.outbound.importdata.SingleRowImportResult;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.importdata.DataImportException;
import com.hrm.employeemanagement.domain.importdata.RawEmployeeImportRow;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;

/**
 * Ứng dụng nghiệp vụ chính (Application Service) cho tính năng Nhập dữ liệu nhân sự (NCL-12-CN-004).
 * Tuân thủ Clean Architecture: Phân tách rõ ràng giữa Validation logic và Transactional Execution.
 * Re-validate 100% dữ liệu tại confirm boundary, sử dụng mật khẩu ngẫu nhiên an toàn (SecureRandom),
 * và cô lập transaction theo từng dòng qua SingleRowEmployeeImportPort.
 */
public class EmployeeImportService implements PreviewEmployeeImportUseCase, ConfirmEmployeeImportUseCase, GenerateImportTemplateUseCase {

    private static final Logger log = LoggerFactory.getLogger(EmployeeImportService.class);

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    private static final Set<String> TRUE_BOOLEAN_VALUES = Set.of(
            "true", "1", "có", "co", "yes", "y", "thuê ngoài", "thue ngoai"
    );
    private static final Set<String> FALSE_BOOLEAN_VALUES = Set.of(
            "false", "0", "không", "khong", "no", "n", "nội bộ", "noi bo", "chính thức", "chinh thuc"
    );

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadRolePort loadRolePort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final PasswordEncoderPort passwordEncoder;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SingleRowEmployeeImportPort singleRowImportPort;
    private final List<EmployeeDataFileParser> fileParsers;
    private final List<EmployeeImportTemplateGenerator> templateGenerators;

    public EmployeeImportService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadRolePort loadRolePort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            PasswordEncoderPort passwordEncoder,
            SaveAuditLogPort saveAuditLogPort,
            SingleRowEmployeeImportPort singleRowImportPort,
            List<EmployeeDataFileParser> fileParsers,
            List<EmployeeImportTemplateGenerator> templateGenerators
    ) {
        this.authorizationService = authorizationService;
        this.loadUserPort = loadUserPort;
        this.loadRolePort = loadRolePort;
        this.loadEmployeePort = loadEmployeePort;
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.passwordEncoder = passwordEncoder;
        this.saveAuditLogPort = saveAuditLogPort;
        this.singleRowImportPort = singleRowImportPort;
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

    @Override
    public ImportExecutionResult confirm(ConfirmEmployeeImportCommand command) {
        Long currentAdminId = authorizationService.require(PermissionCode.DATA_IMPORT);

        if (command == null || command.rows() == null || command.rows().isEmpty()) {
            return new ImportExecutionResult(0, 0, List.of(), "Không có dòng dữ liệu nào để nhập", LocalDateTime.now());
        }

        // Chuyển đổi payload client thành RawEmployeeImportRow để thực hiện RE-VALIDATION 100% phía server
        List<RawEmployeeImportRow> rawRowsToRevalidate = command.rows().stream()
                .map(dto -> new RawEmployeeImportRow(
                        dto.rowNumber(),
                        dto.employeeCode(),
                        dto.fullName(),
                        dto.username(),
                        dto.email(),
                        dto.orgUnitIdentifier(),
                        dto.roleCode(),
                        dto.professionalRole(),
                        dto.standardHoursPerWeek() != null ? String.valueOf(dto.standardHoursPerWeek()) : null,
                        dto.startDate() != null ? dto.startDate().toString() : null,
                        dto.contractEndDate() != null ? dto.contractEndDate().toString() : null,
                        dto.isOutsourced() != null ? String.valueOf(dto.isOutsourced()) : null
                ))
                .toList();

        ImportEmployeePreviewResult revalidatedResult = validateAndBuildPreview(rawRowsToRevalidate);
        List<ImportEmployeeRowDto> revalidatedRows = revalidatedResult.rows();

        int importedCount = 0;
        int skippedCount = 0;
        List<String> executionErrors = new ArrayList<>();

        for (ImportEmployeeRowDto row : revalidatedRows) {
            // Không tin cậy cờ valid gửi từ frontend; chỉ chấp nhận kết quả re-validation từ server
            if (!row.valid()) {
                skippedCount++;
                executionErrors.add("Dòng " + row.rowNumber() + " (" + (row.employeeCode() != null ? row.employeeCode() : "N/A") + "): " + String.join("; ", row.errors()));
                continue;
            }

            try {
                RoleCode roleCode = RoleCode.fromCode(row.roleCode() != null ? row.roleCode() : "VT-04");
                Role role = loadRolePort.findByCode(roleCode)
                        .orElseThrow(() -> new DataImportException("Không tìm thấy vai trò hệ thống: " + roleCode));

                Long scopeOrgUnitId = (roleCode == RoleCode.VT_03) ? row.resolvedOrgUnitId() : null;

                // Sinh mật khẩu ngẫu nhiên tạm thời an toàn (SecureRandom) riêng cho từng tài khoản
                String temporaryPassword = generateSecureTemporaryPassword();
                String encodedPassword = passwordEncoder.encode(temporaryPassword);

                SingleRowImportResult result = singleRowImportPort.importSingleRow(
                        row.rowNumber(),
                        row.employeeCode(),
                        row.fullName(),
                        row.username(),
                        encodedPassword,
                        row.email(),
                        row.resolvedOrgUnitId(),
                        role,
                        scopeOrgUnitId,
                        row.professionalRole(),
                        row.standardHoursPerWeek() != null ? row.standardHoursPerWeek() : 40,
                        row.startDate(),
                        row.contractEndDate(),
                        Boolean.TRUE.equals(row.isOutsourced())
                );

                if (result.success()) {
                    importedCount++;
                } else {
                    skippedCount++;
                    executionErrors.add(result.errorMessage());
                }
            } catch (Exception e) {
                skippedCount++;
                log.error("Lỗi nội bộ khi xử lý nhập dòng {} ({})", row.rowNumber(), row.employeeCode(), e);
                executionErrors.add("Dòng " + row.rowNumber() + " (" + (row.employeeCode() != null ? row.employeeCode() : "N/A") + "): Không thể xử lý bản ghi do lỗi hệ thống.");
            }
        }

        // Ghi Audit Log nghiệp vụ
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
            } catch (Exception ex) {
                log.warn("Không thể lưu audit log cho phiên nhập nhân sự", ex);
            }
        }

        log.info("Hoàn tất phiên nhập nhân sự bởi Admin ID {}: Thành công={}, Thất bại={}", currentAdminId, importedCount, skippedCount);

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

    private ImportEmployeePreviewResult validateAndBuildPreview(List<RawEmployeeImportRow> rawRows) {
        List<OrgUnit> allOrgUnits = loadOrgUnitPort.findAllActive();
        Map<String, OrgUnit> orgUnitLookup = new HashMap<>();
        for (OrgUnit u : allOrgUnits) {
            orgUnitLookup.put(u.getUnitName().trim().toLowerCase(Locale.ROOT), u);
            if (u.getUnitCode() != null) {
                orgUnitLookup.put(u.getUnitCode().trim().toLowerCase(Locale.ROOT), u);
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
                String codeKey = employeeCode.toLowerCase(Locale.ROOT);
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

            // 3. Tên đăng nhập & Email
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
                String userKey = username.toLowerCase(Locale.ROOT);
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
                String normalizedEmail = email.toLowerCase(Locale.ROOT);
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

            // 5. Phòng ban / Đơn vị
            String orgUnitIdentifier = cleanString(raw.orgUnitIdentifier());
            Long resolvedOrgUnitId = null;
            String resolvedOrgUnitName = null;

            if (orgUnitIdentifier == null || orgUnitIdentifier.isBlank()) {
                errors.add("Phòng ban / Đơn vị không được để trống");
            } else {
                OrgUnit matchedUnit = orgUnitLookup.get(orgUnitIdentifier.trim().toLowerCase(Locale.ROOT));
                if (matchedUnit == null) {
                    errors.add("Phòng ban / Đơn vị '" + orgUnitIdentifier + "' không tồn tại trong hệ thống");
                } else {
                    resolvedOrgUnitId = matchedUnit.getId().getValue();
                    resolvedOrgUnitName = matchedUnit.getUnitName();
                }
            }

            // 6. Vai trò hệ thống (Role Whitelist & Protection)
            String roleCode = cleanString(raw.roleCode());
            if (roleCode == null || roleCode.isBlank()) {
                roleCode = "VT-04";
            } else {
                try {
                    RoleCode parsedRoleCode = RoleCode.fromCode(roleCode);
                    if (parsedRoleCode == RoleCode.VT_06) {
                        errors.add("Không cho phép tạo tài khoản Quản trị viên hệ thống (VT-06) qua tính năng nhập tệp");
                    } else {
                        roleCode = parsedRoleCode.getCode();
                    }
                } catch (Exception e) {
                    errors.add("Mã vai trò '" + roleCode + "' không hợp lệ (hỗ trợ VT-01, VT-02, VT-03, VT-04, VT-05)");
                }
            }

            // 7. Chức danh chuyên môn
            String professionalRole = cleanString(raw.professionalRole());

            // 8. Giờ làm việc chuẩn / Tuần (Phân biệt ô trống và giá trị sai định dạng)
            Integer standardHours = 40;
            String rawStandardHours = cleanString(raw.rawStandardHours());
            if (rawStandardHours != null) {
                try {
                    int parsedHours = Integer.parseInt(rawStandardHours);
                    if (parsedHours <= 0 || parsedHours > 168) {
                        errors.add("Giờ làm việc chuẩn (" + parsedHours + "h) phải lớn hơn 0 và không vượt quá 168h/tuần");
                    } else {
                        standardHours = parsedHours;
                    }
                } catch (NumberFormatException e) {
                    errors.add("Giờ làm việc chuẩn '" + rawStandardHours + "' không đúng định dạng số");
                }
            }

            // 9. Ngày bắt đầu & Ngày kết thúc HĐ (Phân biệt ô trống và giá trị sai định dạng)
            LocalDate startDate = null;
            String rawStartDate = cleanString(raw.rawStartDate());
            if (rawStartDate != null) {
                try {
                    startDate = parseDate(rawStartDate);
                } catch (Exception e) {
                    errors.add("Ngày bắt đầu '" + rawStartDate + "' không đúng định dạng (hỗ trợ dd/MM/yyyy hoặc yyyy-MM-dd)");
                }
            }

            LocalDate contractEndDate = null;
            String rawContractEndDate = cleanString(raw.rawContractEndDate());
            if (rawContractEndDate != null) {
                try {
                    contractEndDate = parseDate(rawContractEndDate);
                } catch (Exception e) {
                    errors.add("Ngày kết thúc hợp đồng '" + rawContractEndDate + "' không đúng định dạng (hỗ trợ dd/MM/yyyy hoặc yyyy-MM-dd)");
                }
            }

            if (startDate != null && contractEndDate != null && contractEndDate.isBefore(startDate)) {
                errors.add("Ngày kết thúc hợp đồng (" + contractEndDate + ") không được trước ngày bắt đầu (" + startDate + ")");
            }

            // 10. Nhân viên thuê ngoài (Phân biệt ô trống và giá trị sai định dạng)
            Boolean isOutsourced = false;
            String rawIsOutsourced = cleanString(raw.rawIsOutsourced());
            if (rawIsOutsourced != null) {
                String normalizedBoolean = rawIsOutsourced.toLowerCase(Locale.ROOT);
                if (TRUE_BOOLEAN_VALUES.contains(normalizedBoolean)) {
                    isOutsourced = true;
                } else if (FALSE_BOOLEAN_VALUES.contains(normalizedBoolean)) {
                    isOutsourced = false;
                } else {
                    errors.add("Trường thuê ngoài '" + rawIsOutsourced + "' không hợp lệ (nhập Có/Không hoặc True/False)");
                }
            }

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

    private LocalDate parseDate(String text) {
        String trimmed = text.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Không thể phân tích ngày: " + text);
    }

    private String generateSecureTemporaryPassword() {
        StringBuilder sb = new StringBuilder(16);
        sb.append(UPPER.charAt(SECURE_RANDOM.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(SECURE_RANDOM.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(SECURE_RANDOM.nextInt(SPECIAL.length())));
        for (int i = 4; i < 16; i++) {
            sb.append(ALL_CHARS.charAt(SECURE_RANDOM.nextInt(ALL_CHARS.length())));
        }
        char[] passwordArray = sb.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        return new String(passwordArray);
    }

    private String cleanString(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}