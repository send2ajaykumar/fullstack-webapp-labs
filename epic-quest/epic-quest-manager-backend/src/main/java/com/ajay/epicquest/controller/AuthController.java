package com.ajay.epicquest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ajay.epicquest.dto.LoginRequest;
import com.ajay.epicquest.dto.RegisterRequest;
import com.ajay.epicquest.service.interfaces.AuthService;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
/**
 * Exposes the public authentication boundary of the system and keeps request/response
 * contracts small so clients do not couple to persistence models.
 */
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Creates a PLAYER account by default. If an authenticated admin calls this endpoint and sends role=ADMIN, an ADMIN account is created.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User account registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data, invalid role, or username already exists"),
        @ApiResponse(responseCode = "401", description = "Invalid bearer token if authorization header is provided"),
        @ApiResponse(responseCode = "403", description = "Only authenticated admins may register ADMIN users")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT access token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful, returns JWT token"),
        @ApiResponse(responseCode = "400", description = "Invalid username or password")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
