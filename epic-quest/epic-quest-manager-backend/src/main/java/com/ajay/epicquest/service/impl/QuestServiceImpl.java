package com.ajay.epicquest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ajay.epicquest.dto.HeroAcceptedQuestDto;
import com.ajay.epicquest.dto.QuestDto;
import com.ajay.epicquest.exception.BadRequestException;
import com.ajay.epicquest.exception.ForbiddenException;
import com.ajay.epicquest.exception.ResourceNotFoundException;
import com.ajay.epicquest.model.Hero;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.Quest;
import com.ajay.epicquest.model.QuestAcceptance;
import com.ajay.epicquest.model.User;
import com.ajay.epicquest.model.enums.Role;
import com.ajay.epicquest.repository.HeroRepository;
import com.ajay.epicquest.repository.ItemRepository;
import com.ajay.epicquest.repository.QuestAcceptanceRepository;
import com.ajay.epicquest.repository.QuestRepository;
import com.ajay.epicquest.repository.UserRepository;
import com.ajay.epicquest.service.interfaces.QuestService;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Manages quest creation, listing, and acceptance while enforcing rarity,
 * ownership/admin authorization, and duplicate-acceptance rules.
 */
public class QuestServiceImpl implements QuestService {

    private final QuestRepository questRepository;
    private final QuestAcceptanceRepository questAcceptanceRepository;
    private final HeroRepository heroRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    @Override
    public Quest createQuest(QuestDto dto) {
        Item rewardItem = null;
        if (dto.getRewardItemId() != null) {
            rewardItem = itemRepository.findById(dto.getRewardItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reward item not found"));
        }

        Quest quest = Quest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .difficultyLevel(dto.getDifficultyLevel())
                .requiredRarity(dto.getRequiredRarity())
                .rewardItem(rewardItem)
                .build();

        return questRepository.save(quest);
    }

    @Override
    public List<Quest> getAllQuests(Integer page, Integer limit) {
        if (page == null && limit == null) {
            return questRepository.findAll();
        }

        if (page == null || limit == null || page < 0 || limit <= 0) {
            throw new BadRequestException("Pagination requires valid page (>= 0) and limit (> 0)");
        }

        Pageable pageable = PageRequest.of(page, limit);
        return questRepository.findAll(pageable).getContent();
    }

    @Override
    public Quest acceptQuest(Long questId, Long heroId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));

        Hero hero = heroRepository.findById(heroId)
                .orElseThrow(() -> new ResourceNotFoundException("Hero not found"));

        // A hero qualifies if any equipped item meets or exceeds the quest rarity threshold.
        boolean hasRequiredItem = hero.getItems().stream()
                .map(Item::getRarity)
                .anyMatch(r -> r.ordinal() >= quest.getRequiredRarity().ordinal());

        if (!hasRequiredItem) {
            throw new BadRequestException("Hero does not meet rarity requirement");
        }

        User currentUser = getAuthenticatedUser();
        if (!(currentUser.getRole() == Role.ADMIN || hero.getOwner().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("Not authorized to accept this quest for this hero");
        }

        // The same hero cannot accept the same quest more than once.
        boolean alreadyAccepted = quest.getAcceptedBy().stream()
                .anyMatch(acceptance -> acceptance.getHero().getId().equals(heroId));

        if (alreadyAccepted) {
            throw new BadRequestException("Hero has already accepted this quest");
        }

        QuestAcceptance acceptance = QuestAcceptance.builder()
                .quest(quest)
                .hero(hero)
                .build();

        quest.getAcceptedBy().add(acceptance);
        return questRepository.save(quest);
    }

    @Override
    public List<HeroAcceptedQuestDto> getAcceptedQuestsByHero(Long heroId) {
        Hero hero = heroRepository.findById(heroId)
                .orElseThrow(() -> new ResourceNotFoundException("Hero not found"));

        User currentUser = getAuthenticatedUser();
        if (!(currentUser.getRole() == Role.ADMIN || hero.getOwner().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("Not authorized to view accepted quests for this hero");
        }

        return questAcceptanceRepository.findByHeroIdOrderByAcceptedAtDesc(heroId)
                .stream()
                .map(acceptance -> HeroAcceptedQuestDto.builder()
                        .questId(acceptance.getQuest().getId())
                        .title(acceptance.getQuest().getTitle())
                        .difficultyLevel(acceptance.getQuest().getDifficultyLevel())
                        .requiredRarity(acceptance.getQuest().getRequiredRarity())
                        .acceptedAt(acceptance.getAcceptedAt())
                        .build())
                .toList();
    }
}

