package com.lovers.service;

import com.lovers.model.Footprint;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IFootprintService {
    Footprint create(Long coupleId, Long diaryId, String province, String city, String locationName, BigDecimal latitude, BigDecimal longitude);
    List<Footprint> getFootprints(Long coupleId);
    Map<String, Object> getStats(Long coupleId);
    List<Map<String, Object>> getCityRanking(Long coupleId);
    List<String> getDistinctCities(Long coupleId);
    long getDistinctCityCount(Long coupleId);
    void checkAndAwardCityUnlock(Long coupleId);
}
