package com.ajay.epicquest.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
/**
 * Represents a player-owned hero. Inventory uniqueness and size limits are core
 * business rules enforced in the service layer and reinforced by persistence constraints.
 */
public class Hero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "hero_class")
    private String heroClass;

    @Column(nullable = false)
    private int level;

    @ManyToOne
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "hero_items",
            uniqueConstraints = @UniqueConstraint(columnNames = {"hero_id", "item_id"}),
            joinColumns = @JoinColumn(name = "hero_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
        // The join-table uniqueness constraint prevents the same item from being attached twice.
    private List<Item> items;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
