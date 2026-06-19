package com.lovers.service;

import com.lovers.model.UserStatus;
import java.util.List;

public interface IUserStatusService {

    List<String> STATUS_TEMPLATES = List.of("工作中", "学习中", "睡觉中", "运动中", "游戏中", "路上");
    List<String> MOOD_TAGS = List.of("开心", "平静", "难过", "生气");
    UserStatus setStatus(Long userId, String statusName, String mood, Integer durationMinutes);
    UserStatus getCurrentStatus(Long userId);
    void clearStatus(Long userId);
    UserStatus getPartnerStatus(Long userId);
    void cleanExpiredStatuses();
}
