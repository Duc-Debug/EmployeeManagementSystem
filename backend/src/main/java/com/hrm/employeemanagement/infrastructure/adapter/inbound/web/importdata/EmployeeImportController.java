package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.importdata;

import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hrm.employeemanagement.application.dto.importdata.ConfirmEmployeeImportCommand;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeePreviewResult;
import com.hrm.employeemanagement.application.dto.importdata.ImportExecutionResult;
import com.hrm.employeemanagement.application.port.inbound.importdata.ConfirmEmployeeImportUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.GenerateImportTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.PreviewEmployeeImportUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/imports/employees")
@Validated
public class EmployeeImportController {

    private final PreviewEmployeeImportUseCase previewUseCase;
    private final ConfirmEmployeeImportUseCase confirmUseCase;
    private final GenerateImportTemplateUseCase generateTemplateUseCase;

    public EmployeeImportController(
            PreviewEmployeeImportUseCase previewUseCase,
            ConfirmEmployeeImportUseCase confirmUseCase,
            GenerateImportTemplateUseCase generateTemplateUseCase
    ) {
        this.previewUseCase = previewUseCase;
        this.confirmUseCase = confirmUseCase;
        this.generateTemplateUseCase = generateTemplateUseCase;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DATA_IMPORT')")
    public ResponseEntity<ApiResponse<ImportEmployeePreviewResult>> preview(
            @RequestParam("file") MultipartFile file
    ) {
        try (InputStream inputStream = file.getInputStream()) {
            ImportEmployeePreviewResult result = previewUseCase.preview(inputStream, file.getOriginalFilename());
            return ResponseEntity.ok(ApiResponse.success(result.message(), result));
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Lỗi đọc tệp tải lên: " + e.getMessage(), e);
        }
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('DATA_IMPORT')")
    public ResponseEntity<ApiResponse<ImportExecutionResult>> confirm(
            @RequestBody ConfirmEmployeeImportCommand command
    ) {
        ImportExecutionResult result = confirmUseCase.confirm(command);
        return ResponseEntity.ok(ApiResponse.success(result.message(), result));
    }

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('DATA_IMPORT')")
    public ResponseEntity<byte[]> downloadTemplate(
            @RequestParam(defaultValue = "xlsx") String format
    ) {
        byte[] data = generateTemplateUseCase.generateEmployeeTemplate(format);
        String filename = "csv".equalsIgnoreCase(format)
                ? "mau_nhap_nhan_vien.csv"
                : "mau_nhap_nhan_vien.xlsx";

        MediaType mediaType = "csv".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv; charset=UTF-8")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }
}
