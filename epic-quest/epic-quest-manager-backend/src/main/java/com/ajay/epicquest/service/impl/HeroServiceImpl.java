package com.ajay.epicquest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ajay.epicquest.dto.HeroDto;
import com.ajay.epicquest.exception.BadRequestException;
import com.ajay.epicquest.exception.ForbiddenException;
import com.ajay.epicquest.exception.ResourceNotFoundException;
import com.ajay.epicquest.model.Hero;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.User;
import com.ajay.epicquest.model.enums.Role;
import com.ajay.epicquest.repository.HeroRepository;
import com.ajay.epicquest.repository.ItemRepository;
import com.ajay.epicquest.repository.UserRepository;
import com.ajay.epicquest.service.interfaces.HeroService;

@Service
@RequiredArgsConstructor
/**
 * Encapsulates hero lifecycle and inventory rules, including owner/admin
 * authorization, max inventory size, and duplicate item prevention.
 */
public class HeroServiceImpl implements HeroService {

    private final HeroRepository heroRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    @Override
    public Hero createHero(HeroDto dto) {
        User owner = getAuthenticatedUser();

        Hero hero = Hero.builder()
                .name(dto.getName())
                .heroClass(dto.getHeroClass())
                .level(dto.getLevel())
                .owner(owner)
                .build();

        return heroRepository.save(hero);
    }

    @Override
    public Hero getHero(Long id) {
        return heroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hero not found"));
    }

    @Override
    public Hero addItemToHero(Long heroId, Long itemId) {
        Hero hero = getHero(heroId);
        User currentUser = getAuthenticatedUser();

        if (!(currentUser.getRole() == Role.ADMIN || hero.getOwner().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("Not authorized to add item to this hero");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        // Duplicate prevention is enforced in both service logic and the join-table constraint.
        boolean alreadyInInventory = hero.getItems().stream()
            .anyMatch(existingItem -> existingItem.getId().equals(itemId));
        if (alreadyInInventory) {
            throw new BadRequestException("Item is already in hero inventory");
        }

        if (hero.getItems().size() >= 3) {
            throw new BadRequestException("Hero cannot have more than 3 items");
        }

        hero.getItems().add(item);
        return heroRepository.save(hero);
    }

    @Override
    public Hero removeItemFromHero(Long heroId, Long itemId) {
        Hero hero = getHero(heroId);
        User currentUser = getAuthenticatedUser();

        if (!(currentUser.getRole() == Role.ADMIN || hero.getOwner().getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("Not authorized to remove item from this hero");
        }

        boolean removed = hero.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Item not found in hero inventory");
        }

        return heroRepository.save(hero);
    }
}

