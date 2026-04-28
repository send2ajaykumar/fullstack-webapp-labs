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

import com.ajay.epicquest.dto.HeroAcceptedQuestDto;
import com.ajay.epicquest.dto.HeroDto;
import com.ajay.epicquest.dto.HeroInventoryDto;
import com.ajay.epicquest.dto.HeroResponse;
import com.ajay.epicquest.dto.QuestAcceptanceResponse;
import com.ajay.epicquest.dto.QuestResponse;
import com.ajay.epicquest.model.Hero;
import com.ajay.epicquest.model.Quest;
import com.ajay.epicquest.model.QuestAcceptance;
import com.ajay.epicquest.service.interfaces.HeroService;
import com.ajay.epicquest.service.interfaces.QuestService;

import java.util.List;

@RestController
@RequestMapping("/heroes")
@RequiredArgsConstructor
@Tag(name = "Heroes", description = "Hero management and inventory endpoints")
/**
 * Exposes hero-centric workflows, including inventory management and the nested
 * quest-accept route required by the assignment.
 */
public class HeroController {

    private final HeroService heroService;
    private final QuestService questService;

    // Hero responses intentionally expose owner_user_id instead of the nested owner entity.
    private HeroResponse toResponse(Hero hero) {
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
                .hero(toResponse(acceptance.getHero()))
                .acceptedAt(acceptance.getAcceptedAt())
                .build();
    }

    // Nested quest responses are mapped to DTOs to avoid leaking owner/password data.
    private QuestResponse toQuestResponse(Quest quest) {
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
    @Operation(summary = "Create a new hero", description = "Creates a new hero and associates it with the authenticated user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Hero created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<HeroResponse> createHero(@Valid @RequestBody HeroDto dto) {
        return ResponseEntity.status(201).body(toResponse(heroService.createHero(dto)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hero by ID", description = "Retrieves a hero by ID, including their inventory items. Public access.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hero retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Hero not found")
    })
    public ResponseEntity<HeroResponse> getHero(@Parameter(description = "Hero ID") @PathVariable Long id) {
        return ResponseEntity.ok(toResponse(heroService.getHero(id)));
    }

    @PostMapping("/{heroId}/inventory")
    @Operation(summary = "Add item to hero inventory", description = "Adds an item to a hero's inventory. Hero cannot have more than 3 items. Owner or admin access required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item added to inventory successfully"),
        @ApiResponse(responseCode = "400", description = "Hero already has 3 items or item not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - only hero owner or admin can modify inventory"),
        @ApiResponse(responseCode = "404", description = "Hero or item not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<HeroResponse> addItem(@Parameter(description = "Hero ID") @PathVariable Long heroId, @Valid @RequestBody HeroInventoryDto dto) {
        return ResponseEntity.ok(toResponse(heroService.addItemToHero(heroId, dto.getItemId())));
    }

    @DeleteMapping("/{heroId}/inventory/{itemId}")
    @Operation(summary = "Remove item from hero inventory", description = "Removes an item from a hero's inventory. Owner or admin access required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Item removed from inventory successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - only hero owner or admin can modify inventory"),
        @ApiResponse(responseCode = "404", description = "Hero or item not found, or item not in hero's inventory")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> removeItem(@Parameter(description = "Hero ID") @PathVariable Long heroId, @Parameter(description = "Item ID") @PathVariable Long itemId) {
        heroService.removeItemFromHero(heroId, itemId);
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/{heroId}/quests/accepted")
    @Operation(summary = "Get accepted quests for a hero", description = "Returns quests accepted by a hero with acceptance timestamps. Owner or admin access required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Accepted quests retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - only hero owner or admin can view accepted quests"),
        @ApiResponse(responseCode = "404", description = "Hero not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<HeroAcceptedQuestDto>> getAcceptedQuests(@Parameter(description = "Hero ID") @PathVariable Long heroId) {
        return ResponseEntity.ok(questService.getAcceptedQuestsByHero(heroId));
    }

    @PostMapping("/{heroId}/quests/{questId}/accept")
    @Operation(summary = "Accept a quest for a hero", description = "Hero accepts a quest if they meet the rarity requirements. Owner or admin access required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quest accepted successfully"),
        @ApiResponse(responseCode = "400", description = "Hero does not meet rarity requirements or already accepted quest"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - only hero owner or admin can accept quests"),
        @ApiResponse(responseCode = "404", description = "Quest or hero not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QuestResponse> acceptQuestForHero(
            @Parameter(description = "Hero ID") @PathVariable Long heroId,
            @Parameter(description = "Quest ID") @PathVariable Long questId) {
        return ResponseEntity.ok(toQuestResponse(questService.acceptQuest(questId, heroId)));
    }
}
