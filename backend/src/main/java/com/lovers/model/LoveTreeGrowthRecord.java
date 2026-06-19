package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "love_tree_growth_record")
public class LoveTreeGrowthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    /** 行为类型: diary / task / wish / sign_in / status */
    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "growth_value", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer growthValue;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(length = 200)
    private String description;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}
