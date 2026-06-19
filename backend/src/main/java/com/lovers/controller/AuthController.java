package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.User;
import com.lovers.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 微信登录
     * POST /api/auth/login
     * @param request { code: "wx_login_code" }
     * @return { token, userId, isNewUser, hasCouple }
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isEmpty()) {
            return Result.error("登录code不能为空");
        }
        Map<String, Object> result = userService.wxLogin(code);
        return Result.success(result);
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/userinfo
     */
    @GetMapping("/userinfo")
    public Result<User> getUserInfo() {
        Long userId = UserContext.getUserId();
        User user = userService.getUserInfo(userId);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     * POST /api/auth/userinfo
     */
    @PostMapping("/userinfo")
    public Result<User> updateUserInfo(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        String nickname = (String) request.get("nickname");
        Object avatarObj = request.get("avatar");
        String avatar = avatarObj instanceof String ? (String) avatarObj : null;
        Integer gender = (Integer) request.get("gender");
        String phone = (String) request.get("phone");
        User user = userService.updateUserInfo(userId, nickname, avatar, gender, phone);
        return Result.success(user);
    }
}
