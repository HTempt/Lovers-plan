package com.lovers.service;

import com.lovers.model.TimeCapsule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ITimeCapsuleService {

    /** 胶囊类型 */
    List<String> TYPES = List.of("to_future_ta", "to_future_us", "birthday", "anniversary", "wish");

    /** 开启时间选项（天） */
    List<Integer> OPEN_DAYS_OPTIONS = List.of(30, 90, 180, 365);

    /** 创建胶囊 */
    TimeCapsule create(Long coupleId, Long userId, String type, String title, String content,
                       List<Map<String, String>> mediaList, LocalDateTime openAt, Boolean dualMode);

    /** 双人模式：对方写入内容 */
    TimeCapsule writePartner(Long coupleId, Long userId, Long pairCapsuleId,
                              String content, List<Map<String, String>> mediaList);

    /** 获取胶囊列表 */
    Map<String, Object> list(Long coupleId, Integer status, int page, int size);

    /** 获取胶囊详情（含媒体） */
    Map<String, Object> detail(Long coupleId, Long capsuleId);

    /** 开启胶囊 */
    TimeCapsule open(Long coupleId, Long capsuleId);

    /** 删除胶囊 */
    void delete(Long coupleId, Long capsuleId);

    /** 定时任务：将到期胶囊状态从 SEALED 更新为 OPENABLE */
    int checkAndUpdateMatureCapsules();
}
