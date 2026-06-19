package com.lovers.service;

import java.util.Map;

public interface IDailySentenceService {
    Map<String, Object> generate(Long coupleId, Long userId);
}
