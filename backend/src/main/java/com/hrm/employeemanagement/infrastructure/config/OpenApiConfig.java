package com.hrm.employeemanagement.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình OpenAPI 3.0 & Swagger UI (Feedback P2-9).
 * Cung cấp tài liệu tra cứu cho toàn bộ ~245 endpoints của hệ thống Quản trị Nhân sự.
 * Tích hợp cơ chế xác thực JWT Bearer Token toàn cục.
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
        ),
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
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
}
