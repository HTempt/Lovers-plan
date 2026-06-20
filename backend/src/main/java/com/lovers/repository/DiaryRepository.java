package com.lovers.repository;

import com.lovers.model.Diary;
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
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Page<Diary> findByCoupleIdAndStatusOrderByCreateTimeDesc(Long coupleId, Integer status, Pageable pageable);

    List<Diary> findByCoupleIdAndStatusOrderByCreateTimeDesc(Long coupleId, Integer status);

    long countByCoupleIdAndStatus(Long coupleId, Integer status);

    /** 统计情侣全部日记数（含已删除） */
    long countByCoupleId(Long coupleId);

    /** 统计带图片的日记数 */
    @Query("SELECT COUNT(DISTINCT d.id) FROM Diary d JOIN DiaryMedia m ON d.id = m.diaryId WHERE d.coupleId = :coupleId AND d.status = 1 AND m.mediaType = 'image'")
    long countPhotoDiaries(@Param("coupleId") Long coupleId);

    Optional<Diary> findByIdAndCreatorId(Long id, Long creatorId);

    @Query("SELECT DISTINCT d.location FROM Diary d WHERE d.coupleId = :coupleId AND d.status = :status AND d.location IS NOT NULL AND d.location <> ''")
    List<String> findDistinctLocationsByCoupleId(@Param("coupleId") Long coupleId, @Param("status") Integer status);

    List<Diary> findByStatusAndDeleteTimeBefore(Integer status, LocalDateTime before);

    /**
     * 查询历史同月同日的日记（用于回忆重现）
     */
    @Query(value = "SELECT * FROM diary WHERE couple_id = :coupleId AND status = 1 " +
           "AND MONTH(create_time) = :month AND DAY(create_time) = :day " +
           "ORDER BY create_time DESC", nativeQuery = true)
    List<Diary> findMemoriesByMonthDay(@Param("coupleId") Long coupleId,
                                       @Param("month") int month,
                                       @Param("day") int day);

    /**
     * 查询历史±3天范围的日记（当月无回忆时兜底）
     */
    @Query(value = "SELECT * FROM diary WHERE couple_id = :coupleId AND status = 1 " +
           "AND MONTH(create_time) = :month AND DAY(create_time) BETWEEN :startDay AND :endDay " +
           "ORDER BY ABS(DAY(create_time) - :targetDay) ASC, create_time DESC",
           nativeQuery = true)
    List<Diary> findMemoriesByMonthDayRange(@Param("coupleId") Long coupleId,
                                            @Param("month") int month,
                                            @Param("startDay") int startDay,
                                            @Param("endDay") int endDay,
                                            @Param("targetDay") int targetDay);
}
