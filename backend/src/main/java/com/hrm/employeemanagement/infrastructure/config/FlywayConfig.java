package com.hrm.employeemanagement.infrastructure.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Flyway - Nguồn chân lý duy nhất cho Database Schema. Đảm bảo Flyway
 * luôn chạy migration xong trước khi Hibernate tiến hành validate bảng.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.out-of-order:false}") boolean outOfOrder
    ) {
        System.out.println("==================================================");
        System.out.println("🚀 FLYWAY STARTING DATABASE MIGRATION...");
        System.out.println("==================================================");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .outOfOrder(outOfOrder)
                .load();

        // Applied migrations are immutable in production. In local development,
        // repair reconciles checksums and clears failed entries.
        try {
            flyway.repair();
        } catch (Exception e) {
            System.out.println("⚠️ Flyway repair warning: " + e.getMessage());
        }

        flyway.migrate();

        ensureEmployeeSkillsColumns(dataSource);

        System.out.println("==================================================");
        System.out.println("✅ FLYWAY MIGRATION SUCCESSFUL!");
        System.out.println("==================================================");

        return flyway;
    }

    private void ensureEmployeeSkillsColumns(DataSource dataSource) {
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            var meta = conn.getMetaData();
            
            var rs = meta.getColumns(null, null, "employee_skills", null);
            var existingColumns = new java.util.HashSet<String>();
            while (rs.next()) {
                existingColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            rs.close();

            // Try uppercase table name if empty (for some DB engines like H2)
            if (existingColumns.isEmpty()) {
                rs = meta.getColumns(null, null, "EMPLOYEE_SKILLS", null);
                while (rs.next()) {
                    existingColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
                rs.close();
            }

            if (!existingColumns.contains("last_approved_proficiency_level")) {
                stmt.executeUpdate("ALTER TABLE employee_skills ADD COLUMN last_approved_proficiency_level INT NULL");
            }
            if (!existingColumns.contains("last_approved_years_of_experience")) {
                stmt.executeUpdate("ALTER TABLE employee_skills ADD COLUMN last_approved_years_of_experience DECIMAL(4,1) NULL");
            }
            if (!existingColumns.contains("pending_proficiency_level")) {
                stmt.executeUpdate("ALTER TABLE employee_skills ADD COLUMN pending_proficiency_level INT NULL");
            }
            if (!existingColumns.contains("pending_years_of_experience")) {
                stmt.executeUpdate("ALTER TABLE employee_skills ADD COLUMN pending_years_of_experience DECIMAL(4,1) NULL");
            }

            try {
                stmt.executeUpdate("UPDATE employee_skills SET last_approved_proficiency_level = proficiency_level, last_approved_years_of_experience = years_of_experience WHERE status = 'APPROVED' AND last_approved_proficiency_level IS NULL");
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            System.out.println("⚠️ Employee skills column sync warning: " + e.getMessage());
        }
    }

    /**
     * Bắt buộc Hibernate (EntityManagerFactory) phải đợi Bean Flyway hoàn thành
     * trước khi thực hiện kiểm tra cấu trúc bảng (ddl-auto=validate), tránh lỗi
     * khởi động.
     */
    @Bean
    public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                BeanDefinition def
                        = beanFactory.getBeanDefinition("entityManagerFactory");

                String[] existingDependsOn = def.getDependsOn();

                if (existingDependsOn == null || existingDependsOn.length == 0) {
                    def.setDependsOn("flyway");
                } else {
                    String[] newDependsOn
                            = new String[existingDependsOn.length + 1];

                    System.arraycopy(
                            existingDependsOn,
                            0,
                            newDependsOn,
                            0,
                            existingDependsOn.length
                    );

                    newDependsOn[existingDependsOn.length] = "flyway";
                    def.setDependsOn(newDependsOn);
                }
            }
        };
    }
}

