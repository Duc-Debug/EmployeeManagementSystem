package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.DepartmentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataDepartmentRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataInitializerTest {

    @Mock
    private SpringDataRoleRepository roleRepository;

    @Mock
    private SpringDataDepartmentRepository departmentRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataEmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(roleRepository, departmentRepository, userRepository, employeeRepository, passwordEncoder);
        dataInitializer.setInitialAdminEnabled(true);
        dataInitializer.setInitialAdminUsername("admin");

        lenient().when(roleRepository.findByCode(any(String.class))).thenReturn(Optional.of(new RoleJpaEntity(1L, "VT-01", "Vai tro")));
        lenient().when(departmentRepository.findByCode("PB-01")).thenReturn(Optional.of(new DepartmentJpaEntity(1L, "PB-01", "Ban giám đốc", null)));
    }

    @Test
    @DisplayName("Khởi tạo Admin & Employee liên kết Department thành công khi cung cấp password từ biến môi trường")
    void testRun_ProvisionsAdminAndEmployeeWithEnvPassword() throws Exception {
        dataInitializer.setInitialAdminPassword("StrongEnvPassword123!");

        RoleJpaEntity adminRole = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");
        when(roleRepository.findByCode("VT-06")).thenReturn(Optional.of(adminRole));
        when(userRepository.countActiveAdmins()).thenReturn(0L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongEnvPassword123!")).thenReturn("encoded_strong_password");

        UserJpaEntity savedUser = new UserJpaEntity(1L, "admin", "encoded_strong_password", adminRole, true);
        when(userRepository.save(any(UserJpaEntity.class))).thenReturn(savedUser);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        dataInitializer.run();

        ArgumentCaptor<UserJpaEntity> userCaptor = ArgumentCaptor.forClass(UserJpaEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("admin", userCaptor.getValue().getUsername());
        assertEquals("encoded_strong_password", userCaptor.getValue().getPasswordHash());
        assertTrue(userCaptor.getValue().getIsActive());

        ArgumentCaptor<EmployeeJpaEntity> empCaptor = ArgumentCaptor.forClass(EmployeeJpaEntity.class);
        verify(employeeRepository).save(empCaptor.capture());
        assertEquals(1L, empCaptor.getValue().getUserId());
        assertEquals(1L, empCaptor.getValue().getDepartmentId());
    }

    @Test
    @DisplayName("Khởi tạo Admin tự động sinh mật khẩu ngẫu nhiên khi không cung cấp password")
    void testRun_ProvisionsAdminWithRandomPasswordWhenNotProvided() throws Exception {
        dataInitializer.setInitialAdminPassword(""); // Không truyền password

        RoleJpaEntity adminRole = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");
        when(roleRepository.findByCode("VT-06")).thenReturn(Optional.of(adminRole));
        when(userRepository.countActiveAdmins()).thenReturn(0L);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded_random_password");

        UserJpaEntity savedUser = new UserJpaEntity(1L, "admin", "encoded_random_password", adminRole, true);
        when(userRepository.save(any(UserJpaEntity.class))).thenReturn(savedUser);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        dataInitializer.run();

        verify(passwordEncoder).encode(any(String.class));
        verify(userRepository).save(any(UserJpaEntity.class));
        verify(employeeRepository).save(any(EmployeeJpaEntity.class));
    }

    @Test
    @DisplayName("Tự phục hồi (Self-Healing): Khi Admin User đã tồn tại nhưng Employee Profile bị thiếu, hệ thống tự động tạo Employee")
    void testRun_SelfHealing_CreatesMissingEmployeeWhenUserExists() throws Exception {
        RoleJpaEntity adminRole = new RoleJpaEntity(6L, "VT-06", "Quản trị viên");
        UserJpaEntity existingAdmin = new UserJpaEntity(1L, "admin", "encoded_hash", adminRole, true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty()); // Thiếu employee

        dataInitializer.run();

        // Không tạo lại User
        verify(userRepository, never()).save(any(UserJpaEntity.class));
        // Nhưng tạo bù Employee profile và liên kết Department
        ArgumentCaptor<EmployeeJpaEntity> empCaptor = ArgumentCaptor.forClass(EmployeeJpaEntity.class);
        verify(employeeRepository).save(empCaptor.capture());
        assertEquals(1L, empCaptor.getValue().getUserId());
        assertEquals(1L, empCaptor.getValue().getDepartmentId());
    }

    @Test
    @DisplayName("Bỏ qua khởi tạo khi initialAdminEnabled = false")
    void testRun_SkipsProvisioningWhenDisabled() throws Exception {
        dataInitializer.setInitialAdminEnabled(false);

        dataInitializer.run();

        verify(userRepository, never()).save(any(UserJpaEntity.class));
        verify(employeeRepository, never()).save(any(EmployeeJpaEntity.class));
    }
}
