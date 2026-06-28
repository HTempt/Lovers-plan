package com.lovers.service;

import com.lovers.model.Diary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IDiaryService {
    Diary create(Long userId, Long coupleId, String title, String content, String location,
                 String province, String city, BigDecimal latitude, BigDecimal longitude,
                 String mood, List<Map<String, String>> mediaList);
    Map<String, Object> getTimeline(Long coupleId, int page, int size);
    Map<String, List<Map<String, Object>>> getAlbum(Long coupleId);
    List<Map<String, Object>> getMapLocations(Long coupleId);
    void delete(Long userId, Long diaryId);
    void restore(Long userId, Long diaryId);
    List<Diary> getRecycleBin(Long coupleId);
    void cleanExpiredRecycleBin();
}
