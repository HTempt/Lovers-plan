package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "badge")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(name = "badge_type", nullable = false, length = 50)
    private String badgeType;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 100)
    private String icon;

    @Column(name = "earned_date")
    private LocalDateTime earnedDate;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (earnedDate == null) earnedDate = LocalDateTime.now();
    }
}
