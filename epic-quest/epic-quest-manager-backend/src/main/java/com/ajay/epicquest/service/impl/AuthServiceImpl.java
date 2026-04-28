package com.ajay.epicquest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
import com.ajay.epicquest.service.interfaces.AuthService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
/**
 * Handles user registration and login while enforcing the assignment's
 * role-creation rules for public users versus admins.
 */
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return Optional.empty();
        }

        return userRepository.findByUsername(username);
    }

    private Role resolveRequestedRole(RegisterRequest request) {
        if (request.getRole() == null || request.getRole().isBlank()) {
            return Role.PLAYER;
        }

        try {
            return Role.valueOf(request.getRole().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Role must be PLAYER or ADMIN");
        }
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        Role requestedRole = resolveRequestedRole(request);
        Optional<User> authenticatedUser = getAuthenticatedUser();

        // Public callers may register only PLAYER accounts; ADMIN creation is restricted.
        if (requestedRole == Role.ADMIN) {
            boolean isAdminCaller = authenticatedUser
                    .map(user -> user.getRole() == Role.ADMIN)
                    .orElse(false);
            if (!isAdminCaller) {
                throw new ForbiddenException("Only authenticated admins can register ADMIN users");
            }
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException(request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(requestedRole)
                .build();

        User saved = userRepository.save(user);
        return new RegisterResponse(saved.getId(), saved.getUsername(), saved.getRole().name());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid username or password");
        }

        // Tokens carry enough identity data for downstream authorization and auditing.
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new AuthResponse(token, "Bearer", jwtProperties.getExpiration() / 1000);
    }
}
