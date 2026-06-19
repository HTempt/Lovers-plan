package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "activity")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    /** 动态类型: diary / status / task / wish / anniversary */
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    /** 展示标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 展示描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 关联的业务记录 ID（用于点击跳转） */
    @Column(name = "ref_id")
    private Long refId;

    /** 展示图标 */
    @Column(length = 10)
    private String icon;

    /** 发生时间 */
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}
