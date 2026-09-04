package com.hrm.employeemanagement.infrastructure.transaction.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.hrm.employeemanagement.application.dto.project.CreateProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.project.ProjectStatus;

@ExtendWith(MockitoExtension.class)
class RetryableCreateProjectUseCaseDecoratorTest {

    @Mock
    private CreateProjectUseCase delegate;

    private RetryableCreateProjectUseCaseDecorator decorator;

    private final CreateProjectCommand sampleCommand = new CreateProjectCommand(
            "Dự án Thử nghiệm",
            100L,
            50L,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 12, 31),
            BigDecimal.valueOf(100),
            "Mô tả"
    );

    private final ProjectResult sampleResult = new ProjectResult(
            1L,
            "PRJ-IT-260903-ABCDEF",
            "Dự án Thử nghiệm",
            100L,
            50L,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 12, 31),
            BigDecimal.valueOf(100),
            "Mô tả",
            ProjectStatus.ACTIVE,
            10L,
            LocalDateTime.now(),
            null
    );

    @BeforeEach
    void setUp() {
        decorator = new RetryableCreateProjectUseCaseDecorator(delegate, 3);
    }

    @Test
    @DisplayName("Thành công ngay lần đầu tiên, không cần retry")
    void testCreateProject_SuccessOnFirstAttempt() {
        when(delegate.createProject(sampleCommand)).thenReturn(sampleResult);

        ProjectResult result = decorator.createProject(sampleCommand);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(delegate, times(1)).createProject(sampleCommand);
    }

    @Test
    @DisplayName("Tự động retry và thành công ở lần thứ hai khi lần đầu trùng mã")
    void testCreateProject_RetryOnDuplicateCode_Success() {
        when(delegate.createProject(sampleCommand))
                .thenThrow(new DuplicateProjectCodeException("Mã trùng lần 1"))
                .thenReturn(sampleResult);

        ProjectResult result = decorator.createProject(sampleCommand);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(delegate, times(2)).createProject(sampleCommand);
    }

    @Test
    @DisplayName("Ném DuplicateProjectCodeException khi vượt quá 3 lần retry vẫn trùng mã")
    void testCreateProject_ExhaustRetries_ThrowsException() {
        when(delegate.createProject(sampleCommand))
                .thenThrow(new DuplicateProjectCodeException("Mã trùng lần 1"))
                .thenThrow(new DuplicateProjectCodeException("Mã trùng lần 2"))
                .thenThrow(new DuplicateProjectCodeException("Mã trùng lần 3"));

        assertThatThrownBy(() -> decorator.createProject(sampleCommand))
                .isInstanceOf(DuplicateProjectCodeException.class)
                .hasMessageContaining("Mã trùng lần 3");

        verify(delegate, times(3)).createProject(sampleCommand);
    }

    @Test
    @DisplayName("Ném lỗi ngay lập tức mà không retry khi gặp lỗi khác DuplicateProjectCodeException")
    void testCreateProject_OtherException_NoRetry() {
        when(delegate.createProject(sampleCommand))
                .thenThrow(new InvalidProjectDataException("Dữ liệu không hợp lệ"));

        assertThatThrownBy(() -> decorator.createProject(sampleCommand))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("Dữ liệu không hợp lệ");

        verify(delegate, times(1)).createProject(sampleCommand);
    }
}