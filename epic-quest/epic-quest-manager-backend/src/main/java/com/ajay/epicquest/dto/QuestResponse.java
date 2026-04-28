package com.ajay.epicquest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.enums.Rarity;

@Data
@Builder
public class QuestResponse {

    private Long id;

    private String title;

    private String description;

    private int difficultyLevel;

    private Rarity requiredRarity;

    private Item rewardItem;

    private List<QuestAcceptanceResponse> acceptedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}