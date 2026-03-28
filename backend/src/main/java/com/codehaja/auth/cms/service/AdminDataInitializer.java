package com.codehaja.auth.cms.service;

import com.codehaja.auth.cms.entity.Admin;
import com.codehaja.auth.cms.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed.email}")
    private String seedEmail;

    @Value("${app.admin.seed.password}")
    private String seedPassword;

    @Value("${app.admin.seed.name}")
    private String seedName;

    @Override
    public void run(String... args) {
        if (adminRepository.count() == 0) {
            Admin admin = Admin.builder()
                    .email(seedEmail)
                    .passwordHash(passwordEncoder.encode(seedPassword))
                    .name(seedName)
                    .failedAttempts(0)
                    .build();
            adminRepository.save(admin);
            log.info("Default admin created: {}", seedEmail);
        }
    }
}
