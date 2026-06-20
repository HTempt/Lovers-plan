package com.lovers.service;

import java.util.Map;

public interface IDailyQuestionService {

    /** 获取今日问题（不存在则自动分配） */
    Map<String, Object> getTodayQuestion(Long coupleId);

    /** 提交答案 */
    Map<String, Object> submitAnswer(Long coupleId, Long userId, Long dailyQuestionId, String answer);

    /** 获取今日答题结果 */
    Map<String, Object> getResult(Long coupleId);

    /** 获取今日答题结果（含当前用户标识，用于区分双方答案） */
    Map<String, Object> getResult(Long coupleId, Long currentUserId);
}
