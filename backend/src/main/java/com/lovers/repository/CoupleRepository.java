package com.lovers.repository;

import com.lovers.model.Couple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoupleRepository extends JpaRepository<Couple, Long> {

    Optional<Couple> findByIdAndStatus(Long id, Integer status);
}
