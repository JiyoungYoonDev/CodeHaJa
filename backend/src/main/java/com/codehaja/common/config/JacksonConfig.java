package com.codehaja.common.config;

import com.codehaja.common.security.XssStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule xssProtectionModule() {
        SimpleModule module = new SimpleModule("XssProtection");
        module.addDeserializer(String.class, new XssStringDeserializer());
        return module;
    }
}
