package com.lovers.service.impl;

import com.lovers.auth.JwtUtil;
import com.lovers.common.exception.BusinessException;
import com.lovers.model.User;
import com.lovers.repository.UserRepository;
import com.lovers.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl implements IUserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    @Value("${wechat.login-url}")
    private String loginUrl;

    /**
     * 微信登录 - 通过code换取openid，注册或登录，返回JWT token
     */
    @Transactional
    public Map<String, Object> wxLogin(String code) {
        // 调微信接口换取openid
        String openid = getOpenidFromWeChat(code);

        // 查用户是否存在
        Optional<User> userOpt = userRepository.findByOpenid(openid);
        User user;
        boolean isNewUser = false;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (user.getStatus() == 0) {
                throw new BusinessException("账号已被禁用");
            }
        } else {
            // 新用户注册
            user = new User();
            user.setOpenid(openid);
            user = userRepository.save(user);
            isNewUser = true;
        }

        // 生成JWT
        String token = jwtUtil.generateToken(user.getId(), openid);

        return Map.of(
                "token", token,
                "userId", user.getId(),
                "isNewUser", isNewUser,
                "hasCouple", user.getCoupleId() != null
        );
    }

    /**
     * 获取用户信息
     */
    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUserInfo(Long userId, String nickname, String avatar, Integer gender, String phone) {
        User user = getUserInfo(userId);
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        if (gender != null) {
            user.setGender(gender);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        return userRepository.save(user);
    }

    /**
     * 调用微信接口换取openid
     */
    private String getOpenidFromWeChat(String code) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = loginUrl + "?appid=" + appId + "&secret=" + appSecret
                    + "&js_code=" + java.net.URLEncoder.encode(code, "UTF-8")
                    + "&grant_type=authorization_code";

            // 微信返回 text/plain，先以 String 接收再手动解析 JSON
            String respBody = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = mapper.readValue(respBody,
                    new TypeReference<Map<String, Object>>() {});

            if (response.containsKey("errcode") && response.get("errcode") != null
                    && !"0".equals(String.valueOf(response.get("errcode")))) {
                Object errcode = response.get("errcode");
                Object errmsg = response.get("errmsg");
                log.error("WeChat login failed: code={}, msg={}", errcode, errmsg);
                throw new BusinessException("微信登录失败: " + errmsg);
            }

            return (String) response.get("openid");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call WeChat login API", e);
            throw new BusinessException("微信登录服务异常");
        }
    }
}
