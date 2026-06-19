package com.lovers.controller;

import com.lovers.auth.UserContext;
import com.lovers.common.Result;
import com.lovers.model.Todo;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.ITodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todo")
public class TodoController {

    @Autowired
    private ITodoService todoService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建待办
     * POST /api/todo/create
     */
    @PostMapping("/create")
    public Result<Todo> create(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.error("请先绑定情侣");
        }

        String title = (String) request.get("title");
        Long executorId = request.get("executorId") != null ? Long.valueOf(request.get("executorId").toString()) : null;
        String priority = (String) request.get("priority");
        String deadlineStr = (String) request.get("deadline");
        String repeatType = (String) request.get("repeatType");

        LocalDateTime deadline = deadlineStr != null ? LocalDateTime.parse(deadlineStr.replace(" ", "T")) : null;

        Todo todo = todoService.create(userId, user.getCoupleId(), title, executorId, priority, deadline, repeatType);
        return Result.success(todo);
    }

    /**
     * 更新待办
     * POST /api/todo/update
     */
    @PostMapping("/update")
    public Result<Todo> update(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Long id = Long.valueOf(request.get("id").toString());
        String title = (String) request.get("title");
        Long executorId = request.get("executorId") != null ? Long.valueOf(request.get("executorId").toString()) : null;
        String priority = (String) request.get("priority");
        String deadlineStr = (String) request.get("deadline");
        String repeatType = (String) request.get("repeatType");

        LocalDateTime deadline = deadlineStr != null ? LocalDateTime.parse(deadlineStr.replace(" ", "T")) : null;

        Todo todo = todoService.update(id, userId, title, executorId, priority, deadline, repeatType);
        return Result.success(todo);
    }

    /**
     * 完成待办
     * POST /api/todo/complete
     */
    @PostMapping("/complete")
    public Result<Todo> complete(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Long id = Long.valueOf(request.get("id").toString());
        Todo todo = todoService.complete(id, userId);
        return Result.success(todo);
    }

    /**
     * 删除待办
     * POST /api/todo/delete
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Long id = Long.valueOf(request.get("id").toString());
        todoService.delete(id, userId);
        return Result.success();
    }

    /**
     * 获取待办列表
     * GET /api/todo/list
     */
    @GetMapping("/list")
    public Result<List<Todo>> list() {
        Long userId = UserContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getCoupleId() == null) {
            return Result.success(List.of());
        }

        List<Todo> list = todoService.listByCouple(user.getCoupleId());
        return Result.success(list);
    }
}
