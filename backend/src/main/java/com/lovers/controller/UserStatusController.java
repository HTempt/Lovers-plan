package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.UserStatus;
import com.lovers.service.IUserStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class UserStatusController {

    @Autowired
    private IUserStatusService userStatusService;

    /**
     * 设置状态
     * POST /api/status/set
     */
    @PostMapping("/set")
    public Result<UserStatus> setStatus(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        String statusName = (String) request.get("statusName");
        String mood = (String) request.get("mood");
        Integer durationMinutes = (Integer) request.get("durationMinutes");

        if (statusName == null || statusName.isEmpty()) {
            return Result.error("状态名称不能为空");
        }

        UserStatus status = userStatusService.setStatus(userId, statusName, mood, durationMinutes);
        return Result.success(status);
    }

    /**
     * 获取当前状态
     * GET /api/status/current
     */
    @GetMapping("/current")
    public Result<UserStatus> getCurrentStatus() {
        Long userId = UserContext.getUserId();
        UserStatus status = userStatusService.getCurrentStatus(userId);
        return Result.success(status);
    }

    /**
     * 获取对方状态
     * GET /api/status/partner
     */
    @GetMapping("/partner")
    public Result<UserStatus> getPartnerStatus() {
        Long userId = UserContext.getUserId();
        UserStatus status = userStatusService.getPartnerStatus(userId);
        return Result.success(status);
    }

    /**
     * 清空当前状态
     * POST /api/status/clear
     */
    @PostMapping("/clear")
    public Result<Object> clearStatus() {
        Long userId = UserContext.getUserId();
        userStatusService.clearStatus(userId);
        return Result.success();
    }

    /**
     * 获取状态模板
     * GET /api/status/templates
     */
    @GetMapping("/templates")
    public Result<Map<String, Object>> getTemplates() {
        return Result.success(Map.of(
                "statusTemplates", IUserStatusService.STATUS_TEMPLATES,
                "moodTags", IUserStatusService.MOOD_TAGS
        ));
    }
}
