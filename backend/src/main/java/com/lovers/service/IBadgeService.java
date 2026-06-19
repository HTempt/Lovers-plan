package com.lovers.service;

import com.lovers.model.Badge;
import java.util.List;

public interface IBadgeService {
    void checkAndAward(Long coupleId, Long taskId);
    List<Badge> getBadges(Long coupleId);
}
