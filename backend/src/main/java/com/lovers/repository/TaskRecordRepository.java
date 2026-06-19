package com.lovers.repository;

import com.lovers.model.TaskRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRecordRepository extends JpaRepository<TaskRecord, Long> {

    List<TaskRecord> findByTaskId(Long taskId);

    boolean existsByTaskIdAndUserIdAndStatus(Long taskId, Long userId, Integer status);
}
