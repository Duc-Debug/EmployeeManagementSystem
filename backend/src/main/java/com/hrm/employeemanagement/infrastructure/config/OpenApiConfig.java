package com.hrm.employeemanagement.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình OpenAPI 3.0 & Swagger UI (Feedback P2-9).
 * Cung cấp tài liệu tra cứu cho toàn bộ ~245 endpoints của hệ thống Quản trị Nhân sự.
 * Hỗ trợ xác thực JWT Bearer Token, phân định chính xác giữa public API và secured API.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Hệ Thống Quản Lý Nhân Sự & Nguồn Lực (EMS) - REST API",
                version = "1.0.0",
                description = "Tài liệu API đầy đủ cho toàn bộ các chức năng: Xác thực (Auth), Nhân sự (Employees), " +
                              "Cơ cấu tổ chức (OrgUnits), Nghỉ phép (Leaves), Chấm công (Timesheets), " +
                              "Điều phối nguồn lực (Allocations), Dự án (Projects), Công việc (Tasks) và Báo cáo năng lực.",
                contact = @Contact(
                        name = "Đội ngũ Kỹ thuật EMS",
                        email = "dev-support@hrm.local"
                ),
                license = @License(
                        name = "Bản quyền nội bộ CodeGym / Dự án EMS",
                        url = "https://codegym.vn"
                )
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Nhập Access Token nhận được từ API /api/v1/auth/login để thực thi các API yêu cầu quyền hạn."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI();
    }

    @Bean
    public OpenApiCustomizer selectiveSecurityOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    // Không gán security requirement cho các endpoint công khai (auth, health, ...)
                    boolean isPublic = path.startsWith("/api/v1/auth/login")
                            || path.startsWith("/api/v1/auth/forgot-password")
                            || path.startsWith("/api/v1/auth/reset-password")
                            || path.startsWith("/api/v1/public/");

                    if (!isPublic && pathItem.readOperations() != null) {
                        pathItem.readOperations().forEach(operation ->
                                operation.addSecurityItem(
                                        new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth")
                                )
                        );
                    }
                });
            }
        };
    }
}
