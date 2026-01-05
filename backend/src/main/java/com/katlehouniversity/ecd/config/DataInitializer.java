package com.katlehouniversity.ecd.config;

import com.katlehouniversity.ecd.entity.User;
import com.katlehouniversity.ecd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default admin user on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createDefaultAdminUser();
    }

    private void createDefaultAdminUser() {
        String defaultUsername = "admin";

        if (userRepository.existsByUsername(defaultUsername)) {
            log.info("Default admin user already exists");
            return;
        }

        User admin = User.builder()
                .username(defaultUsername)
                .password(passwordEncoder.encode("admin123"))  // Change this in production!
                .fullName("System Administrator")
                .email("admin@katlehouniversity.com")
                .role(User.Role.SUPER_ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("===========================================");
        log.info("Default admin user created successfully");
        log.info("Username: admin");
        log.info("Password: admin123");
        log.info("IMPORTANT: Change the password immediately!");
        log.info("===========================================");
    }
}
