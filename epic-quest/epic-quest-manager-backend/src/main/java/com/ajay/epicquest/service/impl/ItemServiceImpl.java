package com.ajay.epicquest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ajay.epicquest.dto.ItemDto;
import com.ajay.epicquest.exception.BadRequestException;
import com.ajay.epicquest.exception.ResourceNotFoundException;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.enums.Rarity;
import com.ajay.epicquest.repository.ItemRepository;
import com.ajay.epicquest.service.interfaces.ItemService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    @Override
    @CacheEvict(value = "itemsList", allEntries = true)
    public Item createItem(ItemDto dto) {
        itemRepository.findByNameAndCategory(dto.getName(), dto.getCategory())
                .ifPresent(i -> { throw new BadRequestException("Item already exists in this category"); });

        Item item = Item.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .powerValue(dto.getPowerValue())
                .rarity(dto.getRarity())
                .build();

        return itemRepository.save(item);
    }

    @Override
    public Item getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
    }

    @Override
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @Override
    @Cacheable(value = "itemsList", key = "#category + '|' + #rarity + '|' + #page + '|' + #limit")
    public List<Item> getItems(String category, Rarity rarity, Integer page, Integer limit) {
        // Create pageable with sorting: rarity ascending, then power_value descending
        Pageable pageable;
        if (page != null && limit != null && page >= 0 && limit > 0) {
            pageable = PageRequest.of(page, limit);
        } else {
            // No pagination, get all results
            pageable = PageRequest.of(0, Integer.MAX_VALUE);
        }

        return itemRepository.findItemsWithFilters(category, rarity, pageable);
    }

    @Override
    @CacheEvict(value = "itemsList", allEntries = true)
    public Item updateItem(Long id, ItemDto dto) {
        Item item = getItem(id);

        itemRepository.findByNameAndCategoryAndIdNot(dto.getName(), dto.getCategory(), id)
                .ifPresent(i -> { throw new BadRequestException("Another item with same name and category exists"); });

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());
        item.setPowerValue(dto.getPowerValue());
        item.setRarity(dto.getRarity());

        return itemRepository.save(item);
    }

    @Override
    @CacheEvict(value = "itemsList", allEntries = true)
    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item not found");
        }
        itemRepository.deleteById(id);
    }
}

