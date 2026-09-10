package com.hrm.employeemanagement.infrastructure.transaction.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;
import com.hrm.employeemanagement.domain.exception.project.DuplicateResourceDemandException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetryableEstimateResourceDemandUseCaseDecorator Tests")
class RetryableEstimateResourceDemandUseCaseDecoratorTest {

    @Mock
    private TransactionalProjectResourceDemandServiceDecorator delegate;

    private RetryableEstimateResourceDemandUseCaseDecorator decorator;

    private final EstimateResourceDemandCommand sampleCommand = new EstimateResourceDemandCommand(
            1L, 4L, new BigDecimal("20.00"));

    private final ProjectResourceDemandSummaryResult sampleResult = new ProjectResourceDemandSummaryResult(
            1L, "PRJ-01", "Dự án", new BigDecimal("100.00"), new BigDecimal("80.00"),
            false, null, Collections.emptyList());

    @BeforeEach
    void setUp() {
        decorator = new RetryableEstimateResourceDemandUseCaseDecorator(delegate, 3);
    }

    @Test
    @DisplayName("Thành công ngay lần đầu tiên, không cần retry")
    void testEstimateDemand_SuccessOnFirstAttempt() {
        when(delegate.estimateDemand(sampleCommand)).thenReturn(sampleResult);

        ProjectResourceDemandSummaryResult result = decorator.estimateDemand(sampleCommand);

        assertThat(result).isNotNull();
        assertThat(result.projectId()).isEqualTo(1L);
        verify(delegate, times(1)).estimateDemand(sampleCommand);
    }

    @Test
    @DisplayName("Tự động retry và thành công ở lần thứ hai khi gặp DuplicateResourceDemandException do race condition")
    void testEstimateDemand_RetryOnDuplicate_Success() {
        when(delegate.estimateDemand(sampleCommand))
                .thenThrow(new DuplicateResourceDemandException("Xung đột dữ liệu lần 1"))
                .thenReturn(sampleResult);

        ProjectResourceDemandSummaryResult result = decorator.estimateDemand(sampleCommand);

        assertThat(result).isNotNull();
        assertThat(result.projectId()).isEqualTo(1L);
        verify(delegate, times(2)).estimateDemand(sampleCommand);
    }

    @Test
    @DisplayName("Ném DuplicateResourceDemandException khi vượt quá 3 lần retry vẫn xung đột dữ liệu")
    void testEstimateDemand_ExhaustRetries_ThrowsException() {
        when(delegate.estimateDemand(sampleCommand))
                .thenThrow(new DuplicateResourceDemandException("Xung đột dữ liệu lần 1"))
                .thenThrow(new DuplicateResourceDemandException("Xung đột dữ liệu lần 2"))
                .thenThrow(new DuplicateResourceDemandException("Xung đột dữ liệu lần 3"));

        assertThatThrownBy(() -> decorator.estimateDemand(sampleCommand))
                .isInstanceOf(DuplicateResourceDemandException.class)
                .hasMessageContaining("Xung đột dữ liệu lần 3");

        verify(delegate, times(3)).estimateDemand(sampleCommand);
    }

    @Test
    @DisplayName("Ném lỗi ngay lập tức mà không retry khi gặp lỗi khác DuplicateResourceDemandException")
    void testEstimateDemand_OtherException_NoRetry() {
        when(delegate.estimateDemand(sampleCommand))
                .thenThrow(new InvalidProjectDataException("Dữ liệu không hợp lệ"));

        assertThatThrownBy(() -> decorator.estimateDemand(sampleCommand))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("Dữ liệu không hợp lệ");

        verify(delegate, times(1)).estimateDemand(sampleCommand);
    }

    @Test
    @DisplayName("Ủy quyền getProjectResourceDemands không cần retry")
    void testGetProjectResourceDemands_DelegatesDirectly() {
        when(delegate.getProjectResourceDemands(1L)).thenReturn(sampleResult);

        ProjectResourceDemandSummaryResult result = decorator.getProjectResourceDemands(1L);

        assertThat(result).isEqualTo(sampleResult);
        verify(delegate, times(1)).getProjectResourceDemands(1L);
    }

    @Test
    @DisplayName("Ủy quyền deleteDemand trực tiếp")
    void testDeleteDemand_DelegatesDirectly() {
        when(delegate.deleteDemand(1L, 4L)).thenReturn(sampleResult);

        ProjectResourceDemandSummaryResult result = decorator.deleteDemand(1L, 4L);

        assertThat(result).isEqualTo(sampleResult);
        verify(delegate, times(1)).deleteDemand(1L, 4L);
    }

    @Test
    @DisplayName("Ủy quyền getProjectRoles trực tiếp")
    void testGetProjectRoles_DelegatesDirectly() {
        var expectedRoles = java.util.List.of(
                new com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult(1L, "DEV", "Dev", "Mô tả"));

        when(delegate.getProjectRoles()).thenReturn(expectedRoles);

        var actual = decorator.getProjectRoles();

        assertThat(actual).isEqualTo(expectedRoles);
        verify(delegate, times(1)).getProjectRoles();
    }
}