package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "love_tree")
public class LoveTree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false, unique = true)
    private Long coupleId;

    @Column(columnDefinition = "INT DEFAULT 1")
    private Integer level;

    @Column(name = "growth_value", columnDefinition = "INT DEFAULT 0")
    private Integer growthValue;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (level == null) level = 1;
        if (growthValue == null) growthValue = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
