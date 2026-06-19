package com.lovers.service;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.Wish;
import com.lovers.repository.WishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class WishService {

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private ActivityService activityService;

    private static final Logger log = LoggerFactory.getLogger(WishService.class);

    public static final List<String> CATEGORIES = List.of("travel", "life", "growth");

    /**
     * 创建愿望
     */
    @Transactional
    public Wish create(Long coupleId, String title, String category,
                       BigDecimal targetAmount, LocalDate targetDate) {
        if (title == null || title.isEmpty()) {
            throw new BusinessException("愿望标题不能为空");
        }

        if (category != null && !CATEGORIES.contains(category)) {
            throw new BusinessException("无效的分类");
        }

        Wish wish = new Wish();
        wish.setCoupleId(coupleId);
        wish.setTitle(title);
        wish.setCategory(category != null ? category : "life");
        wish.setTargetAmount(targetAmount != null ? targetAmount : BigDecimal.ZERO);
        wish.setCurrentAmount(BigDecimal.ZERO);
        wish.setTargetDate(targetDate);
        wish.setStatus(1);
        return wishRepository.save(wish);
    }

    /**
     * 更新进度
     */
    @Transactional
    public Wish updateProgress(Long wishId, BigDecimal currentAmount) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new BusinessException("愿望不存在"));

        if (wish.getStatus() == 2) {
            throw new BusinessException("愿望已达成");
        }

        wish.setCurrentAmount(currentAmount);

        // 自动达成
        if (wish.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                && currentAmount.compareTo(wish.getTargetAmount()) >= 0) {
            wish.setStatus(2);
            // 记录动态
            try {
                activityService.recordActivity(wish.getCoupleId(), "wish",
                        "✨ 愿望达成：" + wish.getTitle(),
                        "目标金额 " + wish.getTargetAmount() + " 已达成 ✓",
                        wish.getId(), "✨");
            } catch (Exception e) {
                log.warn("Failed to record wish activity", e);
            }
        }

        return wishRepository.save(wish);
    }

    /**
     * 标记为已达成
     */
    @Transactional
    public Wish achieve(Long wishId) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new BusinessException("愿望不存在"));

        wish.setStatus(2);
        wish.setCurrentAmount(wish.getTargetAmount());
        Wish saved = wishRepository.save(wish);

        // 记录动态
        try {
            activityService.recordActivity(wish.getCoupleId(), "wish",
                    "✨ 愿望达成：" + wish.getTitle(),
                    "目标金额 " + wish.getTargetAmount() + " 已达成 ✓",
                    wish.getId(), "✨");
        } catch (Exception e) {
            log.warn("Failed to record wish activity", e);
        }

        return saved;
    }

    /**
     * 获取愿望列表
     */
    public List<Wish> listByCouple(Long coupleId) {
        return wishRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId);
    }
}
