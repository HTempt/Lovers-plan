package com.lovers.repository;

import com.lovers.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findByCoupleIdOrderByEarnedDateDesc(Long coupleId);

    Optional<Badge> findByCoupleIdAndBadgeType(Long coupleId, String badgeType);

    long countByCoupleId(Long coupleId);
}
