package com.lovers.service;

import com.lovers.model.LoveTree;
import java.util.Map;

public interface ILoveTreeService {

    /** 成长值规则 */
    int GROWTH_DIARY = 5;
    int GROWTH_TASK = 10;
    int GROWTH_WISH = 30;
    int GROWTH_SIGN_IN = 2;
    int GROWTH_STATUS = 1;
    int GROWTH_CAPSULE_CREATE = 10;
    int GROWTH_CAPSULE_OPEN = 20;

    LoveTree getOrCreateTree(Long coupleId);
    Map<String, Object> getTreeInfo(Long coupleId);
    Map<String, Object> addGrowth(Long coupleId, String actionType, int growthValue, Long sourceId, String description);
    Map<String, Object> getGrowthHistory(Long coupleId, int page, int size);
}
