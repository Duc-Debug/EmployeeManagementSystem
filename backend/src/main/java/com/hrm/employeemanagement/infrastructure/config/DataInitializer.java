package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.DepartmentJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataDepartmentRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import com.hrm.employeemanagement.infrastructure.security.InitialAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database Initializer & Secure Admin Provisioner.
 * Guarantees atomic (@Transactional) and resource-level idempotent bootstrap of
 * roles, root department, and the initial administrator account.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SpringDataRoleRepository roleRepository;
    private final SpringDataDepartmentRepository departmentRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataEmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialAdminProperties initialAdminProperties;

    public DataInitializer(SpringDataRoleRepository roleRepository,
            SpringDataDepartmentRepository departmentRepository,
            SpringDataUserRepository userRepository,
            SpringDataEmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            InitialAdminProperties initialAdminProperties) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialAdminProperties = initialAdminProperties;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Seed system roles (VT-01 -> VT-07) idempotently
        for (RoleCode rc : RoleCode.values()) {
            if (roleRepository.findByCode(rc.getCode()).isEmpty()) {
                roleRepository.save(new RoleJpaEntity(null, rc.getCode(), rc.getName()));
            }
        }

        // 2. Seed default Root Department (PB-01) idempotently
        DepartmentJpaEntity defaultDept = departmentRepository.findByCode("PB-01")
                .orElseGet(
                        () -> departmentRepository.save(new DepartmentJpaEntity(null, "PB-01", "Ban giám đốc", null)));

        // 3. Provision initial admin user and employee profile atomically &
        // idempotently
        if (!initialAdminProperties.enabled()) {
            return;
        }
        if (initialAdminProperties.username() == null || initialAdminProperties.username().isBlank()) {
            throw new IllegalStateException("Initial admin username must be configured when provisioning is enabled");
        }
        if (initialAdminProperties.password() == null || initialAdminProperties.password().isBlank()) {
            throw new IllegalStateException("Initial admin password must be configured when provisioning is enabled");
        }

        provisionInitialAdmin(defaultDept);
    }

    private void provisionInitialAdmin(DepartmentJpaEntity defaultDept) {
        String username = initialAdminProperties.username().trim();
        String rawPassword = initialAdminProperties.password().trim();

        // Find existing admin or create a new one
        UserJpaEntity adminUser = userRepository.findByUsername(username).orElse(null);
        boolean isNewUser = false;

        if (adminUser == null && userRepository.countActiveAdmins() == 0) {
            RoleJpaEntity adminRole = roleRepository.findByCode("VT-06")
                    .orElseThrow(() -> new IllegalStateException(
                            "Role VT-06 (Quản trị viên) chưa tồn tại trong cơ sở dữ liệu"));

            UserJpaEntity newAdmin = new UserJpaEntity(
                    null,
                    username,
                    passwordEncoder.encode(rawPassword),
                    adminRole,
                    true);
            adminUser = userRepository.save(newAdmin);
            isNewUser = true;
        }

        // Self-Healing: Ensure the Admin User always has a linked Employee profile
        if (adminUser != null && employeeRepository.findByUserId(adminUser.getId()).isEmpty()) {
            EmployeeJpaEntity adminEmployee = new EmployeeJpaEntity(
                    null,
                    adminUser.getId(),
                    defaultDept.getId(),
                    "EMP-ADMIN",
                    "Quản trị viên hệ thống",
                    false,
                    40,
                    "ACTIVE");
            employeeRepository.save(adminEmployee);
        }

        // Log warnings/info on initial creation
        if (isNewUser) {
            log.info("Initial admin account '{}' successfully provisioned from environment configuration.", username);
        }
    }
}
