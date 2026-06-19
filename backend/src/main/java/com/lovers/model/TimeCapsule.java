package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "time_capsule")
public class TimeCapsule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 胶囊类型: to_future_ta / to_future_us / birthday / anniversary / wish */
    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** 预定开启时间 */
    @Column(name = "open_at", nullable = false)
    private LocalDateTime openAt;

    /** 实际开启时间 */
    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /** 状态 0=DRAFT 1=SEALED 2=OPENABLE 3=OPENED */
    @Column(columnDefinition = "TINYINT")
    private Integer status;

    /** 关联胶囊ID（双人模式） */
    @Column(name = "pair_id")
    private Long pairId;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) status = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
