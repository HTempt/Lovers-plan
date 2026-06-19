package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.service.ISignInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sign-in")
public class SignInController {

    @Autowired
    private ISignInService signInService;

    /**
     * 每日签到
     * POST /api/sign-in/do
     */
    @PostMapping("/do")
    public Result<Map<String, Object>> signIn() {
        Long userId = UserContext.getUserId();
        return Result.success(signInService.signIn(userId));
    }

    /**
     * 获取签到状态
     * GET /api/sign-in/status
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getSignInStatus() {
        Long userId = UserContext.getUserId();
        return Result.success(signInService.getSignInStatus(userId));
    }
}
