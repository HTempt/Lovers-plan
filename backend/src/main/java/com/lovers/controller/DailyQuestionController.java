package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.IDailyQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class DailyQuestionController {

    @Autowired
    private IDailyQuestionService dailyQuestionService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取今日问题
     * GET /api/quiz/today
     */
    @GetMapping("/today")
    public Result<Map<String, Object>> getToday() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");
        return Result.success(dailyQuestionService.getTodayQuestion(user.getCoupleId()));
    }

    /**
     * 提交答案
     * POST /api/quiz/answer
     */
    @PostMapping("/answer")
    public Result<Map<String, Object>> answer(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.error("请先绑定情侣");

        Long dailyQuestionId = Long.valueOf(request.get("dailyQuestionId").toString());
        String answer = (String) request.get("answer");
        return Result.success(dailyQuestionService.submitAnswer(user.getCoupleId(), userId, dailyQuestionId, answer));
    }

    /**
     * 获取今日结果
     * GET /api/quiz/result
     */
    @GetMapping("/result")
    public Result<Map<String, Object>> getResult() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getCoupleId() == null) return Result.success(Map.of("answered", false));
        return Result.success(dailyQuestionService.getResult(user.getCoupleId(), userId));
    }
}
