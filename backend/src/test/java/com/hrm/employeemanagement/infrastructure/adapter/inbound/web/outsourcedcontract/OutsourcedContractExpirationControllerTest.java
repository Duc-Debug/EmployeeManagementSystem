package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.outsourcedcontract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractListResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.OutsourcedAffectedAllocationResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ScanOutsourcedContractsResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.AcknowledgeOutsourcedContractWarningUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.GetExpiringOutsourcedContractsUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.ScanOutsourcedContractExpirationsUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutsourcedContractExpirationController Web API Tests (NCL-14-CN-003)")
class OutsourcedContractExpirationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetExpiringOutsourcedContractsUseCase getExpiringOutsourcedContractsUseCase;

    @Mock
    private ScanOutsourcedContractExpirationsUseCase scanOutsourcedContractExpirationsUseCase;

    @Mock
    private AcknowledgeOutsourcedContractWarningUseCase acknowledgeOutsourcedContractWarningUseCase;

    @BeforeEach
    void setUp() {
        OutsourcedContractExpirationController controller = new OutsourcedContractExpirationController(
                getExpiringOutsourcedContractsUseCase,
                scanOutsourcedContractExpirationsUseCase,
                acknowledgeOutsourcedContractWarningUseCase
        );
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/outsourced-contracts/expiring - Trả về 200 OK với danh sách hợp đồng sắp hết hạn")
    void getExpiringContracts_Success() throws Exception {
        OutsourcedAffectedAllocationResult affected = new OutsourcedAffectedAllocationResult(
                101L, 201L, "Dự án CRM", 2026, 42,
                LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18),
                BigDecimal.valueOf(40.0), "SPANS_OVER_EXPIRY", "Vắt qua hạn hợp đồng vi phạm QTN-21"
        );

        ExpiringOutsourcedContractResult contract = new ExpiringOutsourcedContractResult(
                12L, "EXT-001", "Nguyễn Văn Thuê", "Backend Developer",
                3L, "Phòng Phần mềm", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 10, 16), 25, "EXPIRING_SOON", 1,
                List.of(affected)
        );

        when(getExpiringOutsourcedContractsUseCase.execute(30))
                .thenReturn(ExpiringOutsourcedContractListResult.of(List.of(contract)));

        mockMvc.perform(get("/api/outsourced-contracts/expiring")
                        .param("thresholdDays", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpiringContracts").value(1))
                .andExpect(jsonPath("$.items[0].employeeCode").value("EXT-001"))
                .andExpect(jsonPath("$.items[0].fullName").value("Nguyễn Văn Thuê"))
                .andExpect(jsonPath("$.items[0].daysRemaining").value(25))
                .andExpect(jsonPath("$.items[0].status").value("EXPIRING_SOON"))
                .andExpect(jsonPath("$.items[0].affectedAllocations[0].projectName").value("Dự án CRM"))
                .andExpect(jsonPath("$.items[0].affectedAllocations[0].affectedType").value("SPANS_OVER_EXPIRY"));
    }

    @Test
    @DisplayName("POST /api/outsourced-contracts/scan - Trả về 200 OK khi kích hoạt rà soát thủ công")
    void scanContractsManually_Success() throws Exception {
        ScanOutsourcedContractsResult scanResult = new ScanOutsourcedContractsResult(
                LocalDateTime.now(), 10, 2, 2,
                "Đã rà soát 10 nhân sự, phát hiện 2 hợp đồng sắp hết hạn và đã gửi cảnh báo."
        );

        when(scanOutsourcedContractExpirationsUseCase.execute(true)).thenReturn(scanResult);

        mockMvc.perform(post("/api/outsourced-contracts/scan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScanned").value(10))
                .andExpect(jsonPath("$.totalExpiringContractsFound").value(2))
                .andExpect(jsonPath("$.notificationsSent").value(2));
    }

    @Test
    @DisplayName("POST /api/outsourced-contracts/{id}/acknowledge - Trả về 200 OK khi xác nhận xử lý cảnh báo")
    void acknowledgeContractWarning_Success() throws Exception {
        AcknowledgeOutsourcedContractResult ackResult = new AcknowledgeOutsourcedContractResult(
                12L, LocalDateTime.now(), "ACKNOWLEDGED", "Đã ghi nhận xử lý cảnh báo thành công."
        );

        when(acknowledgeOutsourcedContractWarningUseCase.execute(any(AcknowledgeOutsourcedContractCommand.class)))
                .thenReturn(ackResult);

        String requestBody = """
                {
                    "actionNote": "Đang làm thủ tục gia hạn với đối tác",
                    "expectedResolutionDate": "2026-10-01"
                }
                """;

        mockMvc.perform(post("/api/outsourced-contracts/12/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(12))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    @DisplayName("GET /api/outsourced-contracts/expiring - Khi bị từ chối quyền -> Trả về HTTP 403 Forbidden")
    void getExpiringContracts_Unauthorized_Returns403() throws Exception {
        when(getExpiringOutsourcedContractsUseCase.execute(30))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Bạn không có quyền truy cập chức năng này"));

        mockMvc.perform(get("/api/outsourced-contracts/expiring")
                        .param("thresholdDays", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
