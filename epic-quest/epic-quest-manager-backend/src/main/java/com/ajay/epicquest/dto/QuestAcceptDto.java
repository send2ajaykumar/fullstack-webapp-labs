package com.ajay.epicquest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestAcceptDto {
    @NotNull(message = "heroId is required")
    private Long heroId;
}