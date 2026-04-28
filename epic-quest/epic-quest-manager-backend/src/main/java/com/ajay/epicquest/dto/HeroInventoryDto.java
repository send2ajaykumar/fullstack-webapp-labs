package com.ajay.epicquest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HeroInventoryDto {
    @NotNull(message = "itemId is required")
    private Long itemId;
}
