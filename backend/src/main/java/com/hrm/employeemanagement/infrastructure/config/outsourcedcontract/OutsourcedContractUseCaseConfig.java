package com.hrm.employeemanagement.infrastructure.config.outsourcedcontract;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.AcknowledgeOutsourcedContractWarningUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.GetExpiringOutsourcedContractsUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.ScanOutsourcedContractExpirationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadNotificationRecipientUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedContractPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.outsourcedcontract.AcknowledgeOutsourcedContractWarningService;
import com.hrm.employeemanagement.application.service.outsourcedcontract.GetExpiringOutsourcedContractsService;
import com.hrm.employeemanagement.application.service.outsourcedcontract.ScanOutsourcedContractExpirationsService;
import com.hrm.employeemanagement.infrastructure.transaction.outsourcedcontract.TransactionalAcknowledgeOutsourcedContractWarningDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.outsourcedcontract.TransactionalGetExpiringOutsourcedContractsDecorator;
import com.hrm.employeemanagement.infrastructure.transaction.outsourcedcontract.TransactionalScanOutsourcedContractExpirationsDecorator;

@Configuration
public class OutsourcedContractUseCaseConfig {

    @Bean
    public GetExpiringOutsourcedContractsUseCase getExpiringOutsourcedContractsUseCase(
            GetAuthenticatedUserPort authenticatedUserPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            LoadOutsourcedContractPort loadContractPort,
            LoadOutsourcedAllocationPort loadAllocationPort
    ) {
        GetExpiringOutsourcedContractsService pureService = new GetExpiringOutsourcedContractsService(
                authenticatedUserPort,
                deniedAuditLogPort,
                loadContractPort,
                loadAllocationPort
        );
        return new TransactionalGetExpiringOutsourcedContractsDecorator(pureService);
    }

    @Bean
    public ScanOutsourcedContractExpirationsUseCase scanOutsourcedContractExpirationsUseCase(
            GetAuthenticatedUserPort authenticatedUserPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadOutsourcedContractPort loadContractPort,
            LoadOutsourcedAllocationPort loadAllocationPort,
            LoadNotificationRecipientUserPort recipientUserPort,
            CreateNotificationEventUseCase createNotificationEventUseCase
    ) {
        ScanOutsourcedContractExpirationsService pureService = new ScanOutsourcedContractExpirationsService(
                authenticatedUserPort,
                deniedAuditLogPort,
                saveAuditLogPort,
                loadContractPort,
                loadAllocationPort,
                recipientUserPort,
                createNotificationEventUseCase
        );
        return new TransactionalScanOutsourcedContractExpirationsDecorator(pureService);
    }

    @Bean
    public AcknowledgeOutsourcedContractWarningUseCase acknowledgeOutsourcedContractWarningUseCase(
            GetAuthenticatedUserPort authenticatedUserPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadOutsourcedContractPort loadContractPort
    ) {
        AcknowledgeOutsourcedContractWarningService pureService = new AcknowledgeOutsourcedContractWarningService(
                authenticatedUserPort,
                deniedAuditLogPort,
                saveAuditLogPort,
                loadContractPort
        );
        return new TransactionalAcknowledgeOutsourcedContractWarningDecorator(pureService);
    }
}
