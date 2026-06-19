package com.lovers.service;

import com.lovers.model.User;
import java.util.Map;

public interface IUserService {
    Map<String, Object> wxLogin(String code);
    User getUserInfo(Long userId);
    User updateUserInfo(Long userId, String nickname, String avatar, Integer gender, String phone);
}
