package com.researchi.admin.publicform.config;

import com.researchi.admin.publicform.service.PublicFormProtectionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicFormConfig {

    @Bean
    public PublicFormProtectionService publicFormProtectionService(PublicFormProperties properties) {
        return new PublicFormProtectionService(properties);
    }
}
