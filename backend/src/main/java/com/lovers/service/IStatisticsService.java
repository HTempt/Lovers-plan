package com.lovers.service;

import java.util.Map;

public interface IStatisticsService {
    Map<String, Object> getOverview(Long userId);
}
