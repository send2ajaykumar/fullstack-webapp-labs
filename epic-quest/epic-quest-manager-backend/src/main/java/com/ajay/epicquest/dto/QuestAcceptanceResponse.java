package com.ajay.epicquest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuestAcceptanceResponse {

    private Long id;

    private HeroResponse hero;

    private LocalDateTime acceptedAt;
}