package com.hrm.employeemanagement.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LocalEnvironmentValidationConfigTest {

    private final ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);

    @Test
    @DisplayName("Ném IllegalStateException khi thiếu INITIAL_ADMIN_PASSWORD và initial-admin được bật ngầm định")
    void testValidateLocalEnvironment_MissingAdminPassword_ThrowsException() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATASOURCE_USERNAME", "root");
        environment.setProperty("SPRING_DATASOURCE_PASSWORD", "secret");
        environment.setProperty("JWT_SECRET", "super-secret-key-that-is-at-least-32-chars-long");
        environment.setProperty("app.security.initial-admin.enabled", "true");

        BeanFactoryPostProcessor postProcessor = LocalEnvironmentValidationConfig.validateLocalEnvironment(environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> postProcessor.postProcessBeanFactory(beanFactory));
        assertTrue(ex.getMessage().contains("INITIAL_ADMIN_PASSWORD"));
    }

    @Test
    @DisplayName("Ném IllegalStateException khi INITIAL_ADMIN_ENABLED=true và thiếu INITIAL_ADMIN_PASSWORD")
    void testValidateLocalEnvironment_ExplicitAdminEnabled_MissingPassword_ThrowsException() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATASOURCE_USERNAME", "root");
        environment.setProperty("SPRING_DATASOURCE_PASSWORD", "secret");
        environment.setProperty("JWT_SECRET", "super-secret-key-that-is-at-least-32-chars-long");
        environment.setProperty("INITIAL_ADMIN_ENABLED", "true");

        BeanFactoryPostProcessor postProcessor = LocalEnvironmentValidationConfig.validateLocalEnvironment(environment);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> postProcessor.postProcessBeanFactory(beanFactory));
        assertTrue(ex.getMessage().contains("INITIAL_ADMIN_PASSWORD"));
    }

    @Test
    @DisplayName("Thành công khi cấu hình đầy đủ biến môi trường")
    void testValidateLocalEnvironment_AllVariablesPresent_Success() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATASOURCE_USERNAME", "root");
        environment.setProperty("SPRING_DATASOURCE_PASSWORD", "secret");
        environment.setProperty("JWT_SECRET", "super-secret-key-that-is-at-least-32-chars-long");
        environment.setProperty("app.security.initial-admin.enabled", "true");
        environment.setProperty("INITIAL_ADMIN_PASSWORD", "AdminSecure123!");

        BeanFactoryPostProcessor postProcessor = LocalEnvironmentValidationConfig.validateLocalEnvironment(environment);

        assertDoesNotThrow(() -> postProcessor.postProcessBeanFactory(beanFactory));
    }

    @Test
    @DisplayName("Không yêu cầu INITIAL_ADMIN_PASSWORD khi initial admin bị tắt")
    void testValidateLocalEnvironment_AdminDisabled_DoesNotRequirePassword() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("SPRING_DATASOURCE_USERNAME", "root");
        environment.setProperty("SPRING_DATASOURCE_PASSWORD", "secret");
        environment.setProperty("JWT_SECRET", "super-secret-key-that-is-at-least-32-chars-long");
        environment.setProperty("INITIAL_ADMIN_ENABLED", "false");
        environment.setProperty("app.security.initial-admin.enabled", "false");

        BeanFactoryPostProcessor postProcessor = LocalEnvironmentValidationConfig.validateLocalEnvironment(environment);

        assertDoesNotThrow(() -> postProcessor.postProcessBeanFactory(beanFactory));
    }
}
