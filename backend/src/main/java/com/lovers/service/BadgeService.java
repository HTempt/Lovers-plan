package com.lovers.service;

import com.lovers.model.Badge;
import com.lovers.model.Task;
import com.lovers.model.TaskRecord;
import com.lovers.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BadgeService {

    private static final Logger log = LoggerFactory.getLogger(BadgeService.class);

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    /** 旅行达人所需最少地点数 */
    private static final int TRAVEL_MIN_LOCATIONS = 5;

    /** 连续打卡天数阈值 */
    private static final int STREAK_DAYS = 7;

    /** 健身关键词 */
    private static final Set<String> FITNESS_KEYWORDS = Set.of("运动", "健身", "跑步", "锻炼", "瑜伽", "游泳", "跳绳", "打球");

    /**
     * 任务确认打卡后检查并颁发成就徽章
     */
    @Transactional
    public void checkAndAward(Long coupleId, Long taskId) {
        // 逐个检查成就类型
        checkStreakBadge(coupleId);
        checkTravelBadge(coupleId);
        checkFitnessBadge(coupleId, taskId);
    }

    /**
     * 连续7天打卡徽章
     */
    private void checkStreakBadge(Long coupleId) {
        // 已获得则跳过
        if (badgeRepository.findByCoupleIdAndBadgeType(coupleId, "STREAK_7").isPresent()) {
            return;
        }

        // 获取该情侣所有已确认的打卡记录日期
        List<Task> tasks = taskRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId);
        Set<LocalDate> confirmedDates = new HashSet<>();

        for (Task task : tasks) {
            List<TaskRecord> records = taskRecordRepository.findByTaskId(task.getId());
            for (TaskRecord record : records) {
                if (record.getStatus() == 1) { // 已确认
                    confirmedDates.add(record.getCreateTime().toLocalDate());
                }
            }
        }

        // 检查是否有连续7天
        if (hasConsecutiveDays(confirmedDates, STREAK_DAYS)) {
            awardBadge(coupleId, "STREAK_7", "连续7天打卡", "🔥");
            log.info("Couple {} earned STREAK_7 badge", coupleId);
        }
    }

    /**
     * 旅行达人徽章 — 足迹≥5个不同城市
     */
    private void checkTravelBadge(Long coupleId) {
        if (badgeRepository.findByCoupleIdAndBadgeType(coupleId, "TRAVEL").isPresent()) {
            return;
        }

        List<String> locations = diaryRepository.findDistinctLocationsByCoupleId(coupleId, 1);

        if (locations.size() >= TRAVEL_MIN_LOCATIONS) {
            awardBadge(coupleId, "TRAVEL", "旅行达人", "🌍");
            log.info("Couple {} earned TRAVEL badge with {} locations", coupleId, locations.size());
        }
    }

    /**
     * 健身达人徽章 — 完成含健身关键词的任务
     */
    private void checkFitnessBadge(Long coupleId, Long taskId) {
        if (badgeRepository.findByCoupleIdAndBadgeType(coupleId, "FITNESS").isPresent()) {
            return;
        }

        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return;

        String title = taskOpt.get().getTitle();
        boolean isFitness = FITNESS_KEYWORDS.stream().anyMatch(title::contains);

        if (isFitness && taskOpt.get().getStatus() == 2) { // 任务已完成
            awardBadge(coupleId, "FITNESS", "健身达人", "💪");
            log.info("Couple {} earned FITNESS badge via task: {}", coupleId, title);
        }
    }

    /**
     * 颁发徽章
     */
    private void awardBadge(Long coupleId, String badgeType, String title, String icon) {
        Badge badge = new Badge();
        badge.setCoupleId(coupleId);
        badge.setBadgeType(badgeType);
        badge.setTitle(title);
        badge.setIcon(icon);
        badge.setEarnedDate(LocalDateTime.now());
        badgeRepository.save(badge);
    }

    /**
     * 查询情侣已获得的全部徽章
     */
    public List<Badge> getBadges(Long coupleId) {
        return badgeRepository.findByCoupleIdOrderByEarnedDateDesc(coupleId);
    }

    // ======== 工具方法 ========

    /**
     * 判断日期集合中是否存在连续 N 天
     */
    private boolean hasConsecutiveDays(Set<LocalDate> dates, int n) {
        if (dates.size() < n) return false;

        List<LocalDate> sorted = dates.stream().sorted().collect(Collectors.toList());
        int streak = 1;

        for (int i = 1; i < sorted.size(); i++) {
            long diff = ChronoUnit.DAYS.between(sorted.get(i - 1), sorted.get(i));
            if (diff == 1) {
                streak++;
                if (streak >= n) return true;
            } else if (diff > 1) {
                streak = 1;
            }
            // diff == 0 (同一天多次打卡) 不打断连续
        }
        return false;
    }
}
