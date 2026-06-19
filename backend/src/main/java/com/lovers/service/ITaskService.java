package com.lovers.service;

import com.lovers.model.Task;
import com.lovers.model.TaskRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ITaskService {
    Task create(Long coupleId, String title, Integer targetCount, LocalDate deadline);
    TaskRecord checkIn(Long userId, Long taskId);
    Map<String, Object> confirm(Long userId, Long recordId);
    List<Map<String, Object>> listByCouple(Long coupleId);
    long getTaskCount(Long coupleId);
}
