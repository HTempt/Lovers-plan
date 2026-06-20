package com.lovers.repository;

import com.lovers.model.DailyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAnswerRepository extends JpaRepository<DailyAnswer, Long> {

    Optional<DailyAnswer> findByDailyQuestionIdAndUserId(Long dailyQuestionId, Long userId);

    List<DailyAnswer> findByDailyQuestionId(Long dailyQuestionId);

    long countByDailyQuestionId(Long dailyQuestionId);

    /** 统计情侣全部问答次数 */
    @Query(value = "SELECT COUNT(da.id) FROM daily_answer da " +
           "JOIN daily_question dq ON da.daily_question_id = dq.id " +
           "WHERE dq.couple_id = :coupleId", nativeQuery = true)
    long countByCoupleId(@Param("coupleId") Long coupleId);

    /** 统计默契一致的天数（双方答案相同） */
    @Query(value = "SELECT COUNT(*) FROM (" +
           "  SELECT da.daily_question_id FROM daily_answer da " +
           "  JOIN daily_question dq ON da.daily_question_id = dq.id " +
           "  WHERE dq.couple_id = :coupleId " +
           "  GROUP BY da.daily_question_id " +
           "  HAVING COUNT(da.id) >= 2 AND MIN(da.answer) = MAX(da.answer)" +
           ") AS matches", nativeQuery = true)
    long countPerfectMatches(@Param("coupleId") Long coupleId);

    /** 统计有答题记录的天数 */
    @Query(value = "SELECT COUNT(DISTINCT dq.question_date) FROM daily_answer da " +
           "JOIN daily_question dq ON da.daily_question_id = dq.id " +
           "WHERE dq.couple_id = :coupleId", nativeQuery = true)
    long countAnswerDays(@Param("coupleId") Long coupleId);
}
