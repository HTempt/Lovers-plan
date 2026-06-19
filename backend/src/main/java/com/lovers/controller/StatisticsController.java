package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取统计概览
     * GET /api/statistics/overview
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Long userId = UserContext.getUserId();
        Map<String, Object> overview = statisticsService.getOverview(userId);
        return Result.success(overview);
    }
}
