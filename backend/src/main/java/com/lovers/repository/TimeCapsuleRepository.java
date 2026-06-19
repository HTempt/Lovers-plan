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
}
