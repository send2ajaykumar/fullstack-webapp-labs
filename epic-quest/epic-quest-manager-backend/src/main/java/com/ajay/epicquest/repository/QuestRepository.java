package com.ajay.epicquest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ajay.epicquest.model.Quest;

public interface QuestRepository extends JpaRepository<Quest, Long> {
}
