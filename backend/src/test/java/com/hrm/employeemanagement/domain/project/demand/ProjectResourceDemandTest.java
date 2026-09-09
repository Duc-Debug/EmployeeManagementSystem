package com.hrm.employeemanagement.domain.project.demand;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.project.InvalidResourceDemandException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.RoleId;

@DisplayName("ProjectResourceDemand Entity Tests")
class ProjectResourceDemandTest {

    @Test
    @DisplayName("Tạo mới ProjectResourceDemand thành công")
    void createNew_Success() {
        ProjectId projectId = new ProjectId(1L);
        RoleId roleId = new RoleId(4L);
        YearWeek yearWeek = YearWeek.of(2026, 41);
        BigDecimal hours = new BigDecimal("20.00");

        ProjectResourceDemand demand = ProjectResourceDemand.createNew(projectId, roleId, yearWeek, hours);

        assertNotNull(demand);
        assertEquals(1L, demand.getProjectIdValue());
        assertEquals(4L, demand.getRoleIdValue());
        assertEquals(2026, demand.getYear());
        assertEquals(41, demand.getWeekNumber());
        assertEquals(new BigDecimal("20.00"), demand.getRequiredHours());
    }

    @Test
    @DisplayName("Cập nhật số giờ nhu cầu thành công")
    void updateRequiredHours_Success() {
        ProjectResourceDemand demand = ProjectResourceDemand.createNew(
                new ProjectId(1L), new RoleId(4L), YearWeek.of(2026, 41), new BigDecimal("20.00"));

        demand.updateRequiredHours(new BigDecimal("35.50"));

        assertEquals(new BigDecimal("35.50"), demand.getRequiredHours());
        assertNotNull(demand.getUpdatedAt());
    }

    @Test
    @DisplayName("Ném InvalidResourceDemandException khi tạo hoặc sửa với số giờ âm")
    void invalidHours_ThrowsException() {
        ProjectId projectId = new ProjectId(1L);
        RoleId roleId = new RoleId(4L);
        YearWeek yearWeek = YearWeek.of(2026, 41);

        assertThrows(InvalidResourceDemandException.class,
                () -> ProjectResourceDemand.createNew(projectId, roleId, yearWeek, new BigDecimal("-10.00")));

        ProjectResourceDemand validDemand = ProjectResourceDemand.createNew(
                projectId, roleId, yearWeek, new BigDecimal("20.00"));

        assertThrows(InvalidResourceDemandException.class,
                () -> validDemand.updateRequiredHours(BigDecimal.ZERO));
    }
}