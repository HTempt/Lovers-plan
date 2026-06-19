package com.lovers.service;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.Couple;
import com.lovers.model.Task;
import com.lovers.model.TaskRecord;
import com.lovers.model.User;
import com.lovers.repository.CoupleRepository;
import com.lovers.repository.TaskRecordRepository;
import com.lovers.repository.TaskRepository;
import com.lovers.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private BadgeService badgeService;

    @Autowired
    private ActivityService activityService;

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    /**
     * 创建任务
     */
    @Transactional
    public Task create(Long coupleId, String title, Integer targetCount, LocalDate deadline) {
        if (title == null || title.isEmpty()) {
            throw new BusinessException("任务标题不能为空");
        }

        Task task = new Task();
        task.setCoupleId(coupleId);
        task.setTitle(title);
        task.setTargetCount(targetCount != null ? targetCount : 1);
        task.setCurrentCount(0);
        task.setDeadline(deadline);
        task.setStatus(1);
        return taskRepository.save(task);
    }

    /**
     * 打卡
     */
    @Transactional
    public TaskRecord checkIn(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));

        if (task.getStatus() != 1) {
            throw new BusinessException("任务已结束");
        }

        // 检查是否有待确认的打卡
        if (taskRecordRepository.existsByTaskIdAndUserIdAndStatus(taskId, userId, 0)) {
            throw new BusinessException("已有待确认的打卡记录");
        }

        TaskRecord record = new TaskRecord();
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setStatus(0);
        return taskRecordRepository.save(record);
    }

    /**
     * 确认打卡（由另一半确认）
     */
    @Transactional
    public Map<String, Object> confirm(Long userId, Long recordId) {
        TaskRecord record = taskRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("打卡记录不存在"));

        Task task = taskRepository.findById(record.getTaskId())
                .orElseThrow(() -> new BusinessException("任务不存在"));

        // 不能确认自己的打卡
        if (record.getUserId().equals(userId)) {
            throw new BusinessException("不能确认自己的打卡");
        }

        // 确认用户必须在同一情侣关系中
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!task.getCoupleId().equals(user.getCoupleId())) {
            throw new BusinessException("无权确认此打卡");
        }

        record.setStatus(1);
        record.setConfirmUserId(userId);
        taskRecordRepository.save(record);

        // 增加完成次数
        task.setCurrentCount(task.getCurrentCount() + 1);

        // 检查是否达成目标
        if (task.getCurrentCount() >= task.getTargetCount()) {
            task.setStatus(2); // 已完成

            // 记录动态
            try {
                activityService.recordActivity(task.getCoupleId(), "task",
                        "🎯 完成任务：" + task.getTitle(),
                        "累计完成 " + task.getTargetCount() + " 次 ✓",
                        task.getId(), "🎯");
            } catch (Exception e) {
                log.warn("Failed to record task activity", e);
            }
        }

        taskRepository.save(task);

        // 检查成就徽章
        badgeService.checkAndAward(task.getCoupleId(), task.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("task", task);
        return result;
    }

    /**
     * 获取情侣的任务列表（含打卡记录）
     */
    public List<Map<String, Object>> listByCouple(Long coupleId) {
        List<Task> tasks = taskRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId);

        return tasks.stream().map(task -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("targetCount", task.getTargetCount());
            item.put("currentCount", task.getCurrentCount());
            item.put("deadline", task.getDeadline());
            item.put("status", task.getStatus());
            item.put("createTime", task.getCreateTime());

            // 加载打卡记录
            List<TaskRecord> records = taskRecordRepository.findByTaskId(task.getId());
            item.put("records", records);

            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 获取进行中任务数量
     */
    public long getTaskCount(Long coupleId) {
        return taskRepository.countByCoupleIdAndStatus(coupleId, 1);
    }
}
