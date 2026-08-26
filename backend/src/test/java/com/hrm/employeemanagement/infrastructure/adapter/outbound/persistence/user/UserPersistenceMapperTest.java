package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPersistenceMapperTest {

    private UserPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserPersistenceMapper();
    }

    @Test
    @DisplayName("Ánh xạ AuditLog bảo toàn oldValue và newValue")
    void testAuditLogMapping_PreservesChangeValues() {
        AuditLog auditLog = AuditLog.createChange(
                1L,
                "UPDATE_AUTHORIZATION",
                "users",
                25L,
                "role=VT-04;dataScope=SELF;scopeOrgUnitId=null",
                "role=VT-02;dataScope=ORGANIZATION_BRANCH;scopeOrgUnitId=5"
        );

        AuditLogJpaEntity entity =
                mapper.toJpaEntity(auditLog);

        assertEquals(
                auditLog.getOldValue(),
                entity.getOldValue()
        );

        assertEquals(
                auditLog.getNewValue(),
                entity.getNewValue()
        );

        AuditLog mappedBack =
                mapper.toDomain(entity);

        assertEquals(
                auditLog.getOldValue(),
                mappedBack.getOldValue()
        );

        assertEquals(
                auditLog.getNewValue(),
                mappedBack.getNewValue()
        );
    }

    @Test
    @DisplayName("Ánh xạ từ UserJpaEntity sang User domain bảo toàn trường @Version")
    void testToDomain_PreservesVersion() {
        RoleJpaEntity roleJpa = new RoleJpaEntity(4L, "VT-04", "Nhân viên");
        UserJpaEntity jpaEntity = new UserJpaEntity(1L, "admin", "hash123", roleJpa, true, 5L);
        jpaEntity.setDataScope("SELF");

        User user = mapper.toDomain(jpaEntity, 10L);

        assertNotNull(user);
        assertEquals(1L, user.getIdValue());
        assertEquals("admin", user.getUsername());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(10L, user.getEmployeeIdValue());
        assertEquals(DataScope.SELF, user.getDataScope());
        assertNull(user.getScopeOrgUnitId());
        assertEquals(5L, user.getVersion());
    }

    @Test
    @DisplayName("Ánh xạ từ User domain sang UserJpaEntity bảo toàn trường @Version")
    void testToJpaEntity_PreservesVersion() {
        Role role = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên");
        User user = new User(
                new UserId(1L),
                "admin",
                "hash123",
                role,
                UserStatus.ACTIVE,
                new EmployeeId(10L),
                DataScope.SELF,
                null,
                3L
        );
        RoleJpaEntity roleJpa = new RoleJpaEntity(4L, "VT-04", "Nhân viên");

        UserJpaEntity jpaEntity = mapper.toJpaEntity(user, roleJpa);

        assertNotNull(jpaEntity);
        assertEquals(1L, jpaEntity.getId());
        assertEquals("admin", jpaEntity.getUsername());
        assertEquals("SELF", jpaEntity.getDataScope());
        assertNull(jpaEntity.getScopeOrgUnitId());
        assertEquals(3L, jpaEntity.getVersion());
        assertTrue(jpaEntity.getIsActive());
    }

    @Test
    @DisplayName("Cập nhật Managed Entity trực tiếp qua updateJpaEntity bảo toàn trường @Version")
    void testUpdateJpaEntity_PreservesVersionAndUpdatesFields() {
        RoleJpaEntity oldRoleJpa = new RoleJpaEntity(4L, "VT-04", "Nhân viên");
        UserJpaEntity managedEntity = new UserJpaEntity(2L, "staff", "old_hash", oldRoleJpa, true, 2L);
        managedEntity.setDataScope("SELF");

        Role newRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án");
        User domain = new User(
                new UserId(2L),
                "staff_updated",
                "new_hash",
                newRole,
                UserStatus.LOCKED,
                new EmployeeId(20L),
                DataScope.SELF,
                null,
                2L
        );
        RoleJpaEntity newRoleJpa = new RoleJpaEntity(2L, "VT-02", "Quản lý dự án");

        mapper.updateJpaEntity(managedEntity, domain, newRoleJpa);

        assertEquals("staff_updated", managedEntity.getUsername());
        assertEquals("new_hash", managedEntity.getPasswordHash());
        assertEquals("VT-02", managedEntity.getRole().getCode());
        assertFalse(managedEntity.getIsActive());
        assertEquals("SELF", managedEntity.getDataScope());
        assertNull(managedEntity.getScopeOrgUnitId());
        assertEquals(2L, managedEntity.getVersion());
    }

    @Test
    @DisplayName("Ánh xạ Employee mới bảo toàn thông tin nghề nghiệp và hợp đồng")
    void testEmployeeToJpaEntity_PreservesProfileFields() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate contractEndDate = LocalDate.of(2027, 12, 31);
        Employee employee = new Employee(
                new EmployeeId(10L), new UserId(20L), 30L, "EMP-020", "Nguyễn Văn A",
                "Backend Developer", startDate, contractEndDate, false, 40, EmployeeStatus.ACTIVE);

        EmployeeJpaEntity entity = mapper.toJpaEntity(employee);

        assertEquals("Backend Developer", entity.getProfessionalRole());
        assertEquals(startDate, entity.getStartDate());
        assertEquals(contractEndDate, entity.getContractEndDate());
    }
}
