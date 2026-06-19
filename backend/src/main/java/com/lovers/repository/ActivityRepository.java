package com.lovers.repository;

import com.lovers.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Page<Activity> findByCoupleIdOrderByCreateTimeDesc(Long coupleId, Pageable pageable);
}
