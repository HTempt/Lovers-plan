package com.lovers.service.impl;

import com.lovers.model.*;
import com.lovers.repository.*;
import com.lovers.service.IMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MemoryServiceImpl implements IMemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryServiceImpl.class);

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private DiaryMediaRepository diaryMediaRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private AnniversaryRepository anniversaryRepository;

    @Override
    public List<Map<String, Object>> getMemories(Long coupleId) {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        // 精确匹配当月当日（历史上的今天）
        List<MemoryCandidate> candidates = queryByMonthDay(coupleId, month, day);

        // 按优先级排序，取前3条
        candidates.sort(Comparator.comparingInt(MemoryCandidate::getPriority));

        return candidates.stream()
                .limit(3)
                .map(MemoryCandidate::toMap)
                .collect(Collectors.toList());
    }

    private List<MemoryCandidate> queryByMonthDay(Long coupleId, int month, int day) {
        List<MemoryCandidate> results = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. 带图片的日记（优先级 1）
        List<Diary> diaries = diaryRepository.findMemoriesByMonthDay(coupleId, month, day);
        for (Diary d : diaries) {
            List<DiaryMedia> mediaList = diaryMediaRepository.findByDiaryId(d.getId());
            boolean hasImage = mediaList.stream().anyMatch(m -> "image".equals(m.getMediaType()));
            if (hasImage) {
                String firstImage = mediaList.stream()
                        .filter(m -> "image".equals(m.getMediaType()))
                        .findFirst().map(DiaryMedia::getFileUrl).orElse(null);
                results.add(new MemoryCandidate("diary", "📷 " + d.getTitle(),
                        d.getContent(), firstImage, d.getCreateTime(), d.getId(), 1));
            }
        }

        // 2. 普通日记（优先级 2）
        for (Diary d : diaries) {
            List<DiaryMedia> mediaList = diaryMediaRepository.findByDiaryId(d.getId());
            boolean hasImage = mediaList.stream().anyMatch(m -> "image".equals(m.getMediaType()));
            if (!hasImage) {
                results.add(new MemoryCandidate("diary", "📝 " + d.getTitle(),
                        d.getContent(), null, d.getCreateTime(), d.getId(), 2));
            }
        }

        // 3. 纪念日（优先级 3）
        List<Anniversary> anniversaries = anniversaryRepository.findByMonthDay(coupleId, month, day);
        for (Anniversary a : anniversaries) {
            results.add(new MemoryCandidate("anniversary", "❤️ " + a.getTitle(),
                    "", null,
                    a.getAnniversaryDate() != null ? a.getAnniversaryDate().atStartOfDay() : today.atStartOfDay(),
                    a.getId(), 3));
        }

        // 4. 达成的愿望（优先级 4）
        List<Wish> wishes = wishRepository.findFulfilledByMonthDay(coupleId, month, day);
        for (Wish w : wishes) {
            results.add(new MemoryCandidate("wish", "✨ 愿望达成：" + w.getTitle(),
                    "目标金额 " + w.getTargetAmount() + " 已达成", null,
                    w.getUpdateTime() != null ? w.getUpdateTime() : today.atStartOfDay(),
                    w.getId(), 4));
        }

        // 5. 完成的任务（优先级 5）
        List<Task> tasks = taskRepository.findCompletedByMonthDay(coupleId, month, day);
        for (Task t : tasks) {
            results.add(new MemoryCandidate("task", "🎯 完成任务：" + t.getTitle(),
                    "累计完成 " + t.getTargetCount() + " 次", null,
                    t.getUpdateTime() != null ? t.getUpdateTime() : today.atStartOfDay(),
                    t.getId(), 5));
        }

        return results;
    }

    private List<MemoryCandidate> queryByMonthDayRange(Long coupleId, int month, int startDay, int endDay, int targetDay) {
        List<MemoryCandidate> results = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 带图片日记
        List<Diary> diaries = diaryRepository.findMemoriesByMonthDayRange(coupleId, month, startDay, endDay, targetDay);
        Set<Long> seenDiaryIds = new HashSet<>();
        for (Diary d : diaries) {
            if (seenDiaryIds.contains(d.getId())) continue;
            seenDiaryIds.add(d.getId());
            List<DiaryMedia> mediaList = diaryMediaRepository.findByDiaryId(d.getId());
            boolean hasImage = mediaList.stream().anyMatch(m -> "image".equals(m.getMediaType()));
            String image = hasImage ? mediaList.stream().filter(m -> "image".equals(m.getMediaType()))
                    .findFirst().map(DiaryMedia::getFileUrl).orElse(null) : null;
            results.add(new MemoryCandidate("diary", hasImage ? "📷 " + d.getTitle() : "📝 " + d.getTitle(),
                    d.getContent(), image, d.getCreateTime(), d.getId(), 1));
        }

        // 纪念日
        List<Anniversary> anniversaries = anniversaryRepository.findByMonthDayRange(coupleId, month, startDay, endDay, targetDay);
        for (Anniversary a : anniversaries) {
            results.add(new MemoryCandidate("anniversary", "❤️ " + a.getTitle(),
                    "", null,
                    a.getAnniversaryDate() != null ? a.getAnniversaryDate().atStartOfDay() : today.atStartOfDay(),
                    a.getId(), 3));
        }

        // 愿望
        List<Wish> wishes = wishRepository.findFulfilledByMonthDayRange(coupleId, month, startDay, endDay, targetDay);
        for (Wish w : wishes) {
            results.add(new MemoryCandidate("wish", "✨ 愿望达成：" + w.getTitle(),
                    "目标金额 " + w.getTargetAmount() + " 已达成", null,
                    w.getUpdateTime() != null ? w.getUpdateTime() : today.atStartOfDay(),
                    w.getId(), 4));
        }

        // 任务
        List<Task> tasks = taskRepository.findCompletedByMonthDayRange(coupleId, month, startDay, endDay, targetDay);
        for (Task t : tasks) {
            results.add(new MemoryCandidate("task", "🎯 完成任务：" + t.getTitle(),
                    "累计完成 " + t.getTargetCount() + " 次", null,
                    t.getUpdateTime() != null ? t.getUpdateTime() : today.atStartOfDay(),
                    t.getId(), 5));
        }

        return results;
    }

    /**
     * 内部候选对象
     */
    private static class MemoryCandidate {
        final String type;
        final String title;
        final String description;
        final String image;
        final LocalDateTime sourceTime;
        final Long refId;
        final int priority;

        MemoryCandidate(String type, String title, String description, String image,
                        LocalDateTime sourceTime, Long refId, int priority) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.image = image;
            this.sourceTime = sourceTime;
            this.refId = refId;
            this.priority = priority;
        }

        int getPriority() { return priority; }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", type);
            map.put("title", title);
            map.put("description", description != null && description.length() > 80
                    ? description.substring(0, 80) + "..." : description);
            map.put("image", image);
            map.put("refId", refId);
            // 计算距今时间标签
            if (sourceTime != null) {
                LocalDate sourceDate = sourceTime.toLocalDate();
                long years = ChronoUnit.YEARS.between(sourceDate, LocalDate.now());
                if (years >= 1) {
                    map.put("offsetLabel", years + "年前");
                } else {
                    long months = ChronoUnit.MONTHS.between(sourceDate, LocalDate.now());
                    if (months >= 1) {
                        map.put("offsetLabel", months + "个月前");
                    } else {
                        long days = ChronoUnit.DAYS.between(sourceDate, LocalDate.now());
                        map.put("offsetLabel", days + "天前");
                    }
                }
                map.put("sourceDate", sourceDate.toString());
            } else {
                map.put("offsetLabel", "");
                map.put("sourceDate", "");
            }
            return map;
        }
    }
}
