package com.lovers.service;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.Couple;
import com.lovers.model.User;
import com.lovers.model.UserStatus;
import com.lovers.repository.CoupleRepository;
import com.lovers.repository.UserRepository;
import com.lovers.repository.UserStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserStatusService {

    public static final List<String> STATUS_TEMPLATES = List.of("工作中", "学习中", "睡觉中", "运动中", "游戏中", "路上");
    public static final List<String> MOOD_TAGS = List.of("开心", "平静", "难过", "生气");

    @Autowired
    private UserStatusRepository userStatusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private WechatSubscribeService wechatSubscribeService;

    @Autowired
    private ActivityService activityService;

    private static final Logger log = LoggerFactory.getLogger(UserStatusService.class);

    /**
     * 设置状态 - 自动结束之前的活跃状态
     */
    @Transactional
    public UserStatus setStatus(Long userId, String statusName, String mood, Integer durationMinutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 结束之前的活跃状态
        userStatusRepository.findByUserIdAndStatus(userId, 1).ifPresent(oldStatus -> {
            oldStatus.setStatus(0);
            userStatusRepository.save(oldStatus);
        });

        // 创建新状态
        UserStatus newStatus = new UserStatus();
        newStatus.setUserId(userId);
        newStatus.setStatusName(statusName);
        newStatus.setMood(mood != null ? mood : "");
        newStatus.setDurationMinutes(durationMinutes != null ? durationMinutes : 0);
        newStatus = userStatusRepository.save(newStatus);

        // 通知另一半状态变更
        notifyPartnerStatusChange(user, statusName, mood);

        // 记录动态
        try {
            String moodStr = mood != null && !mood.isEmpty() ? " · " + mood : "";
            activityService.recordActivity(user.getCoupleId(), "status",
                    "🟢 " + statusName + moodStr, user.getNickname() + " 更新了状态",
                    null, "🟢");
        } catch (Exception e) {
            log.warn("Failed to record status activity", e);
        }

        return newStatus;
    }

    /**
     * 通知另一半状态变更
     */
    private void notifyPartnerStatusChange(User user, String statusName, String mood) {
        try {
            if (user.getCoupleId() == null) return;

            Couple couple = coupleRepository.findById(user.getCoupleId()).orElse(null);
            if (couple == null || couple.getStatus() != 1) return;

            Long partnerId;
            if (couple.getUserA().equals(user.getId())) {
                partnerId = couple.getUserB();
            } else {
                partnerId = couple.getUserA();
            }
            if (partnerId == null) return;

            userRepository.findById(partnerId).ifPresent(partner -> {
                if (partner.getOpenid() != null) {
                    boolean sent = wechatSubscribeService.sendStatusReminder(
                            partner.getOpenid(),
                            user.getNickname() != null ? user.getNickname() : "对方",
                            statusName,
                            mood
                    );
                    if (sent) {
                        log.info("Status change notification sent to partner {}", partner.getId());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to notify partner status change", e);
        }
    }

    /**
     * 获取当前活跃状态
     */
    public UserStatus getCurrentStatus(Long userId) {
        return userStatusRepository.findByUserIdAndStatus(userId, 1).orElse(null);
    }

    /**
     * 清空当前状态 - 结束活跃状态，不创建新状态
     */
    @Transactional
    public void clearStatus(Long userId) {
        userStatusRepository.findByUserIdAndStatus(userId, 1).ifPresent(oldStatus -> {
            oldStatus.setStatus(0);
            userStatusRepository.save(oldStatus);
        });
    }

    /**
     * 获取另一半的状态
     */
    public UserStatus getPartnerStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getCoupleId() == null) {
            return null;
        }

        Couple couple = coupleRepository.findById(user.getCoupleId())
                .orElseThrow(() -> new BusinessException("情侣关系不存在"));

        Long partnerId;
        if (couple.getUserA().equals(userId)) {
            partnerId = couple.getUserB();
        } else {
            partnerId = couple.getUserA();
        }

        if (partnerId == null) {
            return null;
        }

        return userStatusRepository.findByUserIdAndStatus(partnerId, 1).orElse(null);
    }

    /**
     * 定时清理超过24小时的过期状态
     */
    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanExpiredStatuses() {
        try {
            java.time.LocalDateTime expireTime = java.time.LocalDateTime.now().minusHours(24);
            List<UserStatus> expired = userStatusRepository.findExpiredStatuses(expireTime);
            for (UserStatus s : expired) {
                s.setStatus(0);
                userStatusRepository.save(s);
            }
            if (!expired.isEmpty()) {
                org.slf4j.LoggerFactory.getLogger(getClass()).info("Cleaned {} expired statuses", expired.size());
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Failed to clean expired statuses", e);
        }
    }
}
