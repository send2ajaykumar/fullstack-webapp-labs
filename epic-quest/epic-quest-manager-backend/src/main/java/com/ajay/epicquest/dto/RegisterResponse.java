package com.ajay.epicquest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {

    @JsonProperty("user_id")
    private Long userId;

    private String username;

    private String role;
}