package com.lovers.repository;

import com.lovers.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCoupleIdOrderByCreateTimeDesc(Long coupleId);

    List<Task> findByCoupleIdAndStatusOrderByCreateTimeDesc(Long coupleId, Integer status);

    long countByCoupleIdAndStatus(Long coupleId, Integer status);
}
