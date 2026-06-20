package com.lovers.service.impl;

import com.lovers.model.Activity;
import com.lovers.model.Diary;
import com.lovers.model.Task;
import com.lovers.model.Wish;
import com.lovers.repository.ActivityRepository;
import com.lovers.repository.DiaryRepository;
import com.lovers.repository.TaskRepository;
import com.lovers.repository.WishRepository;
import com.lovers.repository.TimeCapsuleRepository;
import com.lovers.service.IActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements IActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private TimeCapsuleRepository timeCapsuleRepository;

    /**
     * 获取情侣动态列表（分页，按时间倒序）
     * 自动过滤已软删除的资源对应的动态（如已删除的日记）
     */
    public Map<String, Object> getActivityFeed(Long coupleId, int page, int size) {
        Page<Activity> activityPage = activityRepository
                .findByCoupleIdOrderByCreateTimeDesc(coupleId, PageRequest.of(page, size));

        // 过滤已软删除的动态
        List<Activity> filtered = activityPage.getContent().stream()
                .filter(this::isSourceActive)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", filtered);
        result.put("page", page);
        result.put("hasMore", activityPage.hasNext());
        result.put("total", activityPage.getTotalElements());
        return result;
    }

    /**
     * 检查动态对应的源数据是否仍有效
     */
    private boolean isSourceActive(Activity activity) {
        if (activity.getRefId() == null) return true;
        try {
            switch (activity.getType()) {
                case "diary":
                    return diaryRepository.findById(activity.getRefId())
                            .map(d -> d.getStatus() == 1)
                            .orElse(false);
                case "capsule":
                    return timeCapsuleRepository.findById(activity.getRefId())
                            .map(c -> c.getStatus() == 1 || c.getStatus() == 2 || c.getStatus() == 3)
                            .orElse(false);
                case "quiz":
                    return true; // 问答记录不删除
                default:
                    return true;
            }
        } catch (Exception e) {
            log.warn("Failed to check source active for activity id={}", activity.getId());
            return true; // 查询出错时不过滤
        }
    }

    /**
     * 记录一条动态
     */
    @Transactional
    public Activity recordActivity(Long coupleId, String type, String title,
                                   String description, Long refId, String icon) {
        Activity activity = new Activity();
        activity.setCoupleId(coupleId);
        activity.setType(type);
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setRefId(refId);
        activity.setIcon(icon);
        Activity saved = activityRepository.save(activity);
        log.debug("Activity recorded: type={} coupleId={}", type, coupleId);
        return saved;
    }

    /**
     * 检查是否有动态数据，如果没有则从已有数据回填
     */
    @Transactional
    public void backfillIfEmpty(Long coupleId) {
        Page<Activity> existing = activityRepository
                .findByCoupleIdOrderByCreateTimeDesc(coupleId, PageRequest.of(0, 1));
        if (existing.hasContent()) return; // 已有动态，无需回填

        log.info("Backfilling activities for couple {}...", coupleId);
        int created = 0;

        // 回填日记
        List<Diary> diaries = diaryRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(coupleId, 1);
        for (Diary diary : diaries) {
            String content = diary.getContent();
            String desc = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
            recordActivityWithTime(coupleId, "diary", "📝 " + diary.getTitle(),
                    desc, diary.getId(), "📝", diary.getCreateTime());
            created++;
        }

        // 回填已完成的任务
        List<Task> tasks = taskRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId);
        for (Task task : tasks) {
            if (task.getStatus() == 2) { // 已完成
                recordActivityWithTime(coupleId, "task", "🎯 完成任务：" + task.getTitle(),
                        "累计完成 " + task.getTargetCount() + " 次 ✓",
                        task.getId(), "🎯", task.getUpdateTime() != null ? task.getUpdateTime() : task.getCreateTime());
                created++;
            }
        }

        // 回填已达成的愿望
        List<Wish> wishes = wishRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId);
        for (Wish wish : wishes) {
            if (wish.getStatus() == 2) { // 已达成
                recordActivityWithTime(coupleId, "wish", "✨ 愿望达成：" + wish.getTitle(),
                        "目标金额 " + wish.getTargetAmount() + " 已达成 ✓",
                        wish.getId(), "✨", wish.getUpdateTime() != null ? wish.getUpdateTime() : wish.getCreateTime());
                created++;
            }
        }

        if (created > 0) {
            log.info("Backfill complete: {} activities created for couple {}", created, coupleId);
        }
    }

    /**
     * 记录动态（可指定发生时间）
     */
    @Transactional
    protected Activity recordActivityWithTime(Long coupleId, String type, String title,
                                              String description, Long refId, String icon,
                                              java.time.LocalDateTime createTime) {
        Activity activity = new Activity();
        activity.setCoupleId(coupleId);
        activity.setType(type);
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setRefId(refId);
        activity.setIcon(icon);
        activity.setCreateTime(createTime);
        Activity saved = activityRepository.save(activity);
        return saved;
    }
}
