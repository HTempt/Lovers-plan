package com.lovers.repository;

import com.lovers.model.SignInRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SignInRecordRepository extends JpaRepository<SignInRecord, Long> {

    Optional<SignInRecord> findByUserIdAndSignDate(Long userId, LocalDate signDate);

    boolean existsByUserIdAndSignDate(Long userId, LocalDate signDate);

    /** 查询某个用户最近的签到记录（用于计算连续签到） */
    @Query("SELECT s FROM SignInRecord s WHERE s.userId = :userId ORDER BY s.signDate DESC")
    List<SignInRecord> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByCoupleId(Long coupleId);

    /** 查询情侣所有签到记录（按签到日期倒序） */
    List<SignInRecord> findByCoupleIdOrderBySignDateDesc(Long coupleId);
}
