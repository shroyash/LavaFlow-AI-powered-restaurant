package com.lavaflow.common.init;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.repository.UserRepository;
import com.lavaflow.common.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminInitializer implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final SuperAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            return;
        }

        if (isBlank(properties.getEmail()) || isBlank(properties.getPassword())) {
            log.warn("No super admin exists. Set lavaflow.bootstrap.super-admin.email and .password to create one.");
            return;
        }

        String password = properties.getPassword();
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            log.error("Super admin not created: the password must be {}-{} characters.",
                    MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH);
            return;
        }

        String email = properties.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            log.error("Super admin not created: {} already belongs to another account.", email);
            return;
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setFullName(properties.getFullName().trim());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.SUPER_ADMIN);
        admin.setRestaurant(null);
        admin.setActive(true);
        admin.setEmailVerified(true);

        try {
            userRepository.saveAndFlush(admin);
            log.info("Super admin created for {}. Change the password after the first login.", email);
        } catch (DataIntegrityViolationException e) {
            log.info("Super admin was created by another instance.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}