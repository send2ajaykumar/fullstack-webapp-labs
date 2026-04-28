package com.ajay.epicquest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ajay.epicquest.model.User;
import com.ajay.epicquest.model.enums.Role;
import com.ajay.epicquest.repository.UserRepository;

@Component
public class BootstrapAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${bootstrap.admin.username:}") String username,
            @Value("${bootstrap.admin.password:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasPassword = StringUtils.hasText(password);

        if (!hasUsername && !hasPassword) {
            return;
        }

        if (!hasUsername || !hasPassword) {
            log.warn("Bootstrap admin skipped because both bootstrap.admin.username and bootstrap.admin.password are required");
            return;
        }

        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Bootstrap admin skipped because an admin user already exists");
            return;
        }

        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Bootstrap admin skipped because username '{}' already exists", username);
            return;
        }

        User adminUser = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build();

        userRepository.save(adminUser);
        log.info("Bootstrap admin user '{}' created", username);
    }
}