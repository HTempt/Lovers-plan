package com.lovers.repository;

import com.lovers.model.TimeCapsuleMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeCapsuleMediaRepository extends JpaRepository<TimeCapsuleMedia, Long> {

    List<TimeCapsuleMedia> findByCapsuleId(Long capsuleId);

    void deleteByCapsuleId(Long capsuleId);
}
