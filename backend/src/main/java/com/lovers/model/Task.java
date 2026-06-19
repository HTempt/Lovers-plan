package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "target_count")
    private Integer targetCount;

    @Column(name = "current_count")
    private Integer currentCount;

    @Column
    private LocalDate deadline;

    @Column(columnDefinition = "TINYINT")
    private Integer status;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (targetCount == null) targetCount = 1;
        if (currentCount == null) currentCount = 0;
        if (status == null) status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
