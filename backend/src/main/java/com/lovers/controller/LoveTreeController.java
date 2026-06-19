package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.LoveTree;
import com.lovers.model.LoveTreeGrowthRecord;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.ILoveTreeService;
import com.lovers.service.impl.LoveTreeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/love-tree")
public class LoveTreeController {

    @Autowired
    private ILoveTreeService loveTreeService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取爱情树信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getTreeInfo() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }
        return Result.success(loveTreeService.getTreeInfo(user.getCoupleId()));
    }

    /**
     * 获取成长记录（分页）
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> getGrowthHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }
        return Result.success(loveTreeService.getGrowthHistory(user.getCoupleId(), page, size));
    }

    /**
     * 获取所有等级信息
     */
    @GetMapping("/levels")
    public Result<List<Map<String, Object>>> getAllLevels() {
        return Result.success(LoveTreeServiceImpl.getAllLevels());
    }

    /**
     * 获取解锁奖励信息
     */
    @GetMapping("/rewards")
    public Result<Map<Integer, String>> getRewards() {
        return Result.success(LoveTreeServiceImpl.getUnlockRewards());
    }
}
