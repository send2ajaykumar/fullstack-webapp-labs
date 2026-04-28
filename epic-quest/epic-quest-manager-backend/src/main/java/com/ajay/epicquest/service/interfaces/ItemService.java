package com.ajay.epicquest.service.interfaces;

import java.util.List;

import com.ajay.epicquest.dto.ItemDto;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.enums.Rarity;

/**
 * Defines item catalog operations for creation, retrieval, filtering, update, and deletion.
 */
public interface ItemService {
    Item createItem(ItemDto dto);
    Item getItem(Long id);
    List<Item> getAllItems();

    /**
     * Returns items with optional category and rarity filters plus optional pagination.
     */
    List<Item> getItems(String category, Rarity rarity, Integer page, Integer limit);
    Item updateItem(Long id, ItemDto dto);
    void deleteItem(Long id);
}
