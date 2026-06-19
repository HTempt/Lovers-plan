package com.lovers.service.impl;

import com.lovers.service.IDiaryService;
import com.lovers.service.IFileService;
import com.lovers.service.IActivityService;
import com.lovers.service.ILoveTreeService;
import com.lovers.service.IFootprintService;
import com.lovers.common.exception.BusinessException;
import com.lovers.model.Diary;
import com.lovers.model.DiaryMedia;
import com.lovers.model.User;
import com.lovers.repository.ActivityRepository;
import com.lovers.repository.DiaryMediaRepository;
import com.lovers.repository.DiaryRepository;
import com.lovers.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiaryServiceImpl implements IDiaryService {

    private static final Logger log = LoggerFactory.getLogger(DiaryServiceImpl.class);

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private DiaryMediaRepository diaryMediaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IFileService fileService;

    @Autowired
    private IActivityService activityService;

    @Autowired
    private ILoveTreeService loveTreeService;

    @Autowired
    private IFootprintService footprintService;

    @Autowired
    private ActivityRepository activityRepository;

    /**
     * 创建日记
     */
    @Transactional
    public Diary create(Long userId, Long coupleId, String title, String content,
                        String location, String province, String city,
                        BigDecimal latitude, BigDecimal longitude,
                        List<Map<String, String>> mediaList) {
        if (title == null || title.isEmpty()) {
            throw new BusinessException("日记标题不能为空");
        }

        Diary diary = new Diary();
        diary.setCoupleId(coupleId);
        diary.setCreatorId(userId);
        diary.setTitle(title);
        diary.setContent(content);
        diary.setLocation(location);
        diary.setProvince(province);
        diary.setCity(city);
        diary.setLatitude(latitude);
        diary.setLongitude(longitude);
        diary.setStatus(1);
        diary = diaryRepository.save(diary);

        // 保存媒体
        if (mediaList != null && !mediaList.isEmpty()) {
            for (Map<String, String> media : mediaList) {
                DiaryMedia dm = new DiaryMedia();
                dm.setDiaryId(diary.getId());
                dm.setMediaType(media.getOrDefault("mediaType", "image"));
                dm.setFileUrl(media.get("fileUrl"));
                diaryMediaRepository.save(dm);
            }
        }

        // 记录动态
        try {
            String desc = content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
            activityService.recordActivity(coupleId, "diary", "📝 " + title, desc, diary.getId(), "📝");
        } catch (Exception e) {
            log.warn("Failed to record diary activity", e);
        }

        // 爱情树成长值 +5
        try {
            loveTreeService.addGrowth(coupleId, "diary", ILoveTreeService.GROWTH_DIARY,
                    diary.getId(), "写日记：" + title);
        } catch (Exception e) {
            log.warn("Failed to add love tree growth for diary", e);
        }

        // 创建足迹
        if (city != null && !city.isEmpty() && latitude != null && longitude != null) {
            try {
                footprintService.create(coupleId, diary.getId(), province, city, location, latitude, longitude);
            } catch (Exception e) {
                log.warn("Failed to create footprint", e);
            }
        }

        return diary;
    }

    /**
     * 获取时间轴（分页）
     */
    public Map<String, Object> getTimeline(Long coupleId, int page, int size) {
        Page<Diary> diaryPage = diaryRepository
                .findByCoupleIdAndStatusOrderByCreateTimeDesc(coupleId, 1, PageRequest.of(page, size));

        List<Map<String, Object>> items = diaryPage.getContent().stream().map(diary -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", diary.getId());
            item.put("title", diary.getTitle());
            item.put("content", diary.getContent());
            item.put("location", diary.getLocation());
            item.put("createTime", diary.getCreateTime());

            // 加载媒体
            List<DiaryMedia> mediaList = diaryMediaRepository.findByDiaryId(diary.getId());
            item.put("mediaList", mediaList.stream().map(m -> {
                Map<String, Object> mediaMap = new HashMap<>();
                mediaMap.put("id", m.getId());
                mediaMap.put("mediaType", m.getMediaType());
                mediaMap.put("fileUrl", fileService.getFileUrl(m.getFileUrl()));
                return mediaMap;
            }).collect(Collectors.toList()));

            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("hasMore", diaryPage.hasNext());
        result.put("total", diaryPage.getTotalElements());
        return result;
    }

    /**
     * 获取相册（按月聚合）
     */
    public Map<String, List<Map<String, Object>>> getAlbum(Long coupleId) {
        List<Diary> diaries = diaryRepository
                .findByCoupleIdAndStatusOrderByCreateTimeDesc(coupleId, 1);

        Map<String, List<Map<String, Object>>> album = new LinkedHashMap<>();

        for (Diary diary : diaries) {
            String monthKey = diary.getCreateTime().toLocalDate().toString().substring(0, 7); // YYYY-MM
            List<DiaryMedia> mediaList = diaryMediaRepository.findByDiaryId(diary.getId());

            if (mediaList.isEmpty()) continue;

            album.computeIfAbsent(monthKey, k -> new ArrayList<>());
            for (DiaryMedia media : mediaList) {
                Map<String, Object> item = new HashMap<>();
                item.put("mediaId", media.getId());
                item.put("mediaType", media.getMediaType());
                item.put("fileUrl", fileService.getFileUrl(media.getFileUrl()));
                item.put("diaryId", diary.getId());
                item.put("diaryTitle", diary.getTitle());
                album.get(monthKey).add(item);
            }
        }

        return album;
    }

    /**
     * 获取地图足迹
     */
    public List<Map<String, Object>> getMapLocations(Long coupleId) {
        List<String> locations = diaryRepository.findDistinctLocationsByCoupleId(coupleId, 1);

        return locations.stream().map(loc -> {
            Map<String, Object> item = new HashMap<>();
            item.put("location", loc);
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 删除日记（移入回收站）
     */
    @Transactional
    public void delete(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdAndCreatorId(diaryId, userId)
                .orElseThrow(() -> new BusinessException("日记不存在或无权删除"));

        diary.setStatus(0);
        diary.setDeleteTime(LocalDateTime.now());
        diaryRepository.save(diary);

        // 删除关联的岛屿动态
        try {
            activityRepository.deleteByRefIdAndType(diaryId, "diary");
        } catch (Exception e) {
            log.warn("Failed to delete diary activity", e);
        }
    }

    /**
     * 恢复日记
     */
    @Transactional
    public void restore(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findByIdAndCreatorId(diaryId, userId)
                .orElseThrow(() -> new BusinessException("日记不存在或无权恢复"));

        if (diary.getStatus() != 0) {
            throw new BusinessException("日记未在回收站中");
        }

        diary.setStatus(1);
        diary.setDeleteTime(null);
        diaryRepository.save(diary);
    }

    /**
     * 获取回收站列表
     */
    public List<Diary> getRecycleBin(Long coupleId) {
        return diaryRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(coupleId, 0);
    }

    /**
     * 清理过期回收站（30天）- 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanExpiredRecycleBin() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Diary> expired = diaryRepository.findByStatusAndDeleteTimeBefore(0, thirtyDaysAgo);

        for (Diary diary : expired) {
            // 删除关联媒体
            List<DiaryMedia> mediaList = diaryMediaRepository.findByDiaryId(diary.getId());
            for (DiaryMedia media : mediaList) {
                fileService.delete(media.getFileUrl());
            }
            diaryMediaRepository.deleteByDiaryId(diary.getId());
            diaryRepository.delete(diary);
        }

        if (!expired.isEmpty()) {
            log.info("Cleaned {} expired diary entries from recycle bin", expired.size());
        }
    }
}
