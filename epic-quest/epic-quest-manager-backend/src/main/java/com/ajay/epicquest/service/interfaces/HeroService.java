package com.ajay.epicquest.service.interfaces;

import com.ajay.epicquest.dto.HeroDto;
import com.ajay.epicquest.model.Hero;

/**
 * Defines hero management operations, including inventory updates tied to
 * ownership and gameplay rules.
 */
public interface HeroService {
    Hero createHero(HeroDto dto);
    Hero getHero(Long id);

    /**
     * Adds an item to a hero subject to authorization, duplicate prevention, and inventory limits.
     */
    Hero addItemToHero(Long heroId, Long itemId);

    /**
     * Removes an item from a hero inventory when the caller is authorized to manage that hero.
     */
    Hero removeItemFromHero(Long heroId, Long itemId);
}
