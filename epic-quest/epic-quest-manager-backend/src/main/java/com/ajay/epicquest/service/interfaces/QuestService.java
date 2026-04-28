package com.ajay.epicquest.service.interfaces;

import java.util.List;

import com.ajay.epicquest.dto.HeroAcceptedQuestDto;
import com.ajay.epicquest.dto.QuestDto;
import com.ajay.epicquest.model.Quest;

/**
 * Defines quest lifecycle operations, including creation, listing, acceptance,
 * and retrieval of quest history for a hero.
 */
public interface QuestService {
    Quest createQuest(QuestDto dto);
    List<Quest> getAllQuests(Integer page, Integer limit);

    /**
     * Accepts a quest for a hero when rarity and authorization checks pass.
     */
    Quest acceptQuest(Long questId, Long heroId);

    /**
     * Returns accepted quest history for a hero in reviewer-friendly DTO form.
     */
    List<HeroAcceptedQuestDto> getAcceptedQuestsByHero(Long heroId);
}
