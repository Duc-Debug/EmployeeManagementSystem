package com.hrm.employeemanagement.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Explicit Flyway Configuration to enforce Single Source of Truth for Database Schema.
 * Guarantees that Flyway migrations execute and complete before JPA EntityManagerFactory
 * validates the schema with Hibernate (ddl-auto=validate).
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

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
