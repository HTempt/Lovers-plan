package com.lovers.repository;

import com.lovers.model.DailyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAnswerRepository extends JpaRepository<DailyAnswer, Long> {

    Optional<DailyAnswer> findByDailyQuestionIdAndUserId(Long dailyQuestionId, Long userId);

    List<DailyAnswer> findByDailyQuestionId(Long dailyQuestionId);

    long countByDailyQuestionId(Long dailyQuestionId);
}
