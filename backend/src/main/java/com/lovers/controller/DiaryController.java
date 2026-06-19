package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.Diary;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.DiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建日记
     * POST /api/diary/create
     */
    @PostMapping("/create")
    public Result<Diary> create(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        String title = (String) request.get("title");
        String content = (String) request.get("content");
        String location = (String) request.get("location");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> mediaList = (List<Map<String, String>>) request.get("mediaList");

        Diary diary = diaryService.create(userId, user.getCoupleId(), title, content, location, mediaList);
        return Result.success(diary);
    }

    /**
     * 获取时间轴
     * GET /api/diary/timeline?page=0&size=20
     */
    @GetMapping("/timeline")
    public Result<Map<String, Object>> getTimeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(Map.of("items", List.of(), "page", 0, "hasMore", false, "total", 0));
        }

        Map<String, Object> timeline = diaryService.getTimeline(user.getCoupleId(), page, size);
        return Result.success(timeline);
    }

    /**
     * 获取相册
     * GET /api/diary/album
     */
    @GetMapping("/album")
    public Result<Map<String, List<Map<String, Object>>>> getAlbum() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(Map.of());
        }

        return Result.success(diaryService.getAlbum(user.getCoupleId()));
    }

    /**
     * 获取地图足迹
     * GET /api/diary/map
     */
    @GetMapping("/map")
    public Result<List<Map<String, Object>>> getMap() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(List.of());
        }

        return Result.success(diaryService.getMapLocations(user.getCoupleId()));
    }

    /**
     * 删除日记
     * POST /api/diary/delete/{id}
     */
    @PostMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        diaryService.delete(userId, id);
        return Result.success();
    }

    /**
     * 恢复日记
     * POST /api/diary/restore/{id}
     */
    @PostMapping("/restore/{id}")
    public Result<Void> restore(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        diaryService.restore(userId, id);
        return Result.success();
    }

    /**
     * 获取回收站
     * GET /api/diary/recycle
     */
    @GetMapping("/recycle")
    public Result<List<Diary>> getRecycleBin() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(List.of());
        }

        return Result.success(diaryService.getRecycleBin(user.getCoupleId()));
    }
}
