package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.IAchievementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/achievement")
public class AchievementController {

    @Autowired
    private IAchievementService achievementService;

    @Autowired
    private UserRepository userRepository;

    /** 获取成就概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");
        return Result.success(achievementService.getOverview(user.getCoupleId()));
    }

    /** 获取全部分类成就列表 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getList(@RequestParam(required = false) String category) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");
        if (category != null && !category.isEmpty()) {
            return Result.success(achievementService.getAchievementsByCategory(user.getCoupleId(), category));
        }
        return Result.success(achievementService.getAllAchievements(user.getCoupleId()));
    }

    /** 获取最近解锁的成就 */
    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> getRecent(@RequestParam(defaultValue = "5") int limit) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");
        return Result.success(achievementService.getRecentUnlocks(user.getCoupleId(), limit));
    }

    /** 主动检查并尝试解锁某分类成就 */
    @PostMapping("/check")
    public Result<List<Map<String, Object>>> checkAndUnlock(@RequestBody Map<String, String> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");

        String category = request.get("category");
        String code = request.get("code");
        List<Map<String, Object>> unlocked = achievementService.checkAndUnlock(
                user.getCoupleId(), category, code, userId);
        return Result.success(unlocked);
    }
}
