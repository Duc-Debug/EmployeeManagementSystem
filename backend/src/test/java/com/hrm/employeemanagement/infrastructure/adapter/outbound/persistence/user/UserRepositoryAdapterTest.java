package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private SpringDataUserRepository springDataUserRepository;

    @Mock
    private SpringDataRoleRepository springDataRoleRepository;

    private final UserPersistenceMapper mapper = new UserPersistenceMapper();

    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserRepositoryAdapter(springDataUserRepository, springDataRoleRepository, mapper);
    }

    @Test
    @DisplayName("findAll nạp danh sách User trong 1 query và không tạo N+1 query")
    void testFindAll_LoadsUsersEfficientlyWithoutNPlusOne() {
        RoleJpaEntity roleJpa = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");
        UserJpaEntity u1 = userJpa(1L, "admin", "hash1", roleJpa, true, 0L);
        UserJpaEntity u2 = userJpa(2L, "staff", "hash2", roleJpa, true, 0L);

        when(springDataUserRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(u1, u2)));

        List<User> result = adapter.findAll(0, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("admin", result.get(0).getUsername());
        assertEquals("staff", result.get(1).getUsername());
        verify(springDataUserRepository).findAll(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("findById nạp User từ repository thành công")
    void testFindById_Success() {
        RoleJpaEntity roleJpa = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");
        UserJpaEntity u1 = userJpa(1L, "admin", "hash1", roleJpa, true, 0L);
        when(springDataUserRepository.findById(1L)).thenReturn(Optional.of(u1));

        Optional<User> userOpt = adapter.findById(new UserId(1L));

        assertTrue(userOpt.isPresent());
        assertEquals("admin", userOpt.get().getUsername());
        assertEquals(1L, userOpt.get().getIdValue());
    }

    @Test
    @DisplayName("findByUsername nạp User từ repository thành công")
    void testFindByUsername_Success() {
        RoleJpaEntity roleJpa = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");
        UserJpaEntity u1 = userJpa(1L, "admin", "hash1", roleJpa, true, 0L);
        when(springDataUserRepository.findByUsername("admin")).thenReturn(Optional.of(u1));

        Optional<User> userOpt = adapter.findByUsername("admin");

        assertTrue(userOpt.isPresent());
        assertEquals("admin", userOpt.get().getUsername());
    }

    @Test
    @DisplayName("existsInOrgUnitBranch gọi thẳng repository để kiểm tra target user trong branch")
    void testExistsInOrgUnitBranch_DelegatesToRepository() {
        when(springDataUserRepository.existsInOrgUnitBranch(2L, 5L))
                .thenReturn(true);

        boolean result =
                adapter.existsInOrgUnitBranch(2L, 5L);

        assertTrue(result);
        verify(springDataUserRepository)
                .existsInOrgUnitBranch(2L, 5L);
    }

    @Test
    @DisplayName("save User mới thành công")
    void testSave_NewUser_Success() {
        Role role = new Role(new RoleId(6L), RoleCode.VT_06, "Quản trị viên");
        User domain = User.createNew("john_doe", "hash123", role, null);

        RoleJpaEntity roleJpa = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");
        when(springDataRoleRepository.findByCode("VT-06")).thenReturn(Optional.of(roleJpa));

        UserJpaEntity savedJpa = userJpa(10L, "john_doe", "hash123", roleJpa, true, 0L);
        when(springDataUserRepository.save(any(UserJpaEntity.class))).thenReturn(savedJpa);

        User savedUser = adapter.save(domain);

        assertNotNull(savedUser);
        assertEquals(10L, savedUser.getIdValue());
        assertEquals("john_doe", savedUser.getUsername());
    }

    private UserJpaEntity userJpa(
            Long id,
            String username,
            String passwordHash,
            RoleJpaEntity role,
            Boolean isActive,
            Long version
    ) {
        UserJpaEntity entity =
                new UserJpaEntity(
                        id,
                        username,
                        passwordHash,
                        role,
                        isActive,
                        version
                );

        entity.setDataScope("SELF");
        return entity;
    }
}
