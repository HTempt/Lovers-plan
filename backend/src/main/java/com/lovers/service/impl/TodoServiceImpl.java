package com.lovers.service.impl;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.Todo;
import com.lovers.model.User;
import com.lovers.repository.TodoRepository;
import com.lovers.repository.UserRepository;
import com.lovers.service.ITodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TodoServiceImpl implements ITodoService {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建待办
     */
    @Transactional
    public Todo create(Long userId, Long coupleId, String title, Long executorId,
                       String priority, LocalDateTime deadline, String repeatType) {
        if (title == null || title.isEmpty()) {
            throw new BusinessException("待办标题不能为空");
        }

        Todo todo = new Todo();
        todo.setCoupleId(coupleId);
        todo.setCreatorId(userId);
        todo.setTitle(title);
        todo.setExecutorId(executorId);
        todo.setPriority(priority != null ? priority : "mid");
        todo.setDeadline(deadline);
        todo.setRepeatType(repeatType != null ? repeatType : "");
        todo.setStatus(0);
        return todoRepository.save(todo);
    }

    /**
     * 更新待办
     */
    @Transactional
    public Todo update(Long todoId, Long userId, String title, Long executorId,
                       String priority, LocalDateTime deadline, String repeatType) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException("待办不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!todo.getCoupleId().equals(user.getCoupleId())) {
            throw new BusinessException("无权操作此待办");
        }

        if (title != null) todo.setTitle(title);
        if (executorId != null) todo.setExecutorId(executorId);
        if (priority != null) todo.setPriority(priority);
        if (deadline != null) todo.setDeadline(deadline);
        if (repeatType != null) todo.setRepeatType(repeatType);

        return todoRepository.save(todo);
    }

    /**
     * 完成待办（若设置了重复规则，自动生成下一期）
     */
    @Transactional
    public Todo complete(Long todoId, Long userId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException("待办不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!todo.getCoupleId().equals(user.getCoupleId())) {
            throw new BusinessException("无权操作此待办");
        }

        todo.setStatus(1);
        todoRepository.save(todo);

        // 自动重复逻辑
        if (todo.getRepeatType() != null && !todo.getRepeatType().isEmpty()) {
            Todo next = new Todo();
            next.setCoupleId(todo.getCoupleId());
            next.setCreatorId(todo.getCreatorId());
            next.setExecutorId(todo.getExecutorId());
            next.setTitle(todo.getTitle());
            next.setPriority(todo.getPriority());
            next.setRepeatType(todo.getRepeatType());
            next.setStatus(0);

            // 计算新的截止时间
            if (todo.getDeadline() != null) {
                switch (todo.getRepeatType()) {
                    case "daily":
                        next.setDeadline(todo.getDeadline().plusDays(1));
                        break;
                    case "weekly":
                        next.setDeadline(todo.getDeadline().plusWeeks(1));
                        break;
                    case "monthly":
                        next.setDeadline(todo.getDeadline().plusMonths(1));
                        break;
                    default:
                        next.setDeadline(todo.getDeadline());
                }
            }

            todoRepository.save(next);
        }

        return todo;
    }

    /**
     * 删除待办（标记已过期）
     */
    @Transactional
    public void delete(Long todoId, Long userId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException("待办不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!todo.getCoupleId().equals(user.getCoupleId())) {
            throw new BusinessException("无权操作此待办");
        }

        todo.setStatus(2);
        todoRepository.save(todo);
    }

    /**
     * 获取情侣的待办列表
     */
    public List<Todo> listByCouple(Long coupleId) {
        return todoRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId);
    }

    /**
     * 获取待办数量（待完成状态）
     */
    public long getTodoCount(Long coupleId) {
        return todoRepository.countByCoupleIdAndStatus(coupleId, 0);
    }
}
