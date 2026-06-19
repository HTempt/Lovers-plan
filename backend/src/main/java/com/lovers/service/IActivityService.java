package com.lovers.service;

import com.lovers.model.Activity;
import java.util.Map;

public interface IActivityService {
    Map<String, Object> getActivityFeed(Long coupleId, int page, int size);
    Activity recordActivity(Long coupleId, String type, String title, String description, Long refId, String icon);
    void backfillIfEmpty(Long coupleId);
}
