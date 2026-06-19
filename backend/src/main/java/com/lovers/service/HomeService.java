package com.lovers.service;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.Anniversary;
import com.lovers.model.Couple;
import com.lovers.model.Task;
import com.lovers.model.Todo;
import com.lovers.model.User;
import com.lovers.model.Wish;
import com.lovers.repository.*;
import com.lovers.service.DiaryService;
import com.lovers.service.WishService;
import com.xhinliang.lunarcalendar.LunarCalendar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HomeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private UserStatusRepository userStatusRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AnniversaryService anniversaryService;

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private WishService wishService;

    @Autowired
    private AnniversaryRepository anniversaryRepository;

    @Autowired
    private ActivityService activityService;

    /**
     * 获取首页聚合数据
     */
    public Map<String, Object> getHomeData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        Map<String, Object> data = new HashMap<>();

        if (user.getCoupleId() == null) {
            data.put("hasCouple", false);
            return data;
        }

        Couple couple = coupleRepository.findByIdAndStatus(user.getCoupleId(), 1)
                .orElseThrow(() -> new BusinessException("情侣关系不存在或已解除"));

        data.put("hasCouple", true);
        data.put("coupleId", couple.getId());

        // 恋爱天数
        if (couple.getLoveDate() != null) {
            long loveDays = ChronoUnit.DAYS.between(couple.getLoveDate(), LocalDate.now());
            data.put("loveDays", loveDays);
            data.put("loveDate", couple.getLoveDate().toString());
            // 添加恋爱起始日对应的农历日期
            try {
                LunarCalendar lunar = LunarCalendar.obtainCalendar(
                        couple.getLoveDate().getYear(),
                        couple.getLoveDate().getMonthValue(),
                        couple.getLoveDate().getDayOfMonth());
                String lunarStr = lunar.getLunarYear() + "年" + lunar.getLunarMonth() + "月" + lunar.getLunarDay();
                data.put("loveDateLunar", lunarStr);
            } catch (Exception e) {
                data.put("loveDateLunar", "");
            }
        }

        // 我的状态
        data.put("myStatus", userStatusRepository.findByUserIdAndStatus(userId, 1).orElse(null));

        // 当前用户信息（头像等）
        data.put("myUserInfo", Map.of(
                "id", user.getId(),
                "nickname", user.getNickname(),
                "avatar", user.getAvatar(),
                "gender", user.getGender() != null ? user.getGender() : 0
        ));

        // 对方状态
        User partner = null;
        Long partnerId = null;
        if (couple.getUserA() != null && couple.getUserA().equals(userId) && couple.getUserB() != null) {
            partner = userRepository.findById(couple.getUserB()).orElse(null);
            partnerId = couple.getUserB();
        } else if (couple.getUserB() != null && couple.getUserB().equals(userId) && couple.getUserA() != null) {
            partner = userRepository.findById(couple.getUserA()).orElse(null);
            partnerId = couple.getUserA();
        }
        data.put("partner", partner != null ? Map.of(
                "id", partner.getId(),
                "nickname", partner.getNickname(),
                "avatar", partner.getAvatar(),
                "gender", partner.getGender() != null ? partner.getGender() : 0
        ) : null);
        data.put("partnerStatus", partnerId != null ? userStatusRepository.findByUserIdAndStatus(partnerId, 1).orElse(null) : null);

        // 待办数量
        data.put("todoCount", todoRepository.countByCoupleIdAndStatus(couple.getId(), 0));

        // 任务数量 & 纪念日
        data.put("taskCount", taskRepository.countByCoupleIdAndStatus(couple.getId(), 1));
        data.put("upcomingAnniversary", anniversaryService.getUpcomingAnniversary(couple.getId()));

        // 判断是否结婚（通过检查是否有"结婚"纪念日）
        boolean isMarried = false;
        LocalDate marriedDate = null;
        try {
            List<Anniversary> allAnniversaries = anniversaryRepository.findByCoupleIdAndStatusOrderByAnniversaryDateDesc(couple.getId(), 1);
            for (Anniversary a : allAnniversaries) {
                if ("结婚纪念日".equals(a.getTitle())) {
                    isMarried = true;
                    marriedDate = a.getAnniversaryDate();
                    break;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        data.put("isMarried", isMarried);
        if (isMarried && marriedDate != null) {
            long marriedDays = ChronoUnit.DAYS.between(marriedDate, LocalDate.now());
            data.put("marriedDays", (int) Math.abs(marriedDays));
            data.put("marriedDate", marriedDate.toString());
        }

        // 最近回忆（最近3条日记含媒体）
        try {
            Map<String, Object> timeline = diaryService.getTimeline(couple.getId(), 0, 3);
            data.put("recentDiaries", timeline.get("items"));
        } catch (Exception e) {
            data.put("recentDiaries", List.of());
        }

        // 进行中任务（最多3条）
        try {
            List<Map<String, Object>> tasks = new java.util.ArrayList<>();
            List<Task> taskList = taskRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(couple.getId(), 1);
            if (taskList != null) {
                for (var t : taskList) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("title", t.getTitle());
                    m.put("targetCount", t.getTargetCount());
                    m.put("currentCount", t.getCurrentCount());
                    tasks.add(m);
                }
            }
            data.put("taskList", tasks);
        } catch (Exception e) {
            data.put("taskList", List.of());
        }

        // 未完成待办（最多3条）
        try {
            List<Map<String, Object>> todos = new java.util.ArrayList<>();
            List<Todo> todoList = todoRepository.findByCoupleIdAndStatusOrderByCreateTimeDesc(couple.getId(), 0);
            if (todoList != null) {
                for (var t : todoList) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("title", t.getTitle());
                    m.put("deadline", t.getDeadline() != null ? t.getDeadline().toLocalDate().toString() : null);
                    todos.add(m);
                }
            }
            data.put("todoList", todos);
        } catch (Exception e) {
            data.put("todoList", List.of());
        }

        // 愿望进度列表
        try {
            List<Wish> wishes = wishService.listByCouple(couple.getId());
            List<Map<String, Object>> wishData = wishes.stream().map(w -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", w.getId());
                m.put("title", w.getTitle());
                m.put("category", w.getCategory());
                m.put("targetAmount", w.getTargetAmount());
                m.put("currentAmount", w.getCurrentAmount());
                m.put("targetDate", w.getTargetDate() != null ? w.getTargetDate().toString() : null);
                m.put("status", w.getStatus());
                // 计算进度百分比
                if (w.getTargetAmount() != null && w.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                        && w.getCurrentAmount() != null) {
                    double progress = w.getCurrentAmount().doubleValue() / w.getTargetAmount().doubleValue() * 100;
                    m.put("progress", Math.min(100, Math.round(progress * 10.0) / 10.0));
                } else {
                    m.put("progress", 0);
                }
                return m;
            }).collect(Collectors.toList());
            data.put("wishList", wishData);
        } catch (Exception e) {
            data.put("wishList", List.of());
        }

        // 岛屿动态（默认加载10条）
        try {
            // 首次加载时，如果还没有动态则从历史数据回填
            activityService.backfillIfEmpty(couple.getId());
            data.put("activityFeed", activityService.getActivityFeed(couple.getId(), 0, 10));
        } catch (Exception e) {
            data.put("activityFeed", Map.of("items", List.of(), "page", 0, "hasMore", false, "total", 0));
        }

        return data;
    }
}
