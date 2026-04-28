package com.ajay.epicquest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ajay.epicquest.dto.HeroResponse;
import com.ajay.epicquest.dto.QuestAcceptDto;
import com.ajay.epicquest.dto.QuestAcceptanceResponse;
import com.ajay.epicquest.dto.QuestDto;
import com.ajay.epicquest.dto.QuestResponse;
import com.ajay.epicquest.model.Hero;
import com.ajay.epicquest.model.Quest;
import com.ajay.epicquest.model.QuestAcceptance;
import com.ajay.epicquest.service.interfaces.QuestService;

import java.util.List;

@RestController
@RequestMapping("/quests")
@RequiredArgsConstructor
@Tag(name = "Quests", description = "Quest management and acceptance endpoints")
/**
 * Exposes quest creation, listing, and acceptance APIs while translating entity
 * graphs into safe response DTOs for external clients.
 */
public class QuestController {

    private final QuestService questService;

    // Quest responses reuse the safe hero projection to avoid leaking nested owner credentials.
    private HeroResponse toHeroResponse(Hero hero) {
        return HeroResponse.builder()
                .id(hero.getId())
                .name(hero.getName())
                .heroClass(hero.getHeroClass())
                .level(hero.getLevel())
                .ownerUserId(hero.getOwner() != null ? hero.getOwner().getId() : null)
                .items(hero.getItems())
                .createdAt(hero.getCreatedAt())
                .updatedAt(hero.getUpdatedAt())
                .build();
    }

    private QuestAcceptanceResponse toAcceptanceResponse(QuestAcceptance acceptance) {
        return QuestAcceptanceResponse.builder()
                .id(acceptance.getId())
                .hero(toHeroResponse(acceptance.getHero()))
                .acceptedAt(acceptance.getAcceptedAt())
                .build();
    }

    // Quest DTO mapping keeps API contracts stable even if persistence relationships evolve.
    private QuestResponse toResponse(Quest quest) {
        return QuestResponse.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .difficultyLevel(quest.getDifficultyLevel())
                .requiredRarity(quest.getRequiredRarity())
                .rewardItem(quest.getRewardItem())
                .acceptedBy(quest.getAcceptedBy() == null ? null : quest.getAcceptedBy().stream()
                        .map(this::toAcceptanceResponse)
                        .toList())
                .createdAt(quest.getCreatedAt())
                .updatedAt(quest.getUpdatedAt())
                .build();
    }

    @PostMapping
    @Operation(summary = "Create a new quest", description = "Creates a new quest with specified requirements. Admin role required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Quest created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QuestResponse> createQuest(@Valid @RequestBody QuestDto dto) {
        return ResponseEntity.status(201).body(toResponse(questService.createQuest(dto)));
    }

    @GetMapping
    @Operation(summary = "Get all quests", description = "Retrieves all available quests. Public access.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quests retrieved successfully")
    })
    public ResponseEntity<List<QuestResponse>> getAllQuests(
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Number of quests per page") @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(questService.getAllQuests(page, limit).stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping("/{questId}/accept")
    @Operation(summary = "Accept a quest", description = "Hero accepts a quest if they meet the rarity requirements. Owner or admin access required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quest accepted successfully"),
        @ApiResponse(responseCode = "400", description = "Hero does not meet rarity requirements or already accepted quest"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - only hero owner or admin can accept quests"),
        @ApiResponse(responseCode = "404", description = "Quest or hero not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QuestResponse> acceptQuest(@Parameter(description = "Quest ID") @PathVariable Long questId, @Valid @RequestBody QuestAcceptDto dto) {
        return ResponseEntity.ok(toResponse(questService.acceptQuest(questId, dto.getHeroId())));
    }
}
