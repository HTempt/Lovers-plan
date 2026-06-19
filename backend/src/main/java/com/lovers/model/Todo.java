package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "todo")
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "executor_id")
    private Long executorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 20)
    private String priority;

    @Column
    private LocalDateTime deadline;

    @Column(name = "repeat_type", length = 20)
    private String repeatType;

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
        if (status == null) status = 0;
        if (priority == null) priority = "mid";
        if (repeatType == null) repeatType = "";
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
