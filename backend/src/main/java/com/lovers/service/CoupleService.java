package com.lovers.service;

import com.lovers.common.exception.BusinessException;
import com.lovers.model.Anniversary;
import com.lovers.model.Couple;
import com.lovers.model.User;
import com.lovers.repository.CoupleRepository;
import com.lovers.repository.UserRepository;
import com.lovers.service.AnniversaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CoupleService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnniversaryService anniversaryService;

    @Value("${invite.code-length}")
    private int codeLength;

    @Value("${invite.expire-hours}")
    private int expireHours;

    private static final String INVITE_CODE_PREFIX = "invite:code:";

    /**
     * 创建邀请码
     */
    public String createInviteCode(Long userId) {
        // 检查用户是否已绑定
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getCoupleId() != null) {
            throw new BusinessException("您已绑定情侣，无法创建邀请");
        }

        // 生成6位随机码
        String code = generateCode();
        // 存入Redis，24小时过期，value为创建者userId
        redisTemplate.opsForValue().set(INVITE_CODE_PREFIX + code, String.valueOf(userId), expireHours, TimeUnit.HOURS);

        return code;
    }

    /**
     * 接受邀请 - 绑定情侣关系
     */
    @Transactional
    public Map<String, Object> acceptInvite(Long userId, String code) {
        // 校验用户是否已绑定
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (currentUser.getCoupleId() != null) {
            throw new BusinessException("您已绑定情侣，无法接受邀请");
        }

        // 校验邀请码
        String creatorIdStr = redisTemplate.opsForValue().get(INVITE_CODE_PREFIX + code);
        if (creatorIdStr == null) {
            throw new BusinessException("邀请码无效或已过期");
        }

        Long creatorId = Long.parseLong(creatorIdStr);

        // 不能自己绑定自己
        if (creatorId.equals(userId)) {
            throw new BusinessException("不能绑定自己");
        }

        // 检查创建者是否已被绑定
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException("邀请者不存在"));

        if (creator.getCoupleId() != null) {
            redisTemplate.delete(INVITE_CODE_PREFIX + code);
            throw new BusinessException("邀请者已绑定其他情侣");
        }

        // 创建情侣关系
        Couple couple = new Couple();
        couple.setUserA(creatorId);
        couple.setUserB(userId);
        LocalDate loveDate = LocalDate.now();
        couple.setLoveDate(loveDate);
        couple = coupleRepository.save(couple);

        // 自动创建系统纪念日
        anniversaryService.createSystemAnniversary(couple.getId(), loveDate);

        // 更新双方用户的couple_id
        creator.setCoupleId(couple.getId());
        userRepository.save(creator);

        currentUser.setCoupleId(couple.getId());
        userRepository.save(currentUser);

        // 删除邀请码
        redisTemplate.delete(INVITE_CODE_PREFIX + code);

        return Map.of(
                "coupleId", couple.getId(),
                "loveDate", couple.getLoveDate().toString()
        );
    }

    /**
     * 获取情侣信息
     */
    public Map<String, Object> getCoupleInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getCoupleId() == null) {
            throw new BusinessException("您还未绑定情侣");
        }

        Couple couple = coupleRepository.findById(user.getCoupleId())
                .orElseThrow(() -> new BusinessException("情侣关系不存在"));

        User partner;
        if (couple.getUserA().equals(userId)) {
            partner = userRepository.findById(couple.getUserB()).orElse(null);
        } else {
            partner = userRepository.findById(couple.getUserA()).orElse(null);
        }

        return Map.of(
                "coupleId", couple.getId(),
                "loveDate", couple.getLoveDate() != null ? couple.getLoveDate().toString() : null,
                "partnerNickname", partner != null ? partner.getNickname() : "未知",
                "partnerAvatar", partner != null ? partner.getAvatar() : "",
                "partnerId", partner != null ? partner.getId() : null
        );
    }

    /**
     * 解除绑定
     */
    @Transactional
    public void unbind(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getCoupleId() == null) {
            throw new BusinessException("您未绑定情侣");
        }

        Couple couple = coupleRepository.findById(user.getCoupleId())
                .orElseThrow(() -> new BusinessException("情侣关系不存在"));

        // 标记关系已解除
        couple.setStatus(0);
        coupleRepository.save(couple);

        // 解除双方用户的绑定
        if (couple.getUserA() != null) {
            userRepository.findById(couple.getUserA()).ifPresent(u -> {
                u.setCoupleId(null);
                userRepository.save(u);
            });
        }
        if (couple.getUserB() != null) {
            userRepository.findById(couple.getUserB()).ifPresent(u -> {
                u.setCoupleId(null);
                userRepository.save(u);
            });
        }
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
