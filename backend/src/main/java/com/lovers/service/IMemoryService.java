package com.lovers.service;

import java.util.List;
import java.util.Map;

public interface IMemoryService {

    /**
     * 获取"那天的我们"回忆重现
     * @param coupleId 情侣ID
     * @return 最多3条回忆记录
     */
    List<Map<String, Object>> getMemories(Long coupleId);
}
