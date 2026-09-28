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

            if (environment.getProperty(
                    "INITIAL_ADMIN_ENABLED",
                    Boolean.class,
                    false
            )) {
                require(
                        environment,
                        missingVariables,
                        "INITIAL_ADMIN_PASSWORD"
                );
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
