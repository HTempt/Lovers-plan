package com.lovers.service.impl;

import com.lovers.model.Footprint;
import com.lovers.repository.FootprintRepository;
import com.lovers.service.IActivityService;
import com.lovers.service.IFootprintService;
import com.lovers.service.ILoveTreeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FootprintServiceImpl implements IFootprintService {

    private static final Logger log = LoggerFactory.getLogger(FootprintServiceImpl.class);

    /** 城市解锁奖励成长值 */
    private static final int[] CITY_UNLOCK_GROWTH = {10, 20, 50};
    private static final int[] CITY_UNLOCK_THRESHOLDS = {1, 5, 10};

    @Autowired
    private FootprintRepository footprintRepository;

    @Autowired
    private IActivityService activityService;

    @Autowired
    private ILoveTreeService loveTreeService;

    @Override
    @Transactional
    public Footprint create(Long coupleId, Long diaryId, String province, String city,
                            String locationName, BigDecimal latitude, BigDecimal longitude) {
        Footprint fp = new Footprint();
        fp.setCoupleId(coupleId);
        fp.setDiaryId(diaryId);
        fp.setProvince(province);
        fp.setCity(city);
        fp.setLocationName(locationName);
        fp.setLatitude(latitude);
        fp.setLongitude(longitude);
        fp = footprintRepository.save(fp);

        // 检测是否解锁新城市
        if (city != null && !city.isEmpty()) {
            List<String> existingCities = footprintRepository.findDistinctCities(coupleId);
            boolean isNewCity = !existingCities.contains(city);
            if (isNewCity) {
                try {
                    activityService.recordActivity(coupleId, "footprint",
                            "📍 解锁新城市：" + city,
                            province != null && !province.isEmpty() ? province : city,
                            fp.getId(), "📍");
                } catch (Exception e) {
                    log.warn("Failed to record footprint activity", e);
                }
            }
            // 检查城市成就
            checkAndAwardCityUnlock(coupleId);
        }

        return fp;
    }

    @Override
    public List<Footprint> getFootprints(Long coupleId) {
        return footprintRepository.findByCoupleIdOrderByCreateTimeDesc(coupleId);
    }

    @Override
    public Map<String, Object> getStats(Long coupleId) {
        long totalPlaces = footprintRepository.countByCoupleId(coupleId);
        long totalCities = footprintRepository.countDistinctCityByCoupleId(coupleId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlaces", totalPlaces);
        stats.put("totalCities", totalCities);
        return stats;
    }

    @Override
    public List<Map<String, Object>> getCityRanking(Long coupleId) {
        List<Object[]> rows = footprintRepository.findCityRanking(coupleId);
        return rows.stream().map(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("city", row[0]);
            item.put("count", row[1]);
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getDistinctCities(Long coupleId) {
        return footprintRepository.findDistinctCities(coupleId);
    }

    @Override
    public long getDistinctCityCount(Long coupleId) {
        return footprintRepository.countDistinctCityByCoupleId(coupleId);
    }

    @Override
    @Transactional
    public void checkAndAwardCityUnlock(Long coupleId) {
        long cityCount = getDistinctCityCount(coupleId);
        for (int i = 0; i < CITY_UNLOCK_THRESHOLDS.length; i++) {
            if (cityCount == CITY_UNLOCK_THRESHOLDS[i]) {
                try {
                    loveTreeService.addGrowth(coupleId, "footprint",
                            CITY_UNLOCK_GROWTH[i], null,
                            "解锁 " + CITY_UNLOCK_THRESHOLDS[i] + " 座城市成就 🌍");
                    log.info("City unlock award: coupleId={} cities={} growth=+{}",
                            coupleId, cityCount, CITY_UNLOCK_GROWTH[i]);
                } catch (Exception e) {
                    log.warn("Failed to award city unlock growth", e);
                }
            }
        }
    }
}
