package com.hrm.employeemanagement.infrastructure.transaction.project;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;
import com.hrm.employeemanagement.application.service.project.ProjectResourceDemandService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionalProjectResourceDemandServiceDecorator Tests")
class TransactionalProjectResourceDemandServiceDecoratorTest {

    @Mock
    private ProjectResourceDemandService delegate;

    private TransactionalProjectResourceDemandServiceDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new TransactionalProjectResourceDemandServiceDecorator(delegate);
    }

    @Test
    @DisplayName("Ủy quyền estimateDemand chính xác tới delegate service")
    void testEstimateDemand_DelegatesCorrectly() {
        EstimateResourceDemandCommand command = new EstimateResourceDemandCommand(
                1L, 4L, new BigDecimal("20.00"));
        ProjectResourceDemandSummaryResult expectedResult = new ProjectResourceDemandSummaryResult(
                1L, "PRJ-01", "Dự án", new BigDecimal("100.00"), new BigDecimal("80.00"),
                false, null, Collections.emptyList());

        when(delegate.estimateDemand(command)).thenReturn(expectedResult);

        ProjectResourceDemandSummaryResult actual = decorator.estimateDemand(command);

        assertThat(actual).isEqualTo(expectedResult);
        verify(delegate).estimateDemand(command);
    }

    @Test
    @DisplayName("Ủy quyền getProjectResourceDemands chính xác tới delegate service")
    void testGetProjectResourceDemands_DelegatesCorrectly() {
        ProjectResourceDemandSummaryResult expectedResult = new ProjectResourceDemandSummaryResult(
                1L, "PRJ-01", "Dự án", new BigDecimal("100.00"), new BigDecimal("80.00"),
                false, null, Collections.emptyList());

        when(delegate.getProjectResourceDemands(1L)).thenReturn(expectedResult);

        ProjectResourceDemandSummaryResult actual = decorator.getProjectResourceDemands(1L);

        assertThat(actual).isEqualTo(expectedResult);
        verify(delegate).getProjectResourceDemands(1L);
    }
}