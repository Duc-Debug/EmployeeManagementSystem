package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.application.port.inbound.orgunit.*;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto.CreateOrgUnitRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit.dto.OrgUnitResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgUnitControllerTest {

    @Mock
    private CreateOrgUnitUseCase createOrgUnitUseCase;
    @Mock
    private UpdateOrgUnitUseCase updateOrgUnitUseCase;
    @Mock
    private MoveOrgUnitUseCase moveOrgUnitUseCase;
    @Mock
    private DeactivateOrgUnitUseCase deactivateOrgUnitUseCase;
    @Mock
    private GetOrgTreeUseCase getOrgTreeUseCase;

    @InjectMocks
    private OrgUnitController orgUnitController;

    @Test
    @DisplayName("createUnit should return HTTP 201 Created when request is valid")
    void shouldReturn201CreatedWhenCreateOrgUnitIsValid() {
        CreateOrgUnitRequest request = new CreateOrgUnitRequest(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, null, "Mô tả"
        );

        OrgUnitResult mockResult = new OrgUnitResult(
                1L, "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                null, "/1/", 1, OrgUnitStatus.ACTIVE, "Mô tả", null, LocalDateTime.now(), null
        );

        when(createOrgUnitUseCase.execute(any())).thenReturn(mockResult);

        ResponseEntity<OrgUnitResponse> response = orgUnitController.createUnit(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DEV-CENTER", response.getBody().unitCode());
    }
}
