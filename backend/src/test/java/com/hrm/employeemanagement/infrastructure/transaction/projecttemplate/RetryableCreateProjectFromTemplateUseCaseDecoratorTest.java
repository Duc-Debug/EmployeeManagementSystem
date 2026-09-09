package com.hrm.employeemanagement.infrastructure.transaction.projecttemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.CreateProjectFromTemplateCommand;
import com.hrm.employeemanagement.application.port.inbound.projecttemplate.CreateProjectFromTemplateUseCase;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.project.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class RetryableCreateProjectFromTemplateUseCaseDecoratorTest {

    @Mock
    private CreateProjectFromTemplateUseCase delegate;

    private RetryableCreateProjectFromTemplateUseCaseDecorator decorator;

    private final CreateProjectFromTemplateCommand sampleCommand = new CreateProjectFromTemplateCommand(
            1L,
            "Dự án ERP từ Mẫu",
            100L,
            50L,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 12, 31),
            "Mô tả dự án từ mẫu"
    );

    private final ProjectResult sampleResult = new ProjectResult(
            1L,
            "PRJ-IT-260909-ABCDEF",
            "Dự án ERP từ Mẫu",
            100L,
            50L,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 12, 31),
            BigDecimal.valueOf(120),
            "Mô tả dự án từ mẫu",
            ProjectStatus.ACTIVE,
            10L,
            LocalDateTime.now(),
            null
    );

    @BeforeEach
    void setUp() {
        decorator = new RetryableCreateProjectFromTemplateUseCaseDecorator(delegate, 3);
    }

    @Test
    @DisplayName("Thành công ngay lần đầu tiên, không cần retry")
    void testCreateProjectFromTemplate_SuccessOnFirstAttempt() {
        when(delegate.createProjectFromTemplate(sampleCommand)).thenReturn(sampleResult);

        ProjectResult result = decorator.createProjectFromTemplate(sampleCommand);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(delegate, times(1)).createProjectFromTemplate(sampleCommand);
    }

    @Test
    @DisplayName("Tự động retry và thành công ở lần thứ hai khi lần đầu trùng mã dự án")
    void testCreateProjectFromTemplate_RetryOnDuplicateCode_Success() {
        when(delegate.createProjectFromTemplate(sampleCommand))
                .thenThrow(new DuplicateProjectCodeException("Mã trùng lần 1"))
                .thenReturn(sampleResult);

        ProjectResult result = decorator.createProjectFromTemplate(sampleCommand);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(delegate, times(2)).createProjectFromTemplate(sampleCommand);
    }

    @Test
    @DisplayName("Ném DuplicateProjectCodeException khi vượt quá 3 lần retry vẫn trùng mã")
    void testCreateProjectFromTemplate_ExhaustRetries_ThrowsException() {
        when(delegate.createProjectFromTemplate(sampleCommand))
                .thenThrow(new DuplicateProjectCodeException("Mã trùng lần 1"))
                .thenThrow(new DuplicateProjectCodeException("Mã trùng lần 2"))
                .thenThrow(new DuplicateProjectCodeException("Mã trùng lần 3"));

        assertThatThrownBy(() -> decorator.createProjectFromTemplate(sampleCommand))
                .isInstanceOf(DuplicateProjectCodeException.class)
                .hasMessageContaining("Mã trùng lần 3");

        verify(delegate, times(3)).createProjectFromTemplate(sampleCommand);
    }

    @Test
    @DisplayName("Ném lỗi ngay lập tức mà không retry khi gặp lỗi khác DuplicateProjectCodeException")
    void testCreateProjectFromTemplate_OtherException_NoRetry() {
        when(delegate.createProjectFromTemplate(sampleCommand))
                .thenThrow(new InvalidProjectDataException("Dữ liệu không hợp lệ"));

        assertThatThrownBy(() -> decorator.createProjectFromTemplate(sampleCommand))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("Dữ liệu không hợp lệ");

        verify(delegate, times(1)).createProjectFromTemplate(sampleCommand);
    }
}
