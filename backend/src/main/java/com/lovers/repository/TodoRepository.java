package com.lovers.repository;

import com.lovers.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByCoupleIdOrderByCreateTimeDesc(Long coupleId);

    List<Todo> findByCoupleIdAndStatusOrderByCreateTimeDesc(Long coupleId, Integer status);

    long countByCoupleIdAndStatus(Long coupleId, Integer status);
}
