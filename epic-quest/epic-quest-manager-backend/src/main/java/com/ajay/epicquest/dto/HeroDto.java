package com.ajay.epicquest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HeroDto {

    @NotBlank(message = "Hero name is required")
    private String name;

    @NotBlank(message = "Hero class is required")
    @JsonProperty("hero_class")
    @JsonAlias("heroClass")
    private String heroClass;

    @Min(value = 1, message = "Level must be >= 1")
    private int level;
}
