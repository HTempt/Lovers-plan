package com.lovers.service;

import java.util.Map;

public interface ICoupleService {
    String createInviteCode(Long userId);
    Map<String, Object> acceptInvite(Long userId, String code);
    Map<String, Object> getCoupleInfo(Long userId);
    void unbind(Long userId);
}
