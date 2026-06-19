package com.lovers.repository;

import com.lovers.model.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishRepository extends JpaRepository<Wish, Long> {

    List<Wish> findByCoupleIdOrderByCreateTimeDesc(Long coupleId);

    long countByCoupleIdAndStatus(Long coupleId, Integer status);

    /**
     * 查询历史同月同日达成的愿望（用于回忆重现）
     */
    @Query(value = "SELECT * FROM wish WHERE couple_id = :coupleId AND status = 2 " +
           "AND MONTH(update_time) = :month AND DAY(update_time) = :day " +
           "ORDER BY update_time DESC", nativeQuery = true)
    List<Wish> findFulfilledByMonthDay(@Param("coupleId") Long coupleId,
                                        @Param("month") int month,
                                        @Param("day") int day);

    @Query(value = "SELECT * FROM wish WHERE couple_id = :coupleId AND status = 2 " +
           "AND MONTH(update_time) = :month AND DAY(update_time) BETWEEN :startDay AND :endDay " +
           "ORDER BY ABS(DAY(update_time) - :targetDay) ASC, update_time DESC",
           nativeQuery = true)
    List<Wish> findFulfilledByMonthDayRange(@Param("coupleId") Long coupleId,
                                             @Param("month") int month,
                                             @Param("startDay") int startDay,
                                             @Param("endDay") int endDay,
                                             @Param("targetDay") int targetDay);
}
