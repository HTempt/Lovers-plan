package com.lovers.service;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.Anniversary;
import com.lovers.repository.AnniversaryRepository;
import com.xhinliang.lunarcalendar.LunarCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnniversaryService {

    @Autowired
    private AnniversaryRepository anniversaryRepository;

    @Autowired
    private ActivityService activityService;

    private static final Logger log = LoggerFactory.getLogger(AnniversaryService.class);

    /**
     * 创建自定义纪念日
     */
    @Transactional
    public Anniversary create(Long coupleId, String title, LocalDate anniversaryDate, Integer remindDays, String icon) {
        if (title == null || title.isEmpty()) {
            throw new BusinessException("纪念日标题不能为空");
        }
        if (anniversaryDate == null) {
            throw new BusinessException("纪念日日期不能为空");
        }

        if (anniversaryRepository.existsByCoupleIdAndTitleAndStatus(coupleId, title, 1)) {
            throw new BusinessException("该纪念日名称已存在");
        }

        Anniversary anniversary = new Anniversary();
        anniversary.setCoupleId(coupleId);
        anniversary.setTitle(title);
        anniversary.setAnniversaryDate(anniversaryDate);
        anniversary.setRemindDays(remindDays != null ? remindDays : 0);
        anniversary.setIcon(icon != null ? icon : "❤️");
        anniversary.setType(0); // 自定义
        anniversary.setStatus(1);
        Anniversary saved = anniversaryRepository.save(anniversary);

        // 记录动态
        try {
            activityService.recordActivity(coupleId, "anniversary",
                    "💝 新增纪念日：" + title,
                    "日期：" + anniversaryDate.toString(),
                    saved.getId(), "💝");
        } catch (Exception e) {
            log.warn("Failed to record anniversary activity", e);
        }

        return saved;
    }

    /**
     * 创建系统纪念日（恋爱纪念日）
     */
    @Transactional
    public Anniversary createSystemAnniversary(Long coupleId, LocalDate loveDate) {
        Anniversary anniversary = new Anniversary();
        anniversary.setCoupleId(coupleId);
        anniversary.setTitle("恋爱纪念日");
        anniversary.setAnniversaryDate(loveDate);
        anniversary.setRemindDays(0);
        anniversary.setType(1); // 系统
        anniversary.setStatus(1);
        Anniversary saved = anniversaryRepository.save(anniversary);

        // 记录动态
        try {
            activityService.recordActivity(coupleId, "anniversary",
                    "💝 恋爱纪念日",
                    "始于 " + loveDate.toString(),
                    saved.getId(), "💝");
        } catch (Exception e) {
            log.warn("Failed to record system anniversary activity", e);
        }

        return saved;
    }

    /**
     * 获取最近的纪念日
     */
    public Map<String, Object> getUpcomingAnniversary(Long coupleId) {
        Optional<Anniversary> opt = anniversaryRepository.findUpcomingByCoupleId(coupleId);
        if (opt.isEmpty()) {
            return null;
        }

        Anniversary anniversary = opt.get();
        LocalDate today = LocalDate.now();
        LocalDate thisYearDate = anniversary.getAnniversaryDate()
                .withYear(today.getYear());

        // 如果今年的已过，算到明年
        if (thisYearDate.isBefore(today) || thisYearDate.isEqual(today)) {
            thisYearDate = thisYearDate.plusYears(1);
        }

        long daysLeft = ChronoUnit.DAYS.between(today, thisYearDate);

        Map<String, Object> result = new HashMap<>();
        result.put("id", anniversary.getId());
        result.put("title", anniversary.getTitle());
        result.put("anniversaryDate", anniversary.getAnniversaryDate().toString());
        result.put("daysLeft", (int) daysLeft);
        result.put("type", anniversary.getType());
        result.put("icon", anniversary.getIcon() != null ? anniversary.getIcon() : "❤️");
        return result;
    }

    /**
     * 获取所有纪念日列表
     */
    public List<Map<String, Object>> listByCouple(Long coupleId) {
        List<Anniversary> list = anniversaryRepository
                .findByCoupleIdAndStatusOrderByAnniversaryDateDesc(coupleId, 1);

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Anniversary anniversary : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", anniversary.getId());
            item.put("title", anniversary.getTitle());
            item.put("anniversaryDate", anniversary.getAnniversaryDate().toString());
            item.put("type", anniversary.getType());
            item.put("remindDays", anniversary.getRemindDays());
            item.put("icon", anniversary.getIcon() != null ? anniversary.getIcon() : "❤️");
            item.put("nextDate", anniversary.getAnniversaryDate().toString());

            // 计算农历日期
            try {
                LunarCalendar lunar = LunarCalendar.obtainCalendar(
                        anniversary.getAnniversaryDate().getYear(),
                        anniversary.getAnniversaryDate().getMonthValue(),
                        anniversary.getAnniversaryDate().getDayOfMonth());
                item.put("lunarDate", lunar.getLunarYear() + "年" + lunar.getLunarMonth() + "月" + lunar.getLunarDay());
            } catch (Exception e) {
                item.put("lunarDate", "");
            }

            // 计算天数
            LocalDate nextDate = anniversary.getAnniversaryDate().withYear(today.getYear());
            if (nextDate.isBefore(today) || nextDate.isEqual(today)) {
                nextDate = nextDate.plusYears(1);
            }
            long daysLeft = ChronoUnit.DAYS.between(today, nextDate);
            long totalDays = ChronoUnit.DAYS.between(anniversary.getAnniversaryDate(), today);
            item.put("daysLeft", (int) daysLeft);
            item.put("totalDays", (int) Math.abs(totalDays));

            result.add(item);
        }

        return result;
    }

    /**
     * 删除纪念日
     */
    @Transactional
    public void delete(Long anniversaryId) {
        Anniversary anniversary = anniversaryRepository.findById(anniversaryId)
                .orElseThrow(() -> new BusinessException("纪念日不存在"));

        anniversary.setStatus(0);
        anniversaryRepository.save(anniversary);
    }

    /**
     * 更新自定义纪念日
     */
    @Transactional
    public Anniversary update(Long id, String title, LocalDate anniversaryDate, Integer remindDays, String icon) {
        Anniversary anniversary = anniversaryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("纪念日不存在"));

        if (title != null && !title.isEmpty()) {
            if (anniversaryRepository.existsByCoupleIdAndTitleAndStatusAndIdNot(
                    anniversary.getCoupleId(), title, 1, id)) {
                throw new BusinessException("该纪念日名称已存在");
            }
            anniversary.setTitle(title);
        }
        if (anniversaryDate != null) {
            anniversary.setAnniversaryDate(anniversaryDate);
        }
        if (remindDays != null) {
            anniversary.setRemindDays(remindDays);
        }
        if (icon != null) {
            anniversary.setIcon(icon);
        }
        return anniversaryRepository.save(anniversary);
    }
}
