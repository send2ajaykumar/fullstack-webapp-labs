package com.ajay.epicquest.dto;

import com.ajay.epicquest.model.Item;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class HeroResponse {

    private Long id;

    private String name;

    @JsonProperty("hero_class")
    private String heroClass;

    private int level;

    @JsonProperty("owner_user_id")
    private Long ownerUserId;

    private List<Item> items;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}