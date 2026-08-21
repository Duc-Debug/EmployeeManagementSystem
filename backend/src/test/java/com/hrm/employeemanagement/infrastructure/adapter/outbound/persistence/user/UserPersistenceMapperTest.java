package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("Ánh xạ từ UserJpaEntity sang User domain bảo toàn trường @Version")
    void testToDomain_PreservesVersion() {
        RoleJpaEntity roleJpa = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");
        UserJpaEntity jpaEntity = new UserJpaEntity(1L, "admin", "hash123", roleJpa, true, 5L);

        User user = mapper.toDomain(jpaEntity, 10L);

        assertNotNull(user);
        assertEquals(1L, user.getIdValue());
        assertEquals("admin", user.getUsername());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(10L, user.getEmployeeIdValue());
        assertEquals(5L, user.getVersion());
    }

    @Test
    @DisplayName("Ánh xạ từ User domain sang UserJpaEntity bảo toàn trường @Version")
    void testToJpaEntity_PreservesVersion() {
        Role role = new Role(new RoleId(6L), RoleCode.VT_06, "Quản trị viên");
        User user = new User(new UserId(1L), "admin", "hash123", role, UserStatus.ACTIVE, new EmployeeId(10L), 3L);
        RoleJpaEntity roleJpa = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");

        UserJpaEntity jpaEntity = mapper.toJpaEntity(user, roleJpa);

        assertNotNull(jpaEntity);
        assertEquals(1L, jpaEntity.getId());
        assertEquals("admin", jpaEntity.getUsername());
        assertEquals(3L, jpaEntity.getVersion());
        assertTrue(jpaEntity.getIsActive());
    }

    @Test
    @DisplayName("Cập nhật Managed Entity trực tiếp qua updateJpaEntity bảo toàn trường @Version")
    void testUpdateJpaEntity_PreservesVersionAndUpdatesFields() {
        RoleJpaEntity oldRoleJpa = new RoleJpaEntity(4L, "VT-04", "Nhân viên");
        UserJpaEntity managedEntity = new UserJpaEntity(2L, "staff", "old_hash", oldRoleJpa, true, 2L);

        Role newRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án");
        User domain = new User(new UserId(2L), "staff_updated", "new_hash", newRole, UserStatus.LOCKED, new EmployeeId(20L), 2L);
        RoleJpaEntity newRoleJpa = new RoleJpaEntity(2L, "VT-02", "Quản lý dự án");

        mapper.updateJpaEntity(managedEntity, domain, newRoleJpa);

        assertEquals("staff_updated", managedEntity.getUsername());
        assertEquals("new_hash", managedEntity.getPasswordHash());
        assertEquals("VT-02", managedEntity.getRole().getCode());
        assertFalse(managedEntity.getIsActive());
        assertEquals(2L, managedEntity.getVersion());
    }
}
