package com.lovers.repository;

import com.lovers.model.LoveTree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoveTreeRepository extends JpaRepository<LoveTree, Long> {

    Optional<LoveTree> findByCoupleId(Long coupleId);
}
