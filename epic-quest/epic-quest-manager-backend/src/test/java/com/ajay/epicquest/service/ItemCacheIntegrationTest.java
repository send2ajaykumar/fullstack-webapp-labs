package com.ajay.epicquest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.ajay.epicquest.dto.ItemDto;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.enums.Rarity;
import com.ajay.epicquest.repository.ItemRepository;
import com.ajay.epicquest.service.interfaces.ItemService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ItemCacheIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ItemRepository realItemRepository;

    @MockitoSpyBean
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache("itemsList");
        if (cache != null) {
            cache.clear();
        }
        realItemRepository.deleteAll();

        realItemRepository.save(Item.builder()
                .name("Cached Sword")
                .description("for cache tests")
                .category("Weapon")
                .powerValue(10)
                .rarity(Rarity.COMMON)
                .build());

        reset(itemRepository);
    }

    @Test
    void getItems_usesCacheForIdenticalRequests() {
        itemService.getItems("Weapon", Rarity.COMMON, 0, 10);
        itemService.getItems("Weapon", Rarity.COMMON, 0, 10);

        verify(itemRepository, times(1))
                .findItemsWithFilters(eq("Weapon"), eq(Rarity.COMMON), any(Pageable.class));
    }

    @Test
    void createItem_invalidatesItemsListCache() {
        itemService.getItems("Weapon", Rarity.COMMON, 0, 10);
        verify(itemRepository, times(1))
                .findItemsWithFilters(eq("Weapon"), eq(Rarity.COMMON), any(Pageable.class));

        ItemDto dto = new ItemDto();
        dto.setName("New Dagger");
        dto.setDescription("new item invalidates cache");
        dto.setCategory("Weapon");
        dto.setPowerValue(6);
        dto.setRarity(Rarity.COMMON);
        itemService.createItem(dto);

        reset(itemRepository);
        itemService.getItems("Weapon", Rarity.COMMON, 0, 10);
        itemService.getItems("Weapon", Rarity.COMMON, 0, 10);

        verify(itemRepository, times(1))
                .findItemsWithFilters(eq("Weapon"), eq(Rarity.COMMON), any(Pageable.class));
        assertNotNull(cacheManager.getCache("itemsList"));
    }
}