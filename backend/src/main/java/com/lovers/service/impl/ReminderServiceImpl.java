package com.lovers.service.impl;

import com.lovers.service.IReminderService;
import com.lovers.service.IWechatSubscribeService;
import com.lovers.model.*;
import com.lovers.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderServiceImpl implements IReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderServiceImpl.class);

    @Autowired
    private AnniversaryRepository anniversaryRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private IWechatSubscribeService wechatSubscribeService;

    /**
     * 每天凌晨 8:00 检查纪念日提醒
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkAnniversaryReminders() {
        log.info("Checking anniversary reminders...");
        List<Couple> couples = coupleRepository.findAll();

        for (Couple couple : couples) {
            if (couple.getStatus() != 1) continue;

            List<Anniversary> anniversaries = anniversaryRepository
                    .findByCoupleIdAndStatusOrderByAnniversaryDateDesc(couple.getId(), 1);

            for (Anniversary anniversary : anniversaries) {
                int remindDays = anniversary.getRemindDays() != null ? anniversary.getRemindDays() : 0;
                if (remindDays <= 0) continue;

                LocalDate today = LocalDate.now();
                LocalDate nextDate = anniversary.getAnniversaryDate().withYear(today.getYear());
                if (nextDate.isBefore(today)) {
                    nextDate = nextDate.plusYears(1);
                }

                long daysUntil = ChronoUnit.DAYS.between(today, nextDate);

                // 在 remindDays 天前提醒
                if (daysUntil == remindDays) {
                    // 提醒双方
                    sendAnniversaryReminderToBoth(couple, anniversary, (int) daysUntil);
                }
            }
        }
    }

    /**
     * 每小时检查待办到期提醒
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkTodoDeadlineReminders() {
        log.info("Checking todo deadline reminders...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inOneHour = now.plusHours(1);

        List<Couple> couples = coupleRepository.findAll();
        for (Couple couple : couples) {
            if (couple.getStatus() != 1) continue;

            List<Todo> todos = todoRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(
                    couple.getId(), 0);

            for (Todo todo : todos) {
                if (todo.getDeadline() == null) continue;

                // 截止时间在1小时内
                if (todo.getDeadline().isAfter(now) && todo.getDeadline().isBefore(inOneHour)) {
                    // 通知执行人
                    if (todo.getExecutorId() != null) {
                        userRepository.findById(todo.getExecutorId()).ifPresent(user -> {
                            if (user.getOpenid() != null) {
                                wechatSubscribeService.sendTodoReminder(
                                        user.getOpenid(), todo.getTitle(),
                                        todo.getDeadline().toString());
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * 每天凌晨 9:00 检查任务待确认提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkTaskPendingConfirmation() {
        log.info("Checking pending task confirmations...");
        List<Couple> couples = coupleRepository.findAll();
        for (Couple couple : couples) {
            if (couple.getStatus() != 1) continue;

            List<Task> tasks = taskRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(
                    couple.getId(), 1);

            for (Task task : tasks) {
                List<TaskRecord> records = taskRecordRepository.findByTaskId(task.getId());
                for (TaskRecord record : records) {
                    if (record.getStatus() == 0) {
                        // 需要另一方确认
                        Long confirmUserId = getPartnerId(couple, record.getUserId());
                        if (confirmUserId != null) {
                            userRepository.findById(confirmUserId).ifPresent(user -> {
                                if (user.getOpenid() != null) {
                                    wechatSubscribeService.sendTaskConfirmReminder(
                                            user.getOpenid(), task.getTitle(), "对方");
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    /**
     * 每天凌晨 10:00 检查愿望截止提醒
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void checkWishDeadlineReminders() {
        log.info("Checking wish deadline reminders...");
        LocalDate today = LocalDate.now();
        LocalDate inWeek = today.plusDays(7);

        List<Couple> couples = coupleRepository.findAll();
        for (Couple couple : couples) {
            if (couple.getStatus() != 1) continue;

            List<Wish> wishes = wishRepository.findByCoupleIdOrderByCreateTimeDesc(couple.getId());
            for (Wish wish : wishes) {
                if (wish.getStatus() == 2) continue;
                if (wish.getTargetDate() == null) continue;

                // 截止日期在一周内
                if (wish.getTargetDate().isAfter(today) && wish.getTargetDate().isBefore(inWeek)) {
                    sendWishReminderToBoth(couple, wish);
                }
            }
        }
    }

    private void sendAnniversaryReminderToBoth(Couple couple, Anniversary anniversary, int daysLeft) {
        Long[] userIds = {couple.getUserA(), couple.getUserB()};
        for (Long userId : userIds) {
            if (userId != null) {
                userRepository.findById(userId).ifPresent(user -> {
                    if (user.getOpenid() != null) {
                        wechatSubscribeService.sendAnniversaryReminder(
                                user.getOpenid(), anniversary.getTitle(),
                                daysLeft, anniversary.getAnniversaryDate().toString());
                    }
                });
            }
        }
    }

    private void sendWishReminderToBoth(Couple couple, Wish wish) {
        Long[] userIds = {couple.getUserA(), couple.getUserB()};
        for (Long userId : userIds) {
            if (userId != null) {
                userRepository.findById(userId).ifPresent(user -> {
                    if (user.getOpenid() != null) {
                        wechatSubscribeService.sendWishDeadlineReminder(
                                user.getOpenid(), wish.getTitle(),
                                wish.getTargetDate().toString());
                    }
                });
            }
        }
    }

    private Long getPartnerId(Couple couple, Long userId) {
        if (couple.getUserA().equals(userId)) return couple.getUserB();
        if (couple.getUserB() != null && couple.getUserB().equals(userId)) return couple.getUserA();
        return null;
    }
}
