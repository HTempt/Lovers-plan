package com.lovers.service.impl;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.*;
import com.lovers.repository.*;
import com.lovers.service.IAchievementService;
import com.lovers.service.IActivityService;
import com.lovers.service.ILoveTreeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AchievementServiceImpl implements IAchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementServiceImpl.class);

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private FootprintRepository footprintRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TimeCapsuleRepository timeCapsuleRepository;

    @Autowired
    private DailyAnswerRepository dailyAnswerRepository;

    @Autowired
    private LoveTreeRepository loveTreeRepository;

    @Autowired
    private SignInRecordRepository signInRecordRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private IActivityService activityService;

    @Autowired
    private ILoveTreeService loveTreeService;

    // ==================== Public API ====================

    @Override
    public Map<String, Object> getOverview(Long coupleId) {
        long total = achievementRepository.count();
        long unlocked = userAchievementRepository.countByCoupleId(coupleId);
        int percent = total > 0 ? (int) (unlocked * 100 / total) : 0;

        int level = getLevel(percent);
        String title = getTitle(percent);
        String nextTitle = getNextTitle(percent);
        long needForNext = getNeedForNext(total, unlocked, percent);

        // 找到最近的一个可解锁目标
        Map<String, Object> nextTarget = findNextTarget(coupleId);

        // 爱情树成长值
        int growthValue = 0;
        int growthTarget = 100;
        Optional<LoveTree> treeOpt = loveTreeRepository.findByCoupleId(coupleId);
        if (treeOpt.isPresent()) {
            LoveTree tree = treeOpt.get();
            int treeLevel = tree.getLevel();
            growthValue = tree.getGrowthValue();
            // 成长阈值定义: {0, 200, 500, 1000, 2000, 5000, 10000}
            int[] thresholds = {0, 200, 500, 1000, 2000, 5000, 10000};
            int idx = Math.min(treeLevel, thresholds.length - 1);
            growthTarget = thresholds[idx];
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("unlocked", unlocked);
        result.put("percent", percent);
        result.put("level", level);
        result.put("title", title);
        result.put("nextTitle", nextTitle);
        result.put("needForNext", needForNext);
        result.put("nextTarget", nextTarget);
        result.put("growthValue", growthValue);
        result.put("growthTarget", growthTarget);
        return result;
    }

    @Override
    public List<Map<String, Object>> getAllAchievements(Long coupleId) {
        List<Achievement> all = achievementRepository.findAllByOrderBySortOrderAsc();
        return buildAchievementListWithProgress(coupleId, all);
    }

    @Override
    public List<Map<String, Object>> getAchievementsByCategory(Long coupleId, String category) {
        List<Achievement> list = achievementRepository.findByCategoryOrderBySortOrderAsc(category);
        return buildAchievementListWithProgress(coupleId, list);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> checkAndUnlock(Long coupleId, String category, String code, Long userId) {
        List<Map<String, Object>> newlyUnlocked = new ArrayList<>();

        // 查该分类下所有未解锁的成就
        List<Achievement> candidates = achievementRepository.findByCategoryOrderBySortOrderAsc(category);
        for (Achievement ach : candidates) {
            if (userAchievementRepository.existsByCoupleIdAndAchievementId(coupleId, ach.getId())) {
                continue;
            }
            if (!isConditionMet(coupleId, ach.getCode(), userId)) {
                continue;
            }
            // 解锁
            UserAchievement ua = new UserAchievement();
            ua.setCoupleId(coupleId);
            ua.setAchievementId(ach.getId());
            userAchievementRepository.save(ua);

            // 发放成长值
            try {
                loveTreeService.addGrowth(coupleId, "achievement_" + ach.getCode(),
                        ach.getGrowthReward(), ach.getId(),
                        "解锁成就：" + ach.getName());
            } catch (Exception e) {
                log.warn("Failed to add growth for achievement {}", ach.getCode(), e);
            }

            // 记录动态
            try {
                activityService.recordActivity(coupleId, "achievement",
                        ach.getIcon() + " " + ach.getName(),
                        ach.getDescription(),
                        ach.getId(), ach.getIcon());
            } catch (Exception e) {
                log.warn("Failed to record achievement activity", e);
            }

            Map<String, Object> unlockedMap = achievementToMap(ach, true);
            // 已解锁，进度 = target
            Map<String, Object> p = getProgress(ach.getCode(),
                    getLoveDays(coupleId), diaryRepository.countByCoupleId(coupleId),
                    diaryRepository.countPhotoDiaries(coupleId),
                    footprintRepository.countDistinctCityByCoupleId(coupleId),
                    footprintRepository.countDistinctProvinces(coupleId),
                    taskRepository.countByCoupleIdAndStatus(coupleId, 2),
                    getMaxConsecutiveSignIns(coupleId),
                    timeCapsuleRepository.countByCoupleIdAndStatusNot(coupleId, 0),
                    timeCapsuleRepository.countByCoupleIdAndStatus(coupleId, 3),
                    timeCapsuleRepository.countDualCompleted(coupleId),
                    dailyAnswerRepository.countByCoupleId(coupleId),
                    dailyAnswerRepository.countPerfectMatches(coupleId),
                    dailyAnswerRepository.countAnswerDays(coupleId),
                    getLoveTreeLevel(coupleId),
                    signInRecordRepository.countByCoupleId(coupleId));
            if (p != null) {
                unlockedMap.put("progress", p.get("current"));
                unlockedMap.put("target", p.get("target"));
            }
            newlyUnlocked.add(unlockedMap);
        }
        return newlyUnlocked;
    }

    @Override
    public List<Map<String, Object>> getRecentUnlocks(Long coupleId, int limit) {
        List<UserAchievement> recent = userAchievementRepository.findByCoupleId(coupleId);
        recent.sort((a, b) -> b.getUnlockedAt().compareTo(a.getUnlockedAt()));

        return recent.stream()
                .limit(limit)
                .map(ua -> {
                    Achievement ach = achievementRepository.findById(ua.getAchievementId()).orElse(null);
                    if (ach == null) return null;
                    Map<String, Object> m = achievementToMap(ach, true);
                    m.put("unlockedAt", ua.getUnlockedAt().toString());
                    return m;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== Internal ====================

    private List<Map<String, Object>> buildAchievementListWithProgress(Long coupleId, List<Achievement> achievements) {
        long loveDays = getLoveDays(coupleId);
        long diaryCount = diaryRepository.countByCoupleId(coupleId);
        long diaryPhotoCount = diaryRepository.countPhotoDiaries(coupleId);
        long cityCount = footprintRepository.countDistinctCityByCoupleId(coupleId);
        long provinceCount = footprintRepository.countDistinctProvinces(coupleId);
        long taskCount = taskRepository.countByCoupleIdAndStatus(coupleId, 2);
        long consecutiveSign = getMaxConsecutiveSignIns(coupleId);
        long capsuleCount = timeCapsuleRepository.countByCoupleIdAndStatusNot(coupleId, 0);
        long capsuleOpenCount = timeCapsuleRepository.countByCoupleIdAndStatus(coupleId, 3);
        long capsuleDualCount = timeCapsuleRepository.countDualCompleted(coupleId);
        long quizCount = dailyAnswerRepository.countByCoupleId(coupleId);
        long quizPerfectCount = dailyAnswerRepository.countPerfectMatches(coupleId);
        long quizDaysCount = dailyAnswerRepository.countAnswerDays(coupleId);
        int treeLevel = getLoveTreeLevel(coupleId);
        long signCount = signInRecordRepository.countByCoupleId(coupleId);

        return achievements.stream().map(ach -> {
            boolean unlocked = userAchievementRepository
                    .existsByCoupleIdAndAchievementId(coupleId, ach.getId());
            Map<String, Object> map = achievementToMap(ach, unlocked);

            // 添加进度信息
            Map<String, Object> progress = getProgress(ach.getCode(),
                    loveDays, diaryCount, diaryPhotoCount, cityCount, provinceCount,
                    taskCount, consecutiveSign, capsuleCount, capsuleOpenCount, capsuleDualCount,
                    quizCount, quizPerfectCount, quizDaysCount, treeLevel, signCount);
            if (progress != null) {
                map.put("progress", progress.get("current"));
                map.put("target", progress.get("target"));
            }

            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> achievementToMap(Achievement ach, boolean unlocked) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ach.getId());
        map.put("category", ach.getCategory());
        map.put("code", ach.getCode());
        map.put("name", ach.getName());
        map.put("description", ach.getDescription() != null ? ach.getDescription() : "");
        map.put("icon", ach.getIcon() != null ? ach.getIcon() : "🏆");
        map.put("rarity", ach.getRarity());
        map.put("growthReward", ach.getGrowthReward());
        map.put("sortOrder", ach.getSortOrder());
        map.put("hidden", ach.getHidden());
        map.put("unlocked", unlocked);
        return map;
    }

    /** 获取成就的当前进度 (current / target) */
    private Map<String, Object> getProgress(String code,
            long loveDays, long diaryCount, long diaryPhotoCount,
            long cityCount, long provinceCount, long taskCount, long consecutiveSign,
            long capsuleCount, long capsuleOpenCount, long capsuleDualCount,
            long quizCount, long quizPerfectCount, long quizDaysCount,
            int treeLevel, long signCount) {
        long current;
        long target;

        switch (code) {
            case "bind_couple": current = 1; target = 1; break;
            case "days_100": current = loveDays; target = 100; break;
            case "days_200": current = loveDays; target = 200; break;
            case "days_300": current = loveDays; target = 300; break;
            case "days_365": current = loveDays; target = 365; break;
            case "diary_1": current = diaryCount; target = 1; break;
            case "diary_10": current = diaryCount; target = 10; break;
            case "diary_50": current = diaryCount; target = 50; break;
            case "diary_100": current = diaryCount; target = 100; break;
            case "diary_photo_10": current = diaryPhotoCount; target = 10; break;
            case "city_1": current = cityCount; target = 1; break;
            case "city_3": current = cityCount; target = 3; break;
            case "city_5": current = cityCount; target = 5; break;
            case "city_10": current = cityCount; target = 10; break;
            case "province_cross": current = provinceCount; target = 2; break;
            case "task_1": current = taskCount; target = 1; break;
            case "task_10": current = taskCount; target = 10; break;
            case "task_50": current = taskCount; target = 50; break;
            case "task_100": current = taskCount; target = 100; break;
            case "checkin_7": current = consecutiveSign; target = 7; break;
            case "capsule_1": current = capsuleCount; target = 1; break;
            case "capsule_open_1": current = capsuleOpenCount; target = 1; break;
            case "capsule_dual": current = capsuleDualCount; target = 1; break;
            case "quiz_1": current = quizCount; target = 2; break;
            case "quiz_perfect": current = quizPerfectCount; target = 1; break;
            case "quiz_7": current = quizDaysCount; target = 7; break;
            case "tree_lv2": current = treeLevel; target = 2; break;
            case "tree_lv5": current = treeLevel; target = 5; break;
            case "tree_lv10": current = treeLevel; target = 10; break;
            case "sign_30": current = signCount; target = 30; break;
            default: return null;
        }
        return Map.of("current", Math.min(current, target), "target", target);
    }

    /** 查找最近的可解锁成就（作为"下一目标"） */
    private Map<String, Object> findNextTarget(Long coupleId) {
        List<Achievement> all = achievementRepository.findAllByOrderBySortOrderAsc();
        long loveDays = getLoveDays(coupleId);
        long diaryCount = diaryRepository.countByCoupleId(coupleId);
        long diaryPhotoCount = diaryRepository.countPhotoDiaries(coupleId);
        long cityCount = footprintRepository.countDistinctCityByCoupleId(coupleId);
        long provinceCount = footprintRepository.countDistinctProvinces(coupleId);
        long taskCount = taskRepository.countByCoupleIdAndStatus(coupleId, 2);
        long consecutiveSign = getMaxConsecutiveSignIns(coupleId);
        long capsuleCount = timeCapsuleRepository.countByCoupleIdAndStatusNot(coupleId, 0);
        long capsuleOpenCount = timeCapsuleRepository.countByCoupleIdAndStatus(coupleId, 3);
        long capsuleDualCount = timeCapsuleRepository.countDualCompleted(coupleId);
        long quizCount = dailyAnswerRepository.countByCoupleId(coupleId);
        long quizPerfectCount = dailyAnswerRepository.countPerfectMatches(coupleId);
        long quizDaysCount = dailyAnswerRepository.countAnswerDays(coupleId);
        int treeLevel = getLoveTreeLevel(coupleId);
        long signCount = signInRecordRepository.countByCoupleId(coupleId);

        Map<String, Object> best = null;
        double bestRatio = -1;

        for (Achievement ach : all) {
            if (userAchievementRepository.existsByCoupleIdAndAchievementId(coupleId, ach.getId())) {
                continue;
            }
            Map<String, Object> p = getProgress(ach.getCode(),
                    loveDays, diaryCount, diaryPhotoCount, cityCount, provinceCount,
                    taskCount, consecutiveSign, capsuleCount, capsuleOpenCount, capsuleDualCount,
                    quizCount, quizPerfectCount, quizDaysCount, treeLevel, signCount);
            if (p == null) continue;

            long current = (long) p.get("current");
            long target = (long) p.get("target");
            double ratio = (double) current / target;

            // 优先选择进度 > 0 且最接近完成的；若无进度则选第一个
            if (ratio > bestRatio || (bestRatio < 0 && current == 0)) {
                bestRatio = ratio;
                best = achievementToMap(ach, false);
                best.put("progress", current);
                best.put("target", target);
            }
        }
        return best;
    }

    /** 获取当前等级 (1~6) */
    private int getLevel(int percent) {
        if (percent >= 100) return 6;
        if (percent >= 80) return 5;
        if (percent >= 60) return 4;
        if (percent >= 40) return 3;
        if (percent >= 20) return 2;
        return 1;
    }

    private String getTitle(int percent) {
        if (percent >= 100) return "成就大师";
        if (percent >= 80) return "资深恋人";
        if (percent >= 60) return "爱情达人";
        if (percent >= 40) return "甜蜜恋人";
        if (percent >= 20) return "热恋新人";
        return "恋爱新手";
    }

    /** 获取下一等级的称号 */
    private String getNextTitle(int percent) {
        if (percent >= 100) return null;
        if (percent >= 80) return "成就大师";
        if (percent >= 60) return "资深恋人";
        if (percent >= 40) return "爱情达人";
        if (percent >= 20) return "甜蜜恋人";
        return "热恋新人";
    }

    /** 计算还需解锁多少个才能升到下一级 */
    private long getNeedForNext(long total, long unlocked, int percent) {
        if (percent >= 100) return 0;
        long nextThreshold;
        if (percent >= 80) nextThreshold = total;
        else if (percent >= 60) nextThreshold = total * 80 / 100;
        else if (percent >= 40) nextThreshold = total * 60 / 100;
        else if (percent >= 20) nextThreshold = total * 40 / 100;
        else nextThreshold = total * 20 / 100;
        long need = nextThreshold - unlocked;
        return Math.max(need, 1);
    }

    private boolean isConditionMet(Long coupleId, String code, Long userId) {
        try {
            switch (code) {
                // ❤️ 纪念类
                case "bind_couple":
                    return coupleRepository.findById(coupleId).isPresent();
                case "days_100": return getLoveDays(coupleId) >= 100;
                case "days_200": return getLoveDays(coupleId) >= 200;
                case "days_300": return getLoveDays(coupleId) >= 300;
                case "days_365": return getLoveDays(coupleId) >= 365;

                // 📷 日记类
                case "diary_1": return diaryRepository.countByCoupleId(coupleId) >= 1;
                case "diary_10": return diaryRepository.countByCoupleId(coupleId) >= 10;
                case "diary_50": return diaryRepository.countByCoupleId(coupleId) >= 50;
                case "diary_100": return diaryRepository.countByCoupleId(coupleId) >= 100;
                case "diary_photo_10": return diaryRepository.countPhotoDiaries(coupleId) >= 10;

                // 🌏 足迹类
                case "city_1": return footprintRepository.countDistinctCityByCoupleId(coupleId) >= 1;
                case "city_3": return footprintRepository.countDistinctCityByCoupleId(coupleId) >= 3;
                case "city_5": return footprintRepository.countDistinctCityByCoupleId(coupleId) >= 5;
                case "city_10": return footprintRepository.countDistinctCityByCoupleId(coupleId) >= 10;
                case "province_cross": return footprintRepository.countDistinctProvinces(coupleId) >= 2;

                // 🎯 任务类
                case "task_1": return taskRepository.countByCoupleIdAndStatus(coupleId, 2) >= 1;
                case "task_10": return taskRepository.countByCoupleIdAndStatus(coupleId, 2) >= 10;
                case "task_50": return taskRepository.countByCoupleIdAndStatus(coupleId, 2) >= 50;
                case "task_100": return taskRepository.countByCoupleIdAndStatus(coupleId, 2) >= 100;
                case "checkin_7": return getMaxConsecutiveSignIns(coupleId) >= 7;

                // 💌 胶囊类
                case "capsule_1": return timeCapsuleRepository.countByCoupleIdAndStatusNot(coupleId, 0) >= 1;
                case "capsule_open_1": return timeCapsuleRepository.countByCoupleIdAndStatus(coupleId, 3) >= 1;
                case "capsule_dual": return timeCapsuleRepository.countDualCompleted(coupleId) > 0;

                // 💬 问答类
                case "quiz_1": return dailyAnswerRepository.countByCoupleId(coupleId) >= 2;
                case "quiz_perfect": return dailyAnswerRepository.countPerfectMatches(coupleId) > 0;
                case "quiz_7": return dailyAnswerRepository.countAnswerDays(coupleId) >= 7;

                // 🌱 成长类
                case "tree_lv2": return getLoveTreeLevel(coupleId) >= 2;
                case "tree_lv5": return getLoveTreeLevel(coupleId) >= 5;
                case "tree_lv10": return getLoveTreeLevel(coupleId) >= 10;
                case "sign_30": return signInRecordRepository.countByCoupleId(coupleId) >= 30;

                default: return false;
            }
        } catch (Exception e) {
            log.warn("Failed to check achievement condition for code: {}", code, e);
            return false;
        }
    }

    private long getLoveDays(Long coupleId) {
        return coupleRepository.findById(coupleId)
                .map(c -> ChronoUnit.DAYS.between(c.getLoveDate(), LocalDate.now()))
                .orElse(0L);
    }

    private int getLoveTreeLevel(Long coupleId) {
        return loveTreeRepository.findByCoupleId(coupleId)
                .map(LoveTree::getLevel)
                .orElse(0);
    }

    /** 计算情侣最大连续签到天数 */
    private long getMaxConsecutiveSignIns(Long coupleId) {
        // 简化实现：查询情侣双方各自的最大连续签到，取较大值
        // 实际可改为专门查询
        try {
            List<SignInRecord> records = signInRecordRepository.findByCoupleIdOrderBySignDateDesc(coupleId);
            if (records.isEmpty()) return 0;

            // 按用户分组计算最大连续天数
            Map<Long, List<SignInRecord>> byUser = records.stream()
                    .collect(Collectors.groupingBy(SignInRecord::getUserId));

            long maxStreak = 0;
            for (List<SignInRecord> userRecords : byUser.values()) {
                userRecords.sort((a, b) -> b.getSignDate().compareTo(a.getSignDate()));
                long streak = 1;
                for (int i = 1; i < userRecords.size(); i++) {
                    long diff = ChronoUnit.DAYS.between(
                            userRecords.get(i).getSignDate(),
                            userRecords.get(i - 1).getSignDate());
                    if (diff == 1) {
                        streak++;
                    } else if (diff > 1) {
                        break;
                    }
                }
                maxStreak = Math.max(maxStreak, streak);
            }
            return maxStreak;
        } catch (Exception e) {
            log.warn("Failed to calculate consecutive sign-ins", e);
            return 0;
        }
    }
}
