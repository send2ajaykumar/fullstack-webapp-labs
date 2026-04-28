package com.ajay.epicquest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.ajay.epicquest.model.enums.Rarity;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroAcceptedQuestDto {
    private Long questId;
    private String title;
    private int difficultyLevel;
    private Rarity requiredRarity;
    private LocalDateTime acceptedAt;
}