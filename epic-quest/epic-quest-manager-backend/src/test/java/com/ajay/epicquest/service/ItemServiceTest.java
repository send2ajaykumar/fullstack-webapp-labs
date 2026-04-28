package com.ajay.epicquest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ajay.epicquest.dto.ItemDto;
import com.ajay.epicquest.exception.BadRequestException;
import com.ajay.epicquest.exception.ResourceNotFoundException;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.enums.Rarity;
import com.ajay.epicquest.repository.ItemRepository;
import com.ajay.epicquest.service.impl.ItemServiceImpl;

import java.util.List;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private ItemDto itemDto;
    private Item item;
    private List<Item> itemList;

    @BeforeEach
    void setUp() {
        itemDto = new ItemDto();
        itemDto.setName("Sword");
        itemDto.setDescription("Sharp sword");
        itemDto.setCategory("Weapon");
        itemDto.setPowerValue(10);
        itemDto.setRarity(Rarity.COMMON);

        item = Item.builder()
                .id(1L)
                .name("Sword")
                .description("Sharp sword")
                .category("Weapon")
                .powerValue(10)
                .rarity(Rarity.COMMON)
                .build();

        itemList = Arrays.asList(item);
    }

    @Test
    void testCreateItem_Success() {
        when(itemRepository.findByNameAndCategory("Sword", "Weapon")).thenReturn(Optional.empty());
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        Item created = itemService.createItem(itemDto);

        assertNotNull(created);
        assertEquals("Sword", created.getName());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void testCreateItem_AlreadyExists() {
        when(itemRepository.findByNameAndCategory("Sword", "Weapon")).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> itemService.createItem(itemDto));
    }

    @Test
    void testGetItem_Success() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Item found = itemService.getItem(1L);

        assertNotNull(found);
        assertEquals(1L, found.getId());
        assertEquals("Sword", found.getName());
    }

    @Test
    void testGetItem_NotFound() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> itemService.getItem(999L));
    }

    @Test
    void testDeleteItem() {
        when(itemRepository.existsById(1L)).thenReturn(true);

        itemService.deleteItem(1L);

        verify(itemRepository).existsById(1L);
        verify(itemRepository).deleteById(1L);
    }

    @Test
    void testDeleteItem_NotFound() {
        when(itemRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> itemService.deleteItem(999L));
        verify(itemRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetItems_NoFilters() {
        when(itemRepository.findItemsWithFilters(eq(null), eq(null), any())).thenReturn(itemList);

        List<Item> result = itemService.getItems(null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Sword", result.get(0).getName());
        verify(itemRepository).findItemsWithFilters(eq(null), eq(null), any());
    }

    @Test
    void testGetItems_FilterByCategory() {
        when(itemRepository.findItemsWithFilters(eq("Weapon"), eq(null), any())).thenReturn(itemList);

        List<Item> result = itemService.getItems("Weapon", null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(itemRepository).findItemsWithFilters(eq("Weapon"), eq(null), any());
    }

    @Test
    void testGetItems_FilterByRarity() {
        when(itemRepository.findItemsWithFilters(eq(null), eq(Rarity.COMMON), any())).thenReturn(itemList);

        List<Item> result = itemService.getItems(null, Rarity.COMMON, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(itemRepository).findItemsWithFilters(eq(null), eq(Rarity.COMMON), any());
    }

    @Test
    void testGetItems_FilterByBothCategoryAndRarity() {
        when(itemRepository.findItemsWithFilters(eq("Weapon"), eq(Rarity.COMMON), any())).thenReturn(itemList);

        List<Item> result = itemService.getItems("Weapon", Rarity.COMMON, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(itemRepository).findItemsWithFilters(eq("Weapon"), eq(Rarity.COMMON), any());
    }

    @Test
    void testGetItems_WithPagination() {
        when(itemRepository.findItemsWithFilters(eq(null), eq(null), any())).thenReturn(itemList);

        List<Item> result = itemService.getItems(null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(itemRepository).findItemsWithFilters(eq(null), eq(null), any());
    }
}
