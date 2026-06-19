package com.lovers.repository;

import com.lovers.model.Task;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCoupleIdOrderByCreateTimeDesc(Long coupleId);

    List<Task> findByCoupleIdAndStatusOrderByCreateTimeDesc(Long coupleId, Integer status);

    long countByCoupleIdAndStatus(Long coupleId, Integer status);

    /**
     * 查询历史同月同日完成的任务（用于回忆重现）
     */
    @Query(value = "SELECT * FROM task WHERE couple_id = :coupleId AND status = 2 " +
           "AND MONTH(update_time) = :month AND DAY(update_time) = :day " +
           "ORDER BY update_time DESC", nativeQuery = true)
    List<Task> findCompletedByMonthDay(@Param("coupleId") Long coupleId,
                                       @Param("month") int month,
                                       @Param("day") int day);

    @Query(value = "SELECT * FROM task WHERE couple_id = :coupleId AND status = 2 " +
           "AND MONTH(update_time) = :month AND DAY(update_time) BETWEEN :startDay AND :endDay " +
           "ORDER BY ABS(DAY(update_time) - :targetDay) ASC, update_time DESC",
           nativeQuery = true)
    List<Task> findCompletedByMonthDayRange(@Param("coupleId") Long coupleId,
                                            @Param("month") int month,
                                            @Param("startDay") int startDay,
                                            @Param("endDay") int endDay,
                                            @Param("targetDay") int targetDay);
}
