package com.lovers.repository;

import com.lovers.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findAllByOrderBySortOrderAsc();

    List<Achievement> findByCategoryOrderBySortOrderAsc(String category);

    Optional<Achievement> findByCode(String code);
}
