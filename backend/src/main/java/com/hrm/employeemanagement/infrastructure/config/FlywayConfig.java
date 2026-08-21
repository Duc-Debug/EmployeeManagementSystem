package com.hrm.employeemanagement.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        System.out.println("==================================================");
        System.out.println("🚀 FLYWAY STARTING DATABASE MIGRATION...");
        System.out.println("==================================================");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .locations("classpath:db/migration")
                .load();
        
        try {
            flyway.repair();
            flyway.migrate();
        } catch (Exception e) {
            System.out.println("⚠️ Migration mismatch detected during dev, cleaning schema...");
            flyway.clean();
            flyway.migrate();
        }
        
        System.out.println("==================================================");
        System.out.println("✅ FLYWAY MIGRATION SUCCESSFUL!");
        System.out.println("==================================================");
        return flyway;
    }
}
