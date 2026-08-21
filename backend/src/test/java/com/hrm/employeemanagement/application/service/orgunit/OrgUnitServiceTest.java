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
import java.util.Optional;

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
        // DEV-CENTER is created as a child of COMPANY_ROOT (id: 1)
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, 1L, "Mô tả"
        );

        OrgUnit rootCompany = new OrgUnit(
                new OrgUnitId(1L), "COMPANY_ROOT", "Công Ty Cổ Phần Software", OrgUnitType.COMPANY,
                null, "/1/", 1, OrgUnitStatus.ACTIVE, "Nút gốc", null, LocalDateTime.now(), null
        );

        when(loadOrgUnitPort.existsByUnitCode("DEV-CENTER")).thenReturn(false);
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(rootCompany));

        OrgUnit savedUnit = new OrgUnit(
                new OrgUnitId(2L), "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                new OrgUnitId(1L), "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả", null, LocalDateTime.now(), null
        );
        when(saveOrgUnitPort.save(any(OrgUnit.class))).thenReturn(savedUnit);

        OrgUnitResult result = orgUnitService.execute(command);

        // Verify business behavior
        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("DEV-CENTER", result.unitCode());
        assertEquals("Khối Phát Triển", result.unitName());
        assertEquals(OrgUnitType.CENTER, result.unitType());
        assertEquals(1L, result.parentId());
        assertEquals("/1/2/", result.treePath());
        assertEquals(2, result.level());
        assertEquals(OrgUnitStatus.ACTIVE, result.status());

        // Verify save() is called exactly once
        verify(saveOrgUnitPort).save(any(OrgUnit.class));
    }

    @Test
    @DisplayName("Should throw DuplicateUnitCodeException when unit code already exists")
    void shouldThrowExceptionWhenUnitCodeExists() {
        CreateOrgUnitCommand command = new CreateOrgUnitCommand(
                "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER, 1L, "Mô tả"
        );

        when(loadOrgUnitPort.existsByUnitCode("DEV-CENTER")).thenReturn(true);

        assertThrows(DuplicateUnitCodeException.class, () -> orgUnitService.execute(command));
        verify(saveOrgUnitPort, never()).save(any());
    }
}