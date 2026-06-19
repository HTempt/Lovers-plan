package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取情侣动态列表
     * GET /api/activity/list?page=0&size=10
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getActivityFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getCoupleId() == null) {
            return Result.success(Map.of("items", java.util.List.of(), "page", page, "hasMore", false, "total", 0));
        }
        Map<String, Object> feed = activityService.getActivityFeed(user.getCoupleId(), page, size);
        return Result.success(feed);
    }
}
