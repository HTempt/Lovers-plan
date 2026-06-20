package com.lovers.repository;

import com.lovers.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByCoupleId(Long coupleId);

    Optional<UserAchievement> findByCoupleIdAndAchievementId(Long coupleId, Long achievementId);

    long countByCoupleId(Long coupleId);

    boolean existsByCoupleIdAndAchievementId(Long coupleId, Long achievementId);
}
