package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.Footprint;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.IFootprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/footprint")
public class FootprintController {

    @Autowired
    private IFootprintService footprintService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取足迹列表
     */
    @GetMapping("/list")
    public Result<List<Footprint>> getFootprints() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");
        return Result.success(footprintService.getFootprints(user.getCoupleId()));
    }

    /**
     * 获取足迹统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");
        return Result.success(footprintService.getStats(user.getCoupleId()));
    }

    /**
     * 获取城市排行榜
     */
    @GetMapping("/city-ranking")
    public Result<List<Map<String, Object>>> getCityRanking() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");
        return Result.success(footprintService.getCityRanking(user.getCoupleId()));
    }
}
