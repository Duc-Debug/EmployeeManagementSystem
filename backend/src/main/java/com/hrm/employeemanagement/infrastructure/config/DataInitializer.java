package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.domain.model.role.RoleCode;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SpringDataRoleRepository roleRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataEmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SpringDataRoleRepository roleRepository,
                           SpringDataUserRepository userRepository,
                           SpringDataEmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed default roles (VT-01 -> VT-07)
        for (RoleCode rc : RoleCode.values()) {
            if (roleRepository.findByCode(rc.getCode()).isEmpty()) {
                roleRepository.save(new RoleJpaEntity(null, rc.getCode(), rc.getName()));
            }
        }

        // Seed initial Admin user if not present
        if (userRepository.findByUsername("admin").isEmpty()) {
            RoleJpaEntity adminRole = roleRepository.findByCode("VT-06")
                    .orElseThrow(() -> new IllegalStateException("Role VT-06 (Quản trị viên) chưa tồn tại"));

            UserJpaEntity adminUser = new UserJpaEntity(
                    null,
                    "admin",
                    passwordEncoder.encode("admin123"),
                    adminRole,
                    true
            );
            UserJpaEntity savedAdmin = userRepository.save(adminUser);

            EmployeeJpaEntity adminEmployee = new EmployeeJpaEntity(
                    null,
                    savedAdmin.getId(),
                    1L,
                    "EMP-ADMIN",
                    "Quản trị viên hệ thống",
                    false,
                    40,
                    "ACTIVE"
            );
            employeeRepository.save(adminEmployee);
        }
    }
}
