package com.lovers.repository;

import com.lovers.model.TimeCapsule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeCapsuleRepository extends JpaRepository<TimeCapsule, Long> {

    /** 情侣所有胶囊（分页，按创建时间倒序） */
    Page<TimeCapsule> findByCoupleIdOrderByCreateTimeDesc(Long coupleId, Pageable pageable);

    /** 情侣指定状态的胶囊 */
    Page<TimeCapsule> findByCoupleIdAndStatusOrderByCreateTimeDesc(Long coupleId, Integer status, Pageable pageable);

    /** 统计情侣某状态的胶囊数 */
    long countByCoupleIdAndStatus(Long coupleId, Integer status);

    /** 查单个胶囊（验证归属） */
    Optional<TimeCapsule> findByIdAndCoupleId(Long id, Long coupleId);

    /** 查询某个 pair_id 下的另一条记录 */
    List<TimeCapsule> findByPairId(Long pairId);

    /** 查询情侣所有草稿（用于双人模式检查） */
    List<TimeCapsule> findByCoupleIdAndUserIdAndStatus(Long coupleId, Long userId, Integer status);

    /** 查询已到期的未开启胶囊（定时任务用） */
    @Query("SELECT t FROM TimeCapsule t WHERE t.status = 1 AND t.openAt <= :now")
    List<TimeCapsule> findMatureCapsules(@Param("now") LocalDateTime now);

    /** 统计即将开启的胶囊数量（status=1 且 openAt 在未来 N 天内） */
    @Query("SELECT COUNT(t) FROM TimeCapsule t WHERE t.coupleId = :coupleId AND t.status = 1 AND t.openAt BETWEEN :now AND :future")
    long countAboutToOpen(@Param("coupleId") Long coupleId, @Param("now") LocalDateTime now, @Param("future") LocalDateTime future);

    /** 查询最近的未来开启时间（用于计算"还有X天"） */
    @Query("SELECT MIN(t.openAt) FROM TimeCapsule t WHERE t.coupleId = :coupleId AND t.status = 1 AND t.openAt > :now")
    Optional<LocalDateTime> findNextOpenAt(@Param("coupleId") Long coupleId, @Param("now") LocalDateTime now);

    /** 统计最近创建的胶囊数量（status=1 且 createTime 在过去 N 小时内） */
    @Query("SELECT COUNT(t) FROM TimeCapsule t WHERE t.coupleId = :coupleId AND t.status = 1 AND t.createTime >= :since")
    long countRecentlyCreated(@Param("coupleId") Long coupleId, @Param("since") LocalDateTime since);

    /** 统计待当前用户写入的胶囊数（DRAFT 且不是自己创建的 = 伴侣创建了但自己还没写） */
    @Query("SELECT COUNT(t) FROM TimeCapsule t WHERE t.coupleId = :coupleId AND t.status = 0 AND t.userId <> :userId")
    long countPendingForUser(@Param("coupleId") Long coupleId, @Param("userId") Long userId);

    /** 统计某状态以外的胶囊数 */
    long countByCoupleIdAndStatusNot(Long coupleId, Integer status);

    /** 统计双方共同完成的胶囊数（pairId 非空） */
    @Query("SELECT COUNT(t) FROM TimeCapsule t WHERE t.coupleId = :coupleId AND t.pairId IS NOT NULL AND t.status >= 1")
    long countDualCompleted(@Param("coupleId") Long coupleId);
}
