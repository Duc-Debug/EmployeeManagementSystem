package com.hrm.employeemanagement.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Cấu hình Flyway - Nguồn chân lý duy nhất cho Database Schema.
 * Đảm bảo Flyway luôn chạy migration xong trước khi Hibernate tiến hành
 * validate bảng.
 */
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
                .locations("classpath:db/migration")
                .load();
        flyway.repair();
        flyway.migrate();

        System.out.println("==================================================");
        System.out.println("✅ FLYWAY MIGRATION SUCCESSFUL!");
        System.out.println("==================================================");
        return flyway;
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
                BeanDefinition def = beanFactory.getBeanDefinition("entityManagerFactory");
                String[] existingDependsOn = def.getDependsOn();
                if (existingDependsOn == null || existingDependsOn.length == 0) {
                    def.setDependsOn("flyway");
                } else {
                    String[] newDependsOn = new String[existingDependsOn.length + 1];
                    System.arraycopy(existingDependsOn, 0, newDependsOn, 0, existingDependsOn.length);
                    newDependsOn[existingDependsOn.length] = "flyway";
                    def.setDependsOn(newDependsOn);
                }
            }
        };
    }
}
