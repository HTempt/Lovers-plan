package com.lovers.service.impl;

import com.lovers.model.Couple;
import com.lovers.model.LoveTree;
import com.lovers.model.User;
import com.lovers.repository.*;
import com.lovers.service.IDailySentenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DailySentenceServiceImpl implements IDailySentenceService {

    private static final Logger log = LoggerFactory.getLogger(DailySentenceServiceImpl.class);

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private LoveTreeRepository loveTreeRepository;

    /** 等级名称 */
    private static final String[] LEVEL_NAMES = {"种子", "发芽", "幼苗", "小树", "开花", "结果", "永恒之树"};

    /** 句子条目：文字 + 对应页面 */
    private static class SentenceEntry {
        final String text;
        final String page; // 空=不跳转, diary/task/wish/love-tree
        SentenceEntry(String text, String page) {
            this.text = text;
            this.page = page;
        }
    }

    /**
     * 为情侣生成一句今日话语（含跳转页面）
     */
    public Map<String, Object> generate(Long coupleId, Long userId) {
        Couple couple = coupleRepository.findById(coupleId).orElse(null);
        if (couple == null) return Map.of("text", "开启你们的美好一天吧 💕", "page", "");

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Map.of("text", "开启你们的美好一天吧 💕", "page", "");

        // 收集各项数据
        LocalDate loveDate = couple.getLoveDate();
        long loveDays = loveDate != null ? ChronoUnit.DAYS.between(loveDate, LocalDate.now()) : 0;

        User partner = getPartner(couple, userId);
        String partnerName = partner != null && partner.getNickname() != null && !partner.getNickname().isEmpty()
                ? partner.getNickname() : "对方";

        long diaryCount = diaryRepository.countByCoupleIdAndStatus(coupleId, 1);
        long completedTasks = taskRepository.countByCoupleIdAndStatus(coupleId, 2);
        long activeTasks = taskRepository.countByCoupleIdAndStatus(coupleId, 1);
        long completedWishes = wishRepository.countByCoupleIdAndStatus(coupleId, 2);

        LoveTree tree = loveTreeRepository.findByCoupleId(coupleId).orElse(null);
        int treeLevel = tree != null ? tree.getLevel() : 1;
        int growthValue = tree != null ? tree.getGrowthValue() : 0;
        int nextThreshold = treeLevel < 7 ? getThreshold(treeLevel) : getThreshold(6);

        // 构建句子池
        List<SentenceEntry> sentences = new ArrayList<>();
        Random rng = ThreadLocalRandom.current();

        // 1. 周末数 → 不跳转
        if (loveDays >= 7) {
            long weekends = loveDays / 7;
            sentences.add(new SentenceEntry(
                    "你们已经一起度过了 " + weekends + " 个周末。", ""));
        }

        // 2. 日记 → 跳转日记页
        if (diaryCount >= 5) {
            sentences.add(new SentenceEntry(
                    "你们已经一起写下了 " + diaryCount + " 篇日记，每一页都是爱的证明 📖", "diary"));
        } else if (diaryCount > 0) {
            sentences.add(new SentenceEntry(
                    "你们已经写了 " + diaryCount + " 篇日记了，继续记录美好吧 📝", "diary"));
        }

        // 3. 任务完成 → 跳转任务页
        if (completedTasks >= 3) {
            sentences.add(new SentenceEntry(
                    "你们已经共同完成了 " + completedTasks + " 个任务，超有默契！🎯", "task"));
        } else if (completedTasks > 0) {
            sentences.add(new SentenceEntry(
                    partnerName + " 和你一起完成了 " + completedTasks + " 个任务 🎯", "task"));
        }

        // 4. 进行中任务 → 跳转任务页
        if (activeTasks > 0) {
            sentences.add(new SentenceEntry(
                    "还有 " + activeTasks + " 个任务在等着你们，加油！💪", "task"));
        }

        // 5. 愿望 → 跳转愿望页
        if (completedWishes > 0) {
            sentences.add(new SentenceEntry(
                    "你们已经共同实现了 " + completedWishes + " 个愿望 ✨", "wish"));
        }

        // 6. 爱情树 → 跳转爱情树页
        if (treeLevel < 7) {
            int need = nextThreshold - growthValue;
            sentences.add(new SentenceEntry(
                    "爱情树还需要 " + need + " 成长值就能升级到 Lv" + (treeLevel + 1) + " " + LEVEL_NAMES[treeLevel] + " 啦 🌱", "love-tree"));
        } else {
            sentences.add(new SentenceEntry(
                    "爱情树已是永恒之树，你们的爱坚不可摧 🌲", "love-tree"));
        }

        // 7. 兜底 → 不跳转
        sentences.add(new SentenceEntry("今天也要一起创造美好回忆 💕", ""));

        // 随机选一句
        SentenceEntry picked = sentences.get(rng.nextInt(sentences.size()));
        return Map.of("text", picked.text, "page", picked.page);
    }

    private User getPartner(Couple couple, Long userId) {
        Long partnerId;
        if (couple.getUserA() != null && couple.getUserA().equals(userId)) {
            partnerId = couple.getUserB();
        } else if (couple.getUserB() != null && couple.getUserB().equals(userId)) {
            partnerId = couple.getUserA();
        } else {
            return null;
        }
        return partnerId != null ? userRepository.findById(partnerId).orElse(null) : null;
    }

    private int getThreshold(int level) {
        int[] thresholds = {0, 200, 500, 1000, 2000, 5000, 10000};
        if (level < 1) return thresholds[0];
        if (level > 7) return thresholds[6];
        return thresholds[level - 1];
    }
}
