package com.lovers.service;

import com.lovers.model.Anniversary;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IAnniversaryService {
    Anniversary create(Long coupleId, String title, LocalDate anniversaryDate, Integer remindDays, String icon);
    Anniversary createSystemAnniversary(Long coupleId, LocalDate loveDate);
    Map<String, Object> getUpcomingAnniversary(Long coupleId);
    List<Map<String, Object>> listByCouple(Long coupleId);
    void delete(Long anniversaryId);
    Anniversary update(Long id, String title, LocalDate anniversaryDate, Integer remindDays, String icon);
}
