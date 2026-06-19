package com.lovers.service.impl;

import com.lovers.service.ILoveTreeService;
import com.lovers.common.exception.BusinessException;
import com.lovers.model.SignInRecord;
import com.lovers.model.User;
import com.lovers.repository.SignInRecordRepository;
import com.lovers.repository.UserRepository;
import com.lovers.service.ISignInService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SignInServiceImpl implements ISignInService {

    private static final Logger log = LoggerFactory.getLogger(SignInServiceImpl.class);

    @Autowired
    private SignInRecordRepository signInRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ILoveTreeService loveTreeService;

    /**
     * 每日签到
     */
    @Transactional
    public Map<String, Object> signIn(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getCoupleId() == null) {
            throw new BusinessException("请先绑定情侣");
        }

        LocalDate today = LocalDate.now();

        // 检查是否已签到
        if (signInRecordRepository.existsByUserIdAndSignDate(userId, today)) {
            throw new BusinessException("今天已经签到过了");
        }

        // 创建签到记录
        SignInRecord record = new SignInRecord();
        record.setUserId(userId);
        record.setCoupleId(user.getCoupleId());
        record.setSignDate(today);
        signInRecordRepository.save(record);

        // 计算连续签到天数
        int streakDays = calculateStreak(userId, today);

        // 爱情树成长值 +2
        String desc = "第 " + streakDays + " 天连续签到";
        try {
            loveTreeService.addGrowth(user.getCoupleId(), "sign_in",
                    ILoveTreeService.GROWTH_SIGN_IN, record.getId(), desc);
        } catch (Exception e) {
            log.warn("Failed to add growth for sign-in", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("signInRecord", record);
        result.put("streakDays", streakDays);
        result.put("growthAdded", ILoveTreeService.GROWTH_SIGN_IN);
        return result;
    }

    /**
     * 获取签到状态
     */
    public Map<String, Object> getSignInStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        LocalDate today = LocalDate.now();
        boolean signedInToday = signInRecordRepository.existsByUserIdAndSignDate(userId, today);

        int streakDays = signedInToday ? calculateStreak(userId, today) : calculateStreak(userId, today.minusDays(1));

        Map<String, Object> result = new HashMap<>();
        result.put("signedInToday", signedInToday);
        result.put("streakDays", streakDays);

        // 当月签到天数
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        long monthCount = signInRecordRepository.findRecentByUserId(userId, PageRequest.of(0, 31))
                .stream()
                .filter(r -> !r.getSignDate().isBefore(firstOfMonth))
                .count();
        result.put("monthCount", monthCount);

        return result;
    }

    /**
     * 计算连续签到天数
     */
    private int calculateStreak(Long userId, LocalDate fromDate) {
        List<SignInRecord> recentRecords = signInRecordRepository
                .findRecentByUserId(userId, PageRequest.of(0, 365));

        int streak = 0;
        LocalDate checkDate = fromDate;

        for (SignInRecord record : recentRecords) {
            if (record.getSignDate().equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (record.getSignDate().isBefore(checkDate)) {
                // 如果今天的签到了，检查昨天；如果没有今天签到，从昨天开始算
                // 但这里我们已经传入正确的 fromDate，所以直接中断
                break;
            }
        }

        return streak;
    }

    /**
     * 获取情侣签到统计
     */
    public Map<String, Object> getCoupleSignInStats(Long coupleId) {
        long totalRecords = signInRecordRepository.countByCoupleId(coupleId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", totalRecords);
        return stats;
    }
}
