package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.TimeCapsule;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.ITimeCapsuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/capsule")
public class TimeCapsuleController {

    @Autowired
    private ITimeCapsuleService timeCapsuleService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建胶囊
     * POST /api/capsule/create
     */
    @PostMapping("/create")
    public Result<TimeCapsule> create(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        String type = (String) request.get("type");
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        Boolean dualMode = request.get("dualMode") != null
                ? Boolean.valueOf(request.get("dualMode").toString()) : false;

        // 解析开启时间
        LocalDateTime openAt;
        String openAtStr = (String) request.get("openAt");
        if (openAtStr != null) {
            openAt = LocalDateTime.parse(openAtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else {
            // 支持传 openDays
            Integer openDays = request.get("openDays") != null
                    ? Integer.valueOf(request.get("openDays").toString()) : 30;
            openAt = LocalDateTime.now().plusDays(openDays);
        }

        // 媒体列表
        @SuppressWarnings("unchecked")
        List<Map<String, String>> mediaList = (List<Map<String, String>>) request.get("mediaList");

        TimeCapsule capsule = timeCapsuleService.create(
                user.getCoupleId(), userId, type, title, content, mediaList, openAt, dualMode);
        return Result.success(capsule);
    }

    /**
     * 双人模式：对方写入
     * POST /api/capsule/write-partner
     */
    @PostMapping("/write-partner")
    public Result<TimeCapsule> writePartner(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        Long pairCapsuleId = Long.valueOf(request.get("pairCapsuleId").toString());
        String content = (String) request.get("content");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> mediaList = (List<Map<String, String>>) request.get("mediaList");

        TimeCapsule capsule = timeCapsuleService.writePartner(
                user.getCoupleId(), userId, pairCapsuleId, content, mediaList);
        return Result.success(capsule);
    }

    /**
     * 获取胶囊列表
     * GET /api/capsule/list?status=&page=0&size=10
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(Map.of("items", List.of(), "page", page, "hasMore", false, "total", 0));
        }

        return Result.success(timeCapsuleService.list(user.getCoupleId(), status, page, size));
    }

    /**
     * 获取胶囊详情
     * GET /api/capsule/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        return Result.success(timeCapsuleService.detail(user.getCoupleId(), id));
    }

    /**
     * 开启胶囊
     * POST /api/capsule/open/{id}
     */
    @PostMapping("/open/{id}")
    public Result<TimeCapsule> open(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        TimeCapsule capsule = timeCapsuleService.open(user.getCoupleId(), id);
        return Result.success(capsule);
    }

    /**
     * 删除胶囊
     * POST /api/capsule/delete/{id}
     */
    @PostMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        timeCapsuleService.delete(user.getCoupleId(), id);
        return Result.success();
    }

    /**
     * 获取胶囊类型列表（前端展示用）
     * GET /api/capsule/types
     */
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> getTypes() {
        List<Map<String, Object>> types = List.of(
                Map.of("value", "to_future_ta", "label", "给未来的TA", "icon", "💌"),
                Map.of("value", "to_future_us", "label", "给未来的我们", "icon", "💑"),
                Map.of("value", "birthday", "label", "生日胶囊", "icon", "🎂"),
                Map.of("value", "anniversary", "label", "纪念日胶囊", "icon", "💍"),
                Map.of("value", "wish", "label", "愿望达成胶囊", "icon", "✨")
        );
        return Result.success(types);
    }

    /**
     * 获取开启时间选项
     * GET /api/capsule/open-options
     */
    @GetMapping("/open-options")
    public Result<List<Map<String, Object>>> getOpenOptions() {
        List<Map<String, Object>> options = List.of(
                Map.of("days", 30, "label", "30天后"),
                Map.of("days", 90, "label", "90天后"),
                Map.of("days", 180, "label", "180天后"),
                Map.of("days", 365, "label", "365天后"),
                Map.of("days", -1, "label", "自定义日期")
        );
        return Result.success(options);
    }

    /**
     * 获取胶囊统计（首页红点用）
     * GET /api/capsule/stats
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(Map.of("openableCount", 0, "draftCount", 0));
        }

        Map<String, Object> capsuleData = timeCapsuleService.list(user.getCoupleId(), 2, 0, 1);
        long openableCount = ((Number) capsuleData.getOrDefault("total", 0)).longValue();
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("openableCount", openableCount);
        stats.put("draftCount", 0);
        return Result.success(stats);
    }
}
