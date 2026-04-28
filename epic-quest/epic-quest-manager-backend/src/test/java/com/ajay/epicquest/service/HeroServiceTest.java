package com.ajay.epicquest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ajay.epicquest.exception.BadRequestException;
import com.ajay.epicquest.exception.ResourceNotFoundException;
import com.ajay.epicquest.model.Hero;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.User;
import com.ajay.epicquest.model.enums.Role;
import com.ajay.epicquest.repository.HeroRepository;
import com.ajay.epicquest.repository.ItemRepository;
import com.ajay.epicquest.repository.UserRepository;
import com.ajay.epicquest.service.impl.HeroServiceImpl;

import java.util.ArrayList;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class HeroServiceTest {

    @Mock
    private HeroRepository heroRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private HeroServiceImpl heroService;

    private Hero hero;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.PLAYER)
                .build();

        hero = Hero.builder()
                .id(1L)
                .name("Aragorn")
                .heroClass("Ranger")
                .level(10)
                .owner(user)
                .items(new ArrayList<>())
                .build();

            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null)
            );
    }

    @Test
    void testGetHero_Success() {
        when(heroRepository.findById(1L)).thenReturn(Optional.of(hero));

        Hero found = heroService.getHero(1L);

        assertNotNull(found);
        assertEquals(1L, found.getId());
        assertEquals("Aragorn", found.getName());
        assertEquals("Ranger", found.getHeroClass());
    }

    @Test
    void testGetHero_NotFound() {
        when(heroRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> heroService.getHero(999L));
    }

    @Test
    void testAddItemToHero_RejectsDuplicateItem() {
        Item item = Item.builder()
                .id(42L)
                .name("Sword")
                .build();
        hero.getItems().add(item);

        when(heroRepository.findById(1L)).thenReturn(Optional.of(hero));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(itemRepository.findById(42L)).thenReturn(Optional.of(item));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> heroService.addItemToHero(1L, 42L));

        assertEquals("Item is already in hero inventory", exception.getMessage());
        verify(heroRepository, never()).save(any(Hero.class));
    }
}
