package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.Task;
import com.lovers.model.TaskRecord;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.model.Badge;
import com.lovers.service.IBadgeService;
import com.lovers.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private ITaskService taskService;

    @Autowired
    private IBadgeService badgeService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建任务
     * POST /api/task/create
     */
    @PostMapping("/create")
    public Result<Task> create(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        String title = (String) request.get("title");
        Integer targetCount = request.get("targetCount") != null ? Integer.valueOf(request.get("targetCount").toString()) : null;
        String deadlineStr = (String) request.get("deadline");
        LocalDate deadline = deadlineStr != null ? LocalDate.parse(deadlineStr) : null;

        Task task = taskService.create(user.getCoupleId(), title, targetCount, deadline);
        return Result.success(task);
    }

    /**
     * 打卡
     * POST /api/task/check-in
     */
    @PostMapping("/check-in")
    public Result<TaskRecord> checkIn(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Long taskId = Long.valueOf(request.get("taskId").toString());
        TaskRecord record = taskService.checkIn(userId, taskId);
        return Result.success(record);
    }

    /**
     * 确认打卡
     * POST /api/task/confirm
     */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Long recordId = Long.valueOf(request.get("recordId").toString());
        Map<String, Object> result = taskService.confirm(userId, recordId);
        return Result.success(result);
    }

    /**
     * 获取任务列表
     * GET /api/task/list
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(List.of());
        }

        return Result.success(taskService.listByCouple(user.getCoupleId()));
    }

    /**
     * 获取已获得徽章
     * GET /api/task/badges
     */
    @GetMapping("/badges")
    public Result<List<Badge>> getBadges() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(List.of());
        }

        return Result.success(badgeService.getBadges(user.getCoupleId()));
    }
}
