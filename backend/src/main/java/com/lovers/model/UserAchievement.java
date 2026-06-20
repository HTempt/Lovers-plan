package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_achievement",
       uniqueConstraints = @UniqueConstraint(columnNames = {"couple_id", "achievement_id"}))
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(name = "achievement_id", nullable = false)
    private Long achievementId;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @PrePersist
    protected void onCreate() {
        unlockedAt = LocalDateTime.now();
    }
}
