package com.ajay.epicquest.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ajay.epicquest.model.enums.Rarity;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
/**
 * Represents a quest definition that heroes may accept if they satisfy the
 * required item rarity threshold.
 */
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "difficulty_level")
    private int difficultyLevel;

    @Enumerated(EnumType.STRING)
    private Rarity requiredRarity;

    @ManyToOne
    @JoinColumn(name = "reward_item_id")
    private Item rewardItem;

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL)
    // Acceptances are created as heroes opt into the quest; duplicates are blocked in the service layer.
    private List<QuestAcceptance> acceptedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
