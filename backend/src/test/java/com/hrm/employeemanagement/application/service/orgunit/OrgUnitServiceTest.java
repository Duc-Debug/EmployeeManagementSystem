package com.hrm.employeemanagement.application.service.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.*;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.SaveOrgUnitPort;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.orgunit.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgUnitServiceTest {

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private SaveOrgUnitPort saveOrgUnitPort;

    @InjectMocks
    private OrgUnitService orgUnitService;

    @Test
    @DisplayName("Should create org unit successfully when parameters are valid")
    void shouldCreateOrgUnitSuccessfully() {
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, null, "Mô tả"
        );

        when(loadOrgUnitPort.existsByUnitCode("DEV-CENTER")).thenReturn(false);

        OrgUnit savedUnit = new OrgUnit(
                new OrgUnitId(1L), "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                null, "/1/", 1, OrgUnitStatus.ACTIVE, "Mô tả", null, LocalDateTime.now(), null
        );
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenReturn(savedUnit);

        OrgUnitResult result = orgUnitService.execute(command);

        assertNotNull(result);
        assertEquals("DEV-CENTER", result.unitCode());
        assertEquals("Khối Phát Triển", result.unitName());
        verify(saveOrgUnitPort, times(2)).save(any(OrgUnit.class));
    }

    @Test
    @DisplayName("Should throw DuplicateUnitCodeException when unit code already exists")
    void shouldThrowExceptionWhenUnitCodeExists() {
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, null, "Mô tả"
        );

        when(loadOrgUnitPort.existsByUnitCode("DEV-CENTER")).thenReturn(true);

        assertThrows(DuplicateUnitCodeException.class, () -> orgUnitService.execute(command));
        verify(saveOrgUnitPort, never()).save(any());
    }
}
