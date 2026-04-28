package com.ajay.epicquest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ajay.epicquest.model.QuestAcceptance;

import java.util.List;

public interface QuestAcceptanceRepository extends JpaRepository<QuestAcceptance, Long> {
    List<QuestAcceptance> findByHeroIdOrderByAcceptedAtDesc(Long heroId);
}