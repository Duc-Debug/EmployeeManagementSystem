package com.hrm.employeemanagement.infrastructure.transaction.outsourcedcontract;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractListResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ScanOutsourcedContractsResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.AcknowledgeOutsourcedContractWarningUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.GetExpiringOutsourcedContractsUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.ScanOutsourcedContractExpirationsUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalOutsourcedContractDecoratorsTest {

    @Mock
    private GetExpiringOutsourcedContractsUseCase getContractsUseCase;

    @Mock
    private ScanOutsourcedContractExpirationsUseCase scanContractsUseCase;

    @Mock
    private AcknowledgeOutsourcedContractWarningUseCase acknowledgeUseCase;

    @Test
    @DisplayName("TransactionalGetExpiringOutsourcedContractsDecorator delegates to underlying usecase")
    void testGetExpiringDecoratorDelegation() {
        TransactionalGetExpiringOutsourcedContractsDecorator decorator =
                new TransactionalGetExpiringOutsourcedContractsDecorator(getContractsUseCase);

        ExpiringOutsourcedContractListResult expected = ExpiringOutsourcedContractListResult.of(Collections.emptyList());
        when(getContractsUseCase.execute(30)).thenReturn(expected);

        ExpiringOutsourcedContractListResult actual = decorator.execute(30);

        assertThat(actual).isSameAs(expected);
        verify(getContractsUseCase).execute(30);
    }

    @Test
    @DisplayName("TransactionalScanOutsourcedContractExpirationsDecorator delegates to underlying usecase")
    void testScanDecoratorDelegation() {
        TransactionalScanOutsourcedContractExpirationsDecorator decorator =
                new TransactionalScanOutsourcedContractExpirationsDecorator(scanContractsUseCase);

        ScanOutsourcedContractsResult expected = new ScanOutsourcedContractsResult(
                LocalDateTime.now(), 5, 2, 2, "OK"
        );
        when(scanContractsUseCase.execute(true)).thenReturn(expected);

        ScanOutsourcedContractsResult actual = decorator.execute(true);

        assertThat(actual).isSameAs(expected);
        verify(scanContractsUseCase).execute(true);
    }

    @Test
    @DisplayName("TransactionalAcknowledgeOutsourcedContractWarningDecorator delegates to underlying usecase")
    void testAcknowledgeDecoratorDelegation() {
        TransactionalAcknowledgeOutsourcedContractWarningDecorator decorator =
                new TransactionalAcknowledgeOutsourcedContractWarningDecorator(acknowledgeUseCase);

        AcknowledgeOutsourcedContractCommand command = new AcknowledgeOutsourcedContractCommand(
                101L, "Ghi chú gia hạn", LocalDate.now().plusMonths(3)
        );
        AcknowledgeOutsourcedContractResult expected = new AcknowledgeOutsourcedContractResult(
                101L, LocalDateTime.now(), "ACKNOWLEDGED", "Ghi nhận thành công"
        );
        when(acknowledgeUseCase.execute(command)).thenReturn(expected);

        AcknowledgeOutsourcedContractResult actual = decorator.execute(command);

        assertThat(actual).isSameAs(expected);
        verify(acknowledgeUseCase).execute(command);
    }
}
