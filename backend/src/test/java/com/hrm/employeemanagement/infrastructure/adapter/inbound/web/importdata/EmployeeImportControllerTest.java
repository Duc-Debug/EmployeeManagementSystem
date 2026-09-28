package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.importdata;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeePreviewResult;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeeRowDto;
import com.hrm.employeemanagement.application.dto.importdata.ImportExecutionResult;
import com.hrm.employeemanagement.application.port.inbound.importdata.ConfirmEmployeeImportUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.GenerateImportTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.importdata.PreviewEmployeeImportUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeImportController Tests (NCL-12-CN-004)")
class EmployeeImportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PreviewEmployeeImportUseCase previewUseCase;

    @Mock
    private ConfirmEmployeeImportUseCase confirmUseCase;

    @Mock
    private GenerateImportTemplateUseCase generateTemplateUseCase;

    @InjectMocks
    private EmployeeImportController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API Preview tải file lên trả về HTTP 200 và kết quả xem trước")
    void preview_Success() throws Exception {
        ImportEmployeeRowDto row = new ImportEmployeeRowDto(
                2, "EMP001", "Nguyen Van A", "an.nguyen", "an@test.com",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-04", "Developer",
                40, null, null, false, true, List.of()
        );

        ImportEmployeePreviewResult previewResult = new ImportEmployeePreviewResult(
                1, 1, 0, List.of(row), true, "Kiểm tra thành công 1 dòng"
        );

        when(previewUseCase.preview(any(), any())).thenReturn(previewResult);

        MockMultipartFile file = new MockMultipartFile(
                "file", "employees.csv", "text/csv", "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/imports/employees/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(1))
                .andExpect(jsonPath("$.data.validRows").value(1))
                .andExpect(jsonPath("$.data.rows[0].employeeCode").value("EMP001"));
    }

    @Test
    @DisplayName("API Confirm xác nhận nhập dữ liệu trả về kết quả số lượng thành công")
    void confirm_Success() throws Exception {
        ImportExecutionResult executionResult = new ImportExecutionResult(
                1, 0, List.of(), "Đã nhập thành công 1 hồ sơ", LocalDateTime.now()
        );

        when(confirmUseCase.confirm(any())).thenReturn(executionResult);

        String jsonPayload = "{\"rows\": []}";

        mockMvc.perform(post("/api/v1/imports/employees/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.importedCount").value(1));
    }

    @Test
    @DisplayName("API Download template trả về file đính kèm với Content-Disposition")
    void downloadTemplate_Success() throws Exception {
        when(generateTemplateUseCase.generateEmployeeTemplate("xlsx")).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/imports/employees/template?format=xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"mau_nhap_nhan_vien.xlsx\""));
    }

    @Test
    @DisplayName("API Bị từ chối khi không có quyền (HTTP 403 Forbidden)")
    void accessDenied_Forbidden() throws Exception {
        when(confirmUseCase.confirm(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.DATA_IMPORT));

        mockMvc.perform(post("/api/v1/imports/employees/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rows\": []}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("API Preview từ chối tệp rỗng (HTTP 400 Bad Request)")
    void preview_EmptyFile_BadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/imports/employees/preview").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tệp tải lên không có dữ liệu"));
    }

    @Test
    @DisplayName("API Preview từ chối tệp vượt quá 10MB (HTTP 400 Bad Request)")
    void preview_Exceeds10MB_BadRequest() throws Exception {
        byte[] largeBytes = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", largeBytes
        );

        mockMvc.perform(multipart("/api/v1/imports/employees/preview").file(largeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dung lượng tệp vượt quá giới hạn cho phép (tối đa 10MB)"));
    }
}
