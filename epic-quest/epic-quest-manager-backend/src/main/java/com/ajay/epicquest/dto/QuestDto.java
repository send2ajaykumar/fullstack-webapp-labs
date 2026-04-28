package com.ajay.epicquest.dto;

import com.ajay.epicquest.model.enums.Rarity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestDto {

    @NotBlank(message = "Quest title is required")
    private String title;

    private String description;

    @Min(value = 1, message = "Difficulty level must be >= 1")
    private int difficultyLevel;

    @NotNull(message = "Required rarity is required")
    private Rarity requiredRarity;

    private Long rewardItemId;
}
