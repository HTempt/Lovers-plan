package com.lovers.service;

import com.lovers.model.Todo;
import java.time.LocalDateTime;
import java.util.List;

public interface ITodoService {
    Todo create(Long userId, Long coupleId, String title, Long executorId, String priority, LocalDateTime deadline, String repeatType);
    Todo update(Long todoId, Long userId, String title, Long executorId, String priority, LocalDateTime deadline, String repeatType);
    Todo complete(Long todoId, Long userId);
    void delete(Long todoId, Long userId);
    List<Todo> listByCouple(Long coupleId);
    long getTodoCount(Long coupleId);
}
