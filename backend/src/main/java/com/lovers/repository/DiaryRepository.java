package com.lovers.repository;

import com.lovers.model.Diary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Page<Diary> findByCoupleIdAndStatusOrderByCreateTimeDesc(Long coupleId, Integer status, Pageable pageable);

    List<Diary> findByCoupleIdAndStatusOrderByCreateTimeDesc(Long coupleId, Integer status);

    long countByCoupleIdAndStatus(Long coupleId, Integer status);

    Optional<Diary> findByIdAndCreatorId(Long id, Long creatorId);

    @Query("SELECT DISTINCT d.location FROM Diary d WHERE d.coupleId = :coupleId AND d.status = :status AND d.location IS NOT NULL AND d.location <> ''")
    List<String> findDistinctLocationsByCoupleId(@Param("coupleId") Long coupleId, @Param("status") Integer status);

    List<Diary> findByStatusAndDeleteTimeBefore(Integer status, LocalDateTime before);
}
