package com.ajay.epicquest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ajay.epicquest.model.Hero;

public interface HeroRepository extends JpaRepository<Hero, Long> {
}
