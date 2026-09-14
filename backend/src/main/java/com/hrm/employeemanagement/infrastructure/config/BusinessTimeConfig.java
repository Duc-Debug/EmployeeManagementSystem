package com.hrm.employeemanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BusinessTimeConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock businessClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
