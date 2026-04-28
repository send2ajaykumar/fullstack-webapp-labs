package com.ajay.epicquest.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ajay.epicquest.config.BootstrapAdminInitializer;
import com.ajay.epicquest.model.User;
import com.ajay.epicquest.model.enums.Role;
import com.ajay.epicquest.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void doesNothingWhenBootstrapConfigMissing() throws Exception {
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(userRepository, passwordEncoder, "", "");

        initializer.run();

        verify(userRepository, never()).existsByRole(Role.ADMIN);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void createsAdminWhenConfiguredAndNoAdminExists() throws Exception {
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
                userRepository,
                passwordEncoder,
                "bootstrap-admin",
                "secret123"
        );

        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByUsername("bootstrap-admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("bootstrap-admin", savedUser.getUsername());
        assertEquals("hashed-secret", savedUser.getPasswordHash());
        assertEquals(Role.ADMIN, savedUser.getRole());
    }

    @Test
    void skipsCreationWhenAdminAlreadyExists() throws Exception {
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
                userRepository,
                passwordEncoder,
                "bootstrap-admin",
                "secret123"
        );

        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).findByUsername("bootstrap-admin");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void skipsCreationWhenUsernameAlreadyExists() throws Exception {
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
                userRepository,
                passwordEncoder,
                "bootstrap-admin",
                "secret123"
        );

        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.findByUsername("bootstrap-admin")).thenReturn(Optional.of(User.builder().username("bootstrap-admin").build()));

        initializer.run();

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void skipsCreationWhenConfigIsPartial() throws Exception {
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(userRepository, passwordEncoder, "bootstrap-admin", "");

        initializer.run();

        verify(userRepository, never()).existsByRole(Role.ADMIN);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }
}