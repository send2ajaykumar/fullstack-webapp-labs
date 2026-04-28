package com.ajay.epicquest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.ajay.epicquest.dto.QuestDto;
import com.ajay.epicquest.exception.BadRequestException;
import com.ajay.epicquest.exception.ResourceNotFoundException;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.Quest;
import com.ajay.epicquest.model.enums.Rarity;
import com.ajay.epicquest.repository.HeroRepository;
import com.ajay.epicquest.repository.ItemRepository;
import com.ajay.epicquest.repository.QuestAcceptanceRepository;
import com.ajay.epicquest.repository.QuestRepository;
import com.ajay.epicquest.repository.UserRepository;
import com.ajay.epicquest.service.impl.QuestServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @Mock
    private QuestRepository questRepository;

    @Mock
    private HeroRepository heroRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private QuestAcceptanceRepository questAcceptanceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuestServiceImpl questService;

    private Quest quest;
    private Item rewardItem;
    private QuestDto questDto;

    @BeforeEach
    void setUp() {
        rewardItem = Item.builder()
                .id(1L)
                .name("Gold Coin")
                .category("Currency")
                .rarity(Rarity.COMMON)
                .build();

        questDto = new QuestDto();
        questDto.setTitle("Slay the Dragon");
        questDto.setDescription("Defeat the dragon in the cave");
        questDto.setDifficultyLevel(5);
        questDto.setRequiredRarity(Rarity.RARE);
        questDto.setRewardItemId(1L);

        quest = Quest.builder()
                .id(1L)
                .title("Slay the Dragon")
                .description("Defeat the dragon in the cave")
                .difficultyLevel(5)
                .requiredRarity(Rarity.RARE)
                .rewardItem(rewardItem)
                .build();
    }

    @Test
    void testCreateQuest_WithRewardItem_Success() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(rewardItem));
        when(questRepository.save(any(Quest.class))).thenReturn(quest);

        Quest created = questService.createQuest(questDto);

        assertNotNull(created);
        assertEquals("Slay the Dragon", created.getTitle());
        assertEquals(rewardItem.getId(), created.getRewardItem().getId());
        verify(questRepository).save(any(Quest.class));
    }

    @Test
    void testCreateQuest_RewardItemNotFound() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        QuestDto invalidDto = new QuestDto();
        invalidDto.setTitle("Quest");
        invalidDto.setRewardItemId(999L);

        assertThrows(ResourceNotFoundException.class, () -> questService.createQuest(invalidDto));
    }

    @Test
    void testGetAllQuests() {
        List<Quest> quests = new ArrayList<>();
        quests.add(quest);
        
        when(questRepository.findAll()).thenReturn(quests);

        List<Quest> found = questService.getAllQuests(null, null);

        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals("Slay the Dragon", found.get(0).getTitle());
    }

    @Test
    void testGetAllQuests_WithPagination() {
        List<Quest> quests = List.of(quest);
        when(questRepository.findAll(PageRequest.of(0, 1))).thenReturn(new PageImpl<>(quests));

        List<Quest> found = questService.getAllQuests(0, 1);

        assertNotNull(found);
        assertEquals(1, found.size());
        verify(questRepository).findAll(PageRequest.of(0, 1));
    }

    @Test
    void testGetAllQuests_InvalidPagination() {
        assertThrows(BadRequestException.class, () -> questService.getAllQuests(0, null));
        assertThrows(BadRequestException.class, () -> questService.getAllQuests(null, 10));
        assertThrows(BadRequestException.class, () -> questService.getAllQuests(-1, 10));
        assertThrows(BadRequestException.class, () -> questService.getAllQuests(0, 0));
    }
}
