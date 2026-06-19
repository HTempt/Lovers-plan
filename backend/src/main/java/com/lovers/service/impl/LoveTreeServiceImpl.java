package com.lovers.service.impl;

import com.lovers.model.LoveTree;
import com.lovers.model.LoveTreeGrowthRecord;
import com.lovers.repository.LoveTreeGrowthRecordRepository;
import com.lovers.repository.LoveTreeRepository;
import com.lovers.service.ILoveTreeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoveTreeServiceImpl implements ILoveTreeService {

    private static final Logger log = LoggerFactory.getLogger(LoveTreeServiceImpl.class);


    /** 等级阈值（各等级所需的最小成长值） */
    private static final int[] LEVEL_THRESHOLDS = {0, 200, 500, 1000, 2000, 5000, 10000};
    /** 等级名称 */
    private static final String[] LEVEL_NAMES = {"种子", "发芽", "幼苗", "小树", "开花", "结果", "永恒之树"};
    /** 等级图标 */
    private static final String[] LEVEL_ICONS = {"🌰", "🌱", "🌿", "🌳", "🌸", "🍎", "🌲"};

    @Autowired
    private LoveTreeRepository loveTreeRepository;

    @Autowired
    private LoveTreeGrowthRecordRepository growthRecordRepository;

    /**
     * 获取或初始化爱情树
     */
    @Transactional
    public LoveTree getOrCreateTree(Long coupleId) {
        return loveTreeRepository.findByCoupleId(coupleId)
                .orElseGet(() -> {
                    LoveTree tree = new LoveTree();
                    tree.setCoupleId(coupleId);
                    tree.setLevel(1);
                    tree.setGrowthValue(0);
                    return loveTreeRepository.save(tree);
                });
    }

    /**
     * 获取爱情树信息（含等级详情）
     */
    public Map<String, Object> getTreeInfo(Long coupleId) {
        LoveTree tree = getOrCreateTree(coupleId);
        return buildTreeInfoMap(tree);
    }

    /**
     * 添加成长值（核心方法）
     * @param coupleId 情侣ID
     * @param actionType 行为类型
     * @param growthValue 本次成长值
     * @param sourceId 关联记录ID（可选）
     * @param description 行为描述
     */
    @Transactional
    public Map<String, Object> addGrowth(Long coupleId, String actionType, int growthValue,
                                          Long sourceId, String description) {
        LoveTree tree = getOrCreateTree(coupleId);

        // 增加成长值
        int newGrowth = tree.getGrowthValue() + growthValue;
        tree.setGrowthValue(newGrowth);

        // 计算等级
        int newLevel = calculateLevel(newGrowth);
        boolean levelUp = newLevel > tree.getLevel();
        tree.setLevel(newLevel);

        loveTreeRepository.save(tree);

        // 记录成长记录
        LoveTreeGrowthRecord record = new LoveTreeGrowthRecord();
        record.setCoupleId(coupleId);
        record.setActionType(actionType);
        record.setGrowthValue(growthValue);
        record.setSourceId(sourceId);
        record.setDescription(description);
        growthRecordRepository.save(record);

        log.debug("LoveTree growth: coupleId={} action={} +{} total={} level={}",
                coupleId, actionType, growthValue, newGrowth, newLevel);

        Map<String, Object> result = buildTreeInfoMap(tree);
        result.put("levelUp", levelUp);
        result.put("growthAdded", growthValue);
        result.put("growthRecordId", record.getId());
        return result;
    }

    /**
     * 获取成长记录（分页）
     */
    public Map<String, Object> getGrowthHistory(Long coupleId, int page, int size) {
        Page<LoveTreeGrowthRecord> recordPage = growthRecordRepository
                .findByCoupleIdOrderByCreateTimeDesc(coupleId, PageRequest.of(page, size));

        Map<String, Object> result = new HashMap<>();
        result.put("items", recordPage.getContent());
        result.put("page", page);
        result.put("hasMore", recordPage.hasNext());
        result.put("total", recordPage.getTotalElements());
        return result;
    }

    /**
     * 根据成长值计算等级 (1~7)
     */
    public static int calculateLevel(int growthValue) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (growthValue >= LEVEL_THRESHOLDS[i]) {
                return i + 1; // Lv1 ~ Lv7
            }
        }
        return 1;
    }

    /**
     * 获取等级名称
     */
    public static String getLevelName(int level) {
        if (level < 1 || level > 7) return "未知";
        return LEVEL_NAMES[level - 1];
    }

    /**
     * 获取等级图标
     */
    public static String getLevelIcon(int level) {
        if (level < 1 || level > 7) return "🌱";
        return LEVEL_ICONS[level - 1];
    }

    /**
     * 构建爱情树信息Map
     */
    private Map<String, Object> buildTreeInfoMap(LoveTree tree) {
        Map<String, Object> info = new HashMap<>();
        int level = tree.getLevel();
        int growth = tree.getGrowthValue();

        info.put("id", tree.getId());
        info.put("coupleId", tree.getCoupleId());
        info.put("level", level);
        info.put("levelName", getLevelName(level));
        info.put("levelIcon", getLevelIcon(level));
        info.put("growthValue", growth);

        // 当前等级区间
        int currentThreshold = LEVEL_THRESHOLDS[Math.min(level - 1, LEVEL_THRESHOLDS.length - 1)];
        int nextThreshold;
        String nextLevelName;
        if (level < 7) {
            nextThreshold = LEVEL_THRESHOLDS[level];
            nextLevelName = getLevelName(level + 1);
        } else {
            nextThreshold = currentThreshold;
            nextLevelName = "满级";
        }

        info.put("currentThreshold", currentThreshold);
        info.put("nextThreshold", nextThreshold);
        info.put("nextLevelName", nextLevelName);

        // 进度百分比
        int range = nextThreshold - currentThreshold;
        int progress = range > 0 ? Math.min(100, (growth - currentThreshold) * 100 / range) : 100;
        info.put("progress", progress);

        return info;
    }

    /**
     * 获取所有等级信息
     */
    public static List<Map<String, Object>> getAllLevels() {
        java.util.List<Map<String, Object>> levels = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Map<String, Object> lv = new HashMap<>();
            lv.put("level", i + 1);
            lv.put("name", LEVEL_NAMES[i]);
            lv.put("icon", LEVEL_ICONS[i]);
            lv.put("threshold", LEVEL_THRESHOLDS[i]);
            levels.add(lv);
        }
        return levels;
    }

    /**
     * 获取解锁奖励信息
     */
    public static Map<Integer, String> getUnlockRewards() {
        Map<Integer, String> rewards = new HashMap<>();
        rewards.put(3, "情侣头像框");
        rewards.put(5, "爱情树主题");
        rewards.put(7, "专属年度报告");
        return rewards;
    }
}
