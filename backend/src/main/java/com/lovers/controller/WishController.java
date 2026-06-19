package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.User;
import com.lovers.model.Wish;
import com.lovers.repository.UserRepository;
import com.lovers.service.WishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wish")
public class WishController {

    @Autowired
    private WishService wishService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建愿望
     * POST /api/wish/create
     */
    @PostMapping("/create")
    public Result<Wish> create(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        String title = (String) request.get("title");
        String category = (String) request.get("category");
        BigDecimal targetAmount = request.get("targetAmount") != null
                ? new BigDecimal(request.get("targetAmount").toString()) : null;
        String targetDateStr = (String) request.get("targetDate");
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : null;

        Wish wish = wishService.create(user.getCoupleId(), title, category, targetAmount, targetDate);
        return Result.success(wish);
    }

    /**
     * 更新进度
     * POST /api/wish/progress
     */
    @PostMapping("/progress")
    public Result<Wish> updateProgress(@RequestBody Map<String, Object> request) {
        Long wishId = Long.valueOf(request.get("id").toString());
        BigDecimal currentAmount = new BigDecimal(request.get("currentAmount").toString());
        Wish wish = wishService.updateProgress(wishId, currentAmount);
        return Result.success(wish);
    }

    /**
     * 标记为已达成
     * POST /api/wish/achieve
     */
    @PostMapping("/achieve")
    public Result<Wish> achieve(@RequestBody Map<String, Object> request) {
        Long wishId = Long.valueOf(request.get("id").toString());
        Wish wish = wishService.achieve(wishId);
        return Result.success(wish);
    }

    /**
     * 获取愿望列表
     * GET /api/wish/list
     */
    @GetMapping("/list")
    public Result<List<Wish>> list() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(List.of());
        }

        return Result.success(wishService.listByCouple(user.getCoupleId()));
    }
}
