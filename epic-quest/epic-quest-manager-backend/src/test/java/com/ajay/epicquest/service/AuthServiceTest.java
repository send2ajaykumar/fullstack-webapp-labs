package com.ajay.epicquest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ajay.epicquest.dto.AuthResponse;
import com.ajay.epicquest.dto.LoginRequest;
import com.ajay.epicquest.dto.RegisterRequest;
import com.ajay.epicquest.dto.RegisterResponse;
import com.ajay.epicquest.exception.BadRequestException;
import com.ajay.epicquest.exception.ForbiddenException;
import com.ajay.epicquest.exception.UserAlreadyExistsException;
import com.ajay.epicquest.model.User;
import com.ajay.epicquest.model.enums.Role;
import com.ajay.epicquest.repository.UserRepository;
import com.ajay.epicquest.security.JwtProperties;
import com.ajay.epicquest.security.JwtUtil;
import com.ajay.epicquest.service.impl.AuthServiceImpl;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        user = User.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("hashedpassword")
                .role(Role.PLAYER)
                .build();
    }

    @Test
    void testRegister_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("PLAYER", response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_AdminRoleRequiresAdminCaller() {
        registerRequest.setRole("ADMIN");

        assertThrows(ForbiddenException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_AdminCanCreateAdminUser() {
        registerRequest.setRole("ADMIN");

        User adminCaller = User.builder()
                .id(99L)
                .username("admin")
                .passwordHash("hashedpassword")
                .role(Role.ADMIN)
                .build();

        User adminUser = User.builder()
                .id(2L)
                .username("testuser")
                .passwordHash("hashedpassword")
                .role(Role.ADMIN)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of())
        );

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminCaller));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        RegisterResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("ADMIN", response.getRole());
        verify(userRepository).save(argThat(saved -> saved.getRole() == Role.ADMIN));
    }

    @Test
    void testRegister_InvalidRole() {
        registerRequest.setRole("SUPERADMIN");

        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_UserAlreadyExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));
    }

    @Test
    void testLogin_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser", "PLAYER")).thenReturn("token123");
        when(jwtProperties.getExpiration()).thenReturn(3600000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("token123", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
    }

    @Test
    void testLogin_UserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
    }

    @Test
    void testLogin_InvalidPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
    }
}
