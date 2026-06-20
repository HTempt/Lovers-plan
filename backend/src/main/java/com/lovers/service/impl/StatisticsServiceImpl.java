package com.lovers.service.impl;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.Couple;
import com.lovers.model.User;
import com.lovers.repository.*;
import com.lovers.service.IStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private DiaryMediaRepository diaryMediaRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private AnniversaryRepository anniversaryRepository;

    @Autowired
    private FootprintRepository footprintRepository;

    /**
     * 获取情侣统计概览
     */
    public Map<String, Object> getOverview(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        Map<String, Object> stats = new HashMap<>();

        if (user.getCoupleId() == null) {
            stats.put("hasCouple", false);
            return stats;
        }

        Couple couple = coupleRepository.findByIdAndStatus(user.getCoupleId(), 1)
                .orElseThrow(() -> new BusinessException("情侣关系不存在"));

        Long coupleId = couple.getId();
        stats.put("hasCouple", true);

        // 恋爱天数
        if (couple.getLoveDate() != null) {
            long loveDays = ChronoUnit.DAYS.between(couple.getLoveDate(), LocalDate.now());
            stats.put("loveDays", loveDays);
        }

        // 日记统计
        long diaryCount = diaryRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(coupleId, 1).size();
        stats.put("diaryCount", diaryCount);

        // 照片统计
        long photoCount = countMediaByType(coupleId, "image");
        stats.put("photoCount", photoCount);

        // 视频统计
        long videoCount = countMediaByType(coupleId, "video");
        stats.put("videoCount", videoCount);

        // 待办统计
        long totalTodos = todoRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId).size();
        long completedTodos = todoRepository.countByCoupleIdAndStatus(coupleId, 1);
        stats.put("totalTodos", totalTodos);
        stats.put("completedTodos", completedTodos);

        // 任务统计
        long totalTasks = taskRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId).size();
        long activeTasks = taskRepository.countByCoupleIdAndStatus(coupleId, 1);
        long completedTasks = taskRepository.countByCoupleIdAndStatus(coupleId, 2);
        stats.put("totalTasks", totalTasks);
        stats.put("activeTasks", activeTasks);
        stats.put("completedTasks", completedTasks);

        // 愿望统计
        long totalWishes = wishRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId).size();
        long achievedWishes = 0;
        if (totalWishes > 0) {
            achievedWishes = wishRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId)
                    .stream().filter(w -> w.getStatus() == 2).count();
        }
        stats.put("totalWishes", totalWishes);
        stats.put("achievedWishes", achievedWishes);

        // 纪念日统计
        long anniversaryCount = anniversaryRepository
                .findByCoupleIdAndStatusOrderByAnniversaryDateDesc(coupleId, 1).size();
        stats.put("anniversaryCount", anniversaryCount);

        // 共同城市数（足迹系统）
        long cityCount = footprintRepository.countDistinctCityByCoupleId(coupleId);
        stats.put("locationCount", cityCount);

        return stats;
    }

    private long countMediaByType(Long coupleId, String mediaType) {
        return diaryRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(coupleId, 1)
                .stream()
                .flatMap(diary -> diaryMediaRepository.findByDiaryId(diary.getId()).stream())
                .filter(media -> mediaType.equals(media.getMediaType()))
                .count();
    }
}
