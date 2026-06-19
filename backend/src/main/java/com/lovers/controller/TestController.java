package com.lovers.controller;

import com.lovers.auth.JwtUtil;
import com.lovers.common.Result;
import com.lovers.model.Anniversary;
import com.lovers.model.Couple;
import com.lovers.model.User;
import com.lovers.repository.AnniversaryRepository;
import com.lovers.repository.CoupleRepository;
import com.lovers.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/test")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private AnniversaryRepository anniversaryRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 生成测试情侣数据（两个用户 + 情侣关系 + 系统纪念日）
     * POST /api/auth/test/seed
     */
    @PostMapping("/seed")
    public Result<Map<String, Object>> seedTestData() {
        // 创建用户A
        User userA = new User();
        userA.setOpenid("test_openid_a_" + System.currentTimeMillis());
        userA.setNickname("小明");
        userA.setGender(1);
        userA = userRepository.save(userA);

        // 创建用户B
        User userB = new User();
        userB.setOpenid("test_openid_b_" + System.currentTimeMillis());
        userB.setNickname("小红");
        userB.setGender(2);
        userB = userRepository.save(userB);

        // 创建情侣关系
        Couple couple = new Couple();
        couple.setUserA(userA.getId());
        couple.setUserB(userB.getId());
        couple.setLoveDate(LocalDate.of(2026, 1, 1));
        couple = coupleRepository.save(couple);

        // 更新双方用户的couple_id
        userA.setCoupleId(couple.getId());
        userRepository.save(userA);

        userB.setCoupleId(couple.getId());
        userRepository.save(userB);

        // 创建系统纪念日
        Anniversary anniversary = new Anniversary();
        anniversary.setCoupleId(couple.getId());
        anniversary.setTitle("恋爱纪念日");
        anniversary.setAnniversaryDate(LocalDate.of(2026, 1, 1));
        anniversary.setRemindDays(0);
        anniversary.setType(1);
        anniversary.setStatus(1);
        anniversaryRepository.save(anniversary);

        // 生成JWT
        String tokenA = jwtUtil.generateToken(userA.getId(), userA.getOpenid());
        String tokenB = jwtUtil.generateToken(userB.getId(), userB.getOpenid());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "测试数据生成成功");
        result.put("coupleId", couple.getId());
        result.put("loveDate", "2026-01-01");
        result.put("userA", Map.of(
                "id", userA.getId(),
                "nickname", userA.getNickname(),
                "token", tokenA
        ));
        result.put("userB", Map.of(
                "id", userB.getId(),
                "nickname", userB.getNickname(),
                "token", tokenB
        ));

        return Result.success(result);
    }

    /**
     * 为已存在的用户绑定一个测试伴侣
     * POST /api/auth/test/create-partner
     * @param request { userId: 你的用户ID }
     */
    @PostMapping("/create-partner")
    public Result<Map<String, Object>> createPartner(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        if (userIdObj == null) {
            return Result.error("请提供userId");
        }
        Long userId = Long.valueOf(userIdObj.toString());

        User existingUser = userRepository.findById(userId)
                .orElse(null);
        if (existingUser == null) {
            return Result.error("用户不存在");
        }
        if (existingUser.getCoupleId() != null) {
            return Result.error("用户已绑定情侣");
        }

        // 创建测试伴侣
        User partner = new User();
        partner.setOpenid("test_partner_" + System.currentTimeMillis());
        partner.setNickname("小红");
        partner.setGender(2);
        partner = userRepository.save(partner);

        // 创建情侣关系
        Couple couple = new Couple();
        couple.setUserA(userId);
        couple.setUserB(partner.getId());
        couple.setLoveDate(LocalDate.of(2026, 1, 1));
        couple = coupleRepository.save(couple);

        // 更新双方couple_id
        existingUser.setCoupleId(couple.getId());
        userRepository.save(existingUser);
        partner.setCoupleId(couple.getId());
        userRepository.save(partner);

        // 创建系统纪念日
        Anniversary anniversary = new Anniversary();
        anniversary.setCoupleId(couple.getId());
        anniversary.setTitle("恋爱纪念日");
        anniversary.setAnniversaryDate(LocalDate.of(2026, 1, 1));
        anniversary.setRemindDays(0);
        anniversary.setType(1);
        anniversary.setStatus(1);
        anniversaryRepository.save(anniversary);

        // 生成伴侣的JWT
        String partnerToken = jwtUtil.generateToken(partner.getId(), partner.getOpenid());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "伴侣绑定成功");
        result.put("coupleId", couple.getId());
        result.put("loveDate", "2026-01-01");
        result.put("partner", Map.of(
                "id", partner.getId(),
                "nickname", partner.getNickname(),
                "token", partnerToken
        ));

        return Result.success(result);
    }
}
