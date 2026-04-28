package com.ajay.epicquest.dto;

import com.ajay.epicquest.model.enums.Rarity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemDto {

    @NotBlank(message = "Item name is required")
    private String name;

    private String description;

    @NotBlank(message = "Item category is required")
    private String category;

    @Min(value = 0, message = "power_value must be >= 0")
    private int powerValue;

    @NotNull(message = "Item rarity is required")
    private Rarity rarity;
}
