package com.lovers.service;

import java.util.List;
import java.util.Map;

public interface IAchievementService {

    /** 获取情侣成就概览（已解锁数、总数、百分比、等级称号） */
    Map<String, Object> getOverview(Long coupleId);

    /** 获取全部分类成就列表（含解锁状态） */
    List<Map<String, Object>> getAllAchievements(Long coupleId);

    /** 获取指定分类的成就列表 */
    List<Map<String, Object>> getAchievementsByCategory(Long coupleId, String category);

    /** 检查并解锁成就（返回本次新解锁的成就列表） */
    List<Map<String, Object>> checkAndUnlock(Long coupleId, String category, String code, Long userId);

    /** 获取最近解锁的成就（首页/爱情树用） */
    List<Map<String, Object>> getRecentUnlocks(Long coupleId, int limit);
}
