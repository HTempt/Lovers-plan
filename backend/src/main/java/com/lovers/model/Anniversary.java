package com.lovers.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "anniversary")
public class Anniversary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "anniversary_date", nullable = false)
    private LocalDate anniversaryDate;

    @Column(name = "remind_days")
    private Integer remindDays;

    @Column(columnDefinition = "TINYINT")
    private Integer type;

    @Column(length = 20)
    private String icon;

    @Column(columnDefinition = "TINYINT")
    private Integer status;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (type == null) type = 0;
        if (status == null) status = 1;
        if (remindDays == null) remindDays = 0;
        if (icon == null) icon = "❤️";
    }
}
