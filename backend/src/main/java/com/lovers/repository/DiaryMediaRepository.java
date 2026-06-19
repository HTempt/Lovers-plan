package com.lovers.repository;

import com.lovers.model.DiaryMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaryMediaRepository extends JpaRepository<DiaryMedia, Long> {

    List<DiaryMedia> findByDiaryId(Long diaryId);

    void deleteByDiaryId(Long diaryId);
}
