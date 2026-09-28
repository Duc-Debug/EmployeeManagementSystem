package com.hrm.employeemanagement.infrastructure.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("local")
public class LocalEnvironmentValidationConfig {

    @Bean
    public static BeanFactoryPostProcessor validateLocalEnvironment(
            Environment environment
    ) {
        return beanFactory -> {
            List<String> missingVariables =
                    new ArrayList<>();

            require(
                    environment,
                    missingVariables,
                    "SPRING_DATASOURCE_USERNAME"
            );

            require(
                    environment,
                    missingVariables,
                    "SPRING_DATASOURCE_PASSWORD"
            );

            require(
                    environment,
                    missingVariables,
                    "JWT_SECRET"
            );

            boolean adminEnabled = Boolean.parseBoolean(
                    environment.getProperty(
                            "INITIAL_ADMIN_ENABLED",
                            environment.getProperty("app.security.initial-admin.enabled", "false")
                    )
            );
            if (adminEnabled) {
                String adminPassword = environment.getProperty("INITIAL_ADMIN_PASSWORD");
                if (adminPassword == null || adminPassword.isBlank()) {
                    adminPassword = environment.getProperty("app.security.initial-admin.password");
                }
                if (adminPassword == null || adminPassword.isBlank()) {
                    missingVariables.add("INITIAL_ADMIN_PASSWORD");
                }
            }

            if (!missingVariables.isEmpty()) {
                throw new IllegalStateException(
                        "Missing required local environment variables: "
                                + String.join(
                                        ", ",
                                        missingVariables
                                )
                                + ". Define them in EmployeeManagementSystem/.env "
                                + "or in the shell environment."
                );
            }
        };
    }

    private static void require(
            Environment environment,
            List<String> missingVariables,
            String name
    ) {
        String value =
                environment.getProperty(name);

        if (value == null || value.isBlank()) {
            missingVariables.add(name);
        }
    }
}
