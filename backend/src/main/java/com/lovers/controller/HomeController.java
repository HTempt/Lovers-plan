package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.service.IHomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private IHomeService homeService;

    /**
     * 获取首页数据
     * GET /api/home/data
     * @return { hasCouple, loveDays, loveDate, myId, partner, myStatus, partnerStatus, todoCount, taskCount, upcomingAnniversary }
     */
    @GetMapping("/data")
    public Result<Map<String, Object>> getHomeData() {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = homeService.getHomeData(userId);
        return Result.success(data);
    }
}
