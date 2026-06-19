package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.service.ICoupleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/couple")
public class CoupleController {

    @Autowired
    private ICoupleService coupleService;

    /**
     * 创建邀请码
     * POST /api/couple/invite
     * @return { code: "123456" }
     */
    @PostMapping("/invite")
    public Result<Map<String, String>> createInvite() {
        Long userId = UserContext.getUserId();
        String code = coupleService.createInviteCode(userId);
        return Result.success(Map.of("code", code));
    }

    /**
     * 接受邀请 - 绑定情侣
     * POST /api/couple/bind
     * @param request { code: "123456" }
     */
    @PostMapping("/bind")
    public Result<Map<String, Object>> acceptInvite(@RequestBody Map<String, String> request) {
        Long userId = UserContext.getUserId();
        String code = request.get("code");
        if (code == null || code.isEmpty()) {
            return Result.error("邀请码不能为空");
        }
        Map<String, Object> result = coupleService.acceptInvite(userId, code);
        return Result.success(result);
    }

    /**
     * 获取情侣信息
     * GET /api/couple/info
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getCoupleInfo() {
        Long userId = UserContext.getUserId();
        Map<String, Object> info = coupleService.getCoupleInfo(userId);
        return Result.success(info);
    }

    /**
     * 解除绑定
     * POST /api/couple/unbind
     */
    @PostMapping("/unbind")
    public Result<Void> unbind() {
        Long userId = UserContext.getUserId();
        coupleService.unbind(userId);
        return Result.success();
    }
}
