package com.ajay.epicquest.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.enums.Rarity;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByNameAndCategory(String name, String category);
    Optional<Item> findByNameAndCategoryAndIdNot(String name, String category, Long id);

    @Query("SELECT i FROM Item i WHERE " +
           "(:category IS NULL OR i.category = :category) AND " +
           "(:rarity IS NULL OR i.rarity = :rarity) " +
           "ORDER BY CASE i.rarity " +
           "WHEN com.ajay.epicquest.model.enums.Rarity.COMMON THEN 1 " +
           "WHEN com.ajay.epicquest.model.enums.Rarity.RARE THEN 2 " +
           "WHEN com.ajay.epicquest.model.enums.Rarity.EPIC THEN 3 " +
           "WHEN com.ajay.epicquest.model.enums.Rarity.LEGENDARY THEN 4 " +
           "ELSE 5 END ASC, i.powerValue DESC")
    List<Item> findItemsWithFilters(@Param("category") String category,
                                   @Param("rarity") Rarity rarity,
                                   Pageable pageable);
}
