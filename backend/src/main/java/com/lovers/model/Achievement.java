package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "achievement")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 10)
    private String icon;

    @Column(columnDefinition = "TINYINT")
    private Integer rarity;

    @Column(name = "growth_reward")
    private Integer growthReward;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(columnDefinition = "TINYINT")
    private Integer hidden;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (rarity == null) rarity = 1;
        if (growthReward == null) growthReward = 20;
        if (hidden == null) hidden = 0;
    }
}
