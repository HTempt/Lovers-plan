package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.IAnniversaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/anniversary")
public class AnniversaryController {

    @Autowired
    private IAnniversaryService anniversaryService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建自定义纪念日
     * POST /api/anniversary/create
     */
    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        String title = (String) request.get("title");
        String dateStr = (String) request.get("anniversaryDate");
        Integer remindDays = request.get("remindDays") != null
                ? Integer.valueOf(request.get("remindDays").toString()) : null;
        String icon = (String) request.get("icon");

        if (dateStr == null) {
            return Result.error("纪念日日期不能为空");
        }

        LocalDate anniversaryDate = LocalDate.parse(dateStr);
        var anniversary = anniversaryService.create(user.getCoupleId(), title, anniversaryDate, remindDays, icon);
        return Result.success(anniversary);
    }

    /**
     * 获取所有纪念日
     * GET /api/anniversary/list
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(List.of());
        }

        return Result.success(anniversaryService.listByCouple(user.getCoupleId()));
    }

    /**
     * 获取最近纪念日
     * GET /api/anniversary/upcoming
     */
    @GetMapping("/upcoming")
    public Result<Map<String, Object>> getUpcoming() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(null);
        }

        return Result.success(anniversaryService.getUpcomingAnniversary(user.getCoupleId()));
    }

    /**
     * 删除纪念日
     * POST /api/anniversary/delete/{id}
     */
    @PostMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        anniversaryService.delete(id);
        return Result.success();
    }

    /**
     * 更新自定义纪念日
     * POST /api/anniversary/update
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody Map<String, Object> request) {
        Long id = Long.valueOf(request.get("id").toString());
        String title = (String) request.get("title");
        String dateStr = (String) request.get("anniversaryDate");
        Integer remindDays = request.get("remindDays") != null
                ? Integer.valueOf(request.get("remindDays").toString()) : null;
        String icon = (String) request.get("icon");
        LocalDate anniversaryDate = dateStr != null ? LocalDate.parse(dateStr) : null;

        var anniversary = anniversaryService.update(id, title, anniversaryDate, remindDays, icon);
        return Result.success(anniversary);
    }
}
