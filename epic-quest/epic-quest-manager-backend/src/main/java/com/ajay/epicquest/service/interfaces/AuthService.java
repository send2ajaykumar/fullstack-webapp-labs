package com.ajay.epicquest.service.interfaces;

import com.ajay.epicquest.dto.AuthResponse;
import com.ajay.epicquest.dto.LoginRequest;
import com.ajay.epicquest.dto.RegisterRequest;
import com.ajay.epicquest.dto.RegisterResponse;

/**
 * Defines authentication use cases for account registration and JWT-based login.
 */
public interface AuthService {
    /**
     * Registers a new user while enforcing the role-creation rules of the system.
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Authenticates a user and returns the access token contract consumed by clients.
     */
    AuthResponse login(LoginRequest request);
}
