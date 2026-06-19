package com.lovers.service;

import java.util.Map;

public interface ISignInService {
    Map<String, Object> signIn(Long userId);
    Map<String, Object> getSignInStatus(Long userId);
    Map<String, Object> getCoupleSignInStats(Long coupleId);
}
