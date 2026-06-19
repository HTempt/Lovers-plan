package com.lovers.service.impl;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.TimeCapsule;
import com.lovers.model.TimeCapsuleMedia;
import com.lovers.repository.ActivityRepository;
import com.lovers.repository.TimeCapsuleMediaRepository;
import com.lovers.repository.TimeCapsuleRepository;
import com.lovers.service.IActivityService;
import com.lovers.service.ILoveTreeService;
import com.lovers.service.ITimeCapsuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimeCapsuleServiceImpl implements ITimeCapsuleService {

    private static final Logger log = LoggerFactory.getLogger(TimeCapsuleServiceImpl.class);

    @Autowired
    private TimeCapsuleRepository timeCapsuleRepository;

    @Autowired
    private TimeCapsuleMediaRepository mediaRepository;

    @Autowired
    private IActivityService activityService;

    @Autowired
    private ILoveTreeService loveTreeService;

    @Autowired
    private ActivityRepository activityRepository;

    /** 状态常量 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_SEALED = 1;
    private static final int STATUS_OPENABLE = 2;
    private static final int STATUS_OPENED = 3;

    @Override
    @Transactional
    public TimeCapsule create(Long coupleId, Long userId, String type, String title, String content,
                               List<Map<String, String>> mediaList, LocalDateTime openAt, Boolean dualMode) {
        // 校验参数
        if (title == null || title.isEmpty()) {
            throw new BusinessException("胶囊标题不能为空");
        }
        if (!TYPES.contains(type)) {
            throw new BusinessException("无效的胶囊类型");
        }
        if (openAt == null) {
            throw new BusinessException("请设置开启时间");
        }
        if (openAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException("开启时间不能早于当前时间");
        }

        // 双人模式：默认不写死，由 caller 指定
        boolean isDual = dualMode != null && dualMode;

        TimeCapsule capsule = new TimeCapsule();
        capsule.setCoupleId(coupleId);
        capsule.setUserId(userId);
        capsule.setType(type);
        capsule.setTitle(title);
        capsule.setContent(content);
        capsule.setOpenAt(openAt);
        // 双人模式初始为草稿，单人模式直接封存
        capsule.setStatus(isDual ? STATUS_DRAFT : STATUS_SEALED);
        TimeCapsule saved = timeCapsuleRepository.save(capsule);

        // 保存媒体
        saveMedia(saved.getId(), mediaList);

        // 单人模式：直接记录动态 + 爱情树成长值
        if (!isDual) {
            recordSealedActivity(saved);
            addLoveTreeGrowth(saved, ILoveTreeService.GROWTH_CAPSULE_CREATE, "创建胶囊：" + saved.getTitle());
        }

        log.debug("TimeCapsule created: id={} coupleId={} dualMode={}", saved.getId(), coupleId, isDual);
        return saved;
    }

    @Override
    @Transactional
    public TimeCapsule writePartner(Long coupleId, Long userId, Long pairCapsuleId,
                                     String content, List<Map<String, String>> mediaList) {
        // 查找发起方胶囊
        TimeCapsule pairCapsule = timeCapsuleRepository.findById(pairCapsuleId)
                .orElseThrow(() -> new BusinessException("胶囊不存在"));

        if (!pairCapsule.getCoupleId().equals(coupleId)) {
            throw new BusinessException("无权操作");
        }
        if (pairCapsule.getStatus() != STATUS_DRAFT) {
            throw new BusinessException("该胶囊已封存，无法再写入");
        }
        if (pairCapsule.getUserId().equals(userId)) {
            throw new BusinessException("不能对自己发起的胶囊写入");
        }
        if (pairCapsule.getPairId() != null) {
            throw new BusinessException("对方已写入");
        }

        // 创建对方的胶囊记录
        TimeCapsule partnerCapsule = new TimeCapsule();
        partnerCapsule.setCoupleId(coupleId);
        partnerCapsule.setUserId(userId);
        partnerCapsule.setType(pairCapsule.getType());
        partnerCapsule.setTitle(pairCapsule.getTitle());
        partnerCapsule.setContent(content);
        partnerCapsule.setOpenAt(pairCapsule.getOpenAt());
        partnerCapsule.setStatus(STATUS_SEALED);
        partnerCapsule.setPairId(pairCapsuleId);
        TimeCapsule savedPartner = timeCapsuleRepository.save(partnerCapsule);

        // 保存媒体
        saveMedia(savedPartner.getId(), mediaList);

        // 更新发起方：关联 pairId + 封存
        pairCapsule.setPairId(pairCapsuleId);
        pairCapsule.setStatus(STATUS_SEALED);
        timeCapsuleRepository.save(pairCapsule);

        // 记录动态
        recordSealedActivity(pairCapsule);
        // 双方都加成长值
        addLoveTreeGrowth(pairCapsule, ILoveTreeService.GROWTH_CAPSULE_CREATE, "创建胶囊：" + pairCapsule.getTitle());
        addLoveTreeGrowth(savedPartner, ILoveTreeService.GROWTH_CAPSULE_CREATE, "创建胶囊：" + savedPartner.getTitle());

        log.debug("TimeCapsule partner written: pairId={} partnerId={}", pairCapsuleId, savedPartner.getId());
        return savedPartner;
    }

    @Override
    public Map<String, Object> list(Long coupleId, Integer status, int page, int size) {
        Page<TimeCapsule> capsulePage;
        if (status != null) {
            capsulePage = timeCapsuleRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(
                    coupleId, status, PageRequest.of(page, size));
        } else {
            capsulePage = timeCapsuleRepository.findByCoupleIdOrderByCreateTimeDesc(
                    coupleId, PageRequest.of(page, size));
        }

        List<Map<String, Object>> items = capsulePage.getContent().stream()
                .map(this::toCapsuleMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("hasMore", capsulePage.hasNext());
        result.put("total", capsulePage.getTotalElements());
        return result;
    }

    @Override
    public Map<String, Object> detail(Long coupleId, Long capsuleId) {
        TimeCapsule capsule = timeCapsuleRepository.findByIdAndCoupleId(capsuleId, coupleId)
                .orElseThrow(() -> new BusinessException("胶囊不存在"));

        boolean canViewContent = capsule.getStatus() == 3; // 仅 OPENED 可看内容

        Map<String, Object> result = toCapsuleMap(capsule, canViewContent);

        // 加载媒体（仅 OPENED 可见）
        if (canViewContent) {
            List<TimeCapsuleMedia> mediaList = mediaRepository.findByCapsuleId(capsuleId);
            result.put("mediaList", mediaList);
        } else {
            result.put("mediaList", List.of());
            result.put("hint", capsule.getStatus() == 1
                    ? "💌 内容已封存，开启后方可查看"
                    : "📬 时光胶囊已成熟，点击开启");
        }

        // 双人模式：加载对方的内容（仅 OPENED 可见）
        if (capsule.getPairId() != null && canViewContent) {
            List<TimeCapsule> pairs = timeCapsuleRepository.findByPairId(capsule.getPairId());
            Optional<TimeCapsule> partnerCapsule = pairs.stream()
                    .filter(c -> !c.getUserId().equals(capsule.getUserId()))
                    .findFirst();
            if (partnerCapsule.isPresent()) {
                TimeCapsule partner = partnerCapsule.get();
                Map<String, Object> partnerMap = new HashMap<>();
                partnerMap.put("id", partner.getId());
                partnerMap.put("userId", partner.getUserId());
                partnerMap.put("content", partner.getContent());
                partnerMap.put("mediaList", mediaRepository.findByCapsuleId(partner.getId()));
                result.put("partner", partnerMap);
            }
        }

        // 计算封存时长（天数）
        if (capsule.getCreateTime() != null && capsule.getOpenAt() != null) {
            long days = java.time.Duration.between(capsule.getCreateTime(), capsule.getOpenAt()).toDays();
            result.put("sealDays", days);
        }

        return result;
    }

    @Override
    @Transactional
    public TimeCapsule open(Long coupleId, Long capsuleId) {
        TimeCapsule capsule = timeCapsuleRepository.findByIdAndCoupleId(capsuleId, coupleId)
                .orElseThrow(() -> new BusinessException("胶囊不存在"));

        if (capsule.getStatus() != STATUS_OPENABLE) {
            throw new BusinessException("该胶囊目前无法开启");
        }

        capsule.setStatus(STATUS_OPENED);
        capsule.setOpenedAt(LocalDateTime.now());
        TimeCapsule saved = timeCapsuleRepository.save(capsule);

        // 记录动态
        try {
            activityService.recordActivity(coupleId, "capsule",
                    "💌 一颗时光胶囊被开启",
                    saved.getTitle(),
                    saved.getId(), "💌");
        } catch (Exception e) {
            log.warn("Failed to record capsule open activity", e);
        }

        // 爱情树成长值 +20
        try {
            loveTreeService.addGrowth(coupleId, "capsule_open",
                    ILoveTreeService.GROWTH_CAPSULE_OPEN, saved.getId(),
                    "开启胶囊：" + saved.getTitle());
        } catch (Exception e) {
            log.warn("Failed to add love tree growth for capsule open", e);
        }

        log.debug("TimeCapsule opened: id={}", capsuleId);
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long coupleId, Long capsuleId) {
        TimeCapsule capsule = timeCapsuleRepository.findByIdAndCoupleId(capsuleId, coupleId)
                .orElseThrow(() -> new BusinessException("胶囊不存在"));

        if (capsule.getStatus() == STATUS_OPENED) {
            throw new BusinessException("已开启的胶囊无法删除");
        }

        // 删除媒体
        mediaRepository.deleteByCapsuleId(capsuleId);

        // 删除关联动态
        activityRepository.deleteByRefIdAndType(capsuleId, "capsule");

        // 如果是双人模式发起方，同时删除对方的记录
        if (capsule.getPairId() != null && capsule.getPairId().equals(capsuleId)) {
            List<TimeCapsule> pairs = timeCapsuleRepository.findByPairId(capsuleId);
            for (TimeCapsule pair : pairs) {
                if (!pair.getId().equals(capsuleId)) {
                    mediaRepository.deleteByCapsuleId(pair.getId());
                    activityRepository.deleteByRefIdAndType(pair.getId(), "capsule");
                    timeCapsuleRepository.delete(pair);
                }
            }
        }

        timeCapsuleRepository.delete(capsule);
        log.debug("TimeCapsule deleted: id={}", capsuleId);
    }

    /**
     * 定时任务：每分钟检查到期的胶囊，将 SEALED → OPENABLE
     */
    @Override
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public int checkAndUpdateMatureCapsules() {
        List<TimeCapsule> matureCapsules = timeCapsuleRepository.findMatureCapsules(LocalDateTime.now());
        int count = 0;
        for (TimeCapsule capsule : matureCapsules) {
            capsule.setStatus(STATUS_OPENABLE);
            timeCapsuleRepository.save(capsule);
            count++;
            log.debug("TimeCapsule matured: id={} openAt={}", capsule.getId(), capsule.getOpenAt());
        }
        if (count > 0) {
            log.info("TimeCapsule scheduler: {} capsules updated to OPENABLE", count);
        }
        return count;
    }

    // ========== 私有辅助方法 ==========

    private void saveMedia(Long capsuleId, List<Map<String, String>> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) return;
        for (Map<String, String> media : mediaList) {
            TimeCapsuleMedia mediaEntity = new TimeCapsuleMedia();
            mediaEntity.setCapsuleId(capsuleId);
            mediaEntity.setMediaType(media.get("mediaType"));
            mediaEntity.setFileUrl(media.get("fileUrl"));
            mediaRepository.save(mediaEntity);
        }
    }

    private void recordSealedActivity(TimeCapsule capsule) {
        try {
            String typeLabel = getTypeLabel(capsule.getType());
            activityService.recordActivity(capsule.getCoupleId(), "capsule",
                    "💌 " + typeLabel + "：" + capsule.getTitle(),
                    "你们封存了一颗时光胶囊",
                    capsule.getId(), "💌");
        } catch (Exception e) {
            log.warn("Failed to record capsule activity", e);
        }
    }

    private void addLoveTreeGrowth(TimeCapsule capsule, int growthValue, String description) {
        try {
            loveTreeService.addGrowth(capsule.getCoupleId(), "capsule",
                    growthValue, capsule.getId(), description);
        } catch (Exception e) {
            log.warn("Failed to add love tree growth for capsule", e);
        }
    }

    private Map<String, Object> toCapsuleMap(TimeCapsule capsule) {
        return toCapsuleMap(capsule, true);
    }

    private Map<String, Object> toCapsuleMap(TimeCapsule capsule, boolean showContent) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", capsule.getId());
        map.put("coupleId", capsule.getCoupleId());
        map.put("userId", capsule.getUserId());
        map.put("type", capsule.getType());
        map.put("typeLabel", getTypeLabel(capsule.getType()));
        map.put("title", capsule.getTitle());
        map.put("content", showContent ? capsule.getContent() : null);
        map.put("openAt", capsule.getOpenAt());
        map.put("openedAt", capsule.getOpenedAt());
        map.put("status", capsule.getStatus());
        map.put("statusLabel", getStatusLabel(capsule.getStatus()));
        map.put("pairId", capsule.getPairId());
        map.put("createTime", capsule.getCreateTime());
        map.put("updateTime", capsule.getUpdateTime());
        // 封存时长（天数）
        if (capsule.getCreateTime() != null && capsule.getOpenAt() != null) {
            long days = java.time.Duration.between(capsule.getCreateTime(), capsule.getOpenAt()).toDays();
            map.put("sealDays", days);
        }
        return map;
    }

    private String getTypeLabel(String type) {
        switch (type) {
            case "to_future_ta": return "给未来的TA";
            case "to_future_us": return "给未来的我们";
            case "birthday":     return "生日胶囊";
            case "anniversary":  return "纪念日胶囊";
            case "wish":         return "愿望达成胶囊";
            default:             return "时光胶囊";
        }
    }

    private String getStatusLabel(Integer status) {
        switch (status) {
            case 0: return "草稿";
            case 1: return "已封存";
            case 2: return "可开启";
            case 3: return "已开启";
            default: return "未知";
        }
    }
}
