package com.lovers.repository;

import com.lovers.model.Anniversary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnniversaryRepository extends JpaRepository<Anniversary, Long> {

    List<Anniversary> findByCoupleIdAndStatusOrderByAnniversaryDateDesc(Long coupleId, Integer status);

    Optional<Anniversary> findByCoupleIdAndTypeAndStatus(Long coupleId, Integer type, Integer status);

    /**
     * 查询最近的一个纪念日（基于月日计算本年日期）
     */
    @Query(value = "SELECT * FROM anniversary WHERE couple_id = :coupleId AND status = 1 " +
           "ORDER BY " +
           "(CASE " +
           "  WHEN anniversary_date + INTERVAL (YEAR(CURDATE()) - YEAR(anniversary_date)) YEAR >= CURDATE() " +
           "  THEN anniversary_date + INTERVAL (YEAR(CURDATE()) - YEAR(anniversary_date)) YEAR " +
           "  ELSE anniversary_date + INTERVAL (YEAR(CURDATE()) + 1 - YEAR(anniversary_date)) YEAR " +
           "END) ASC LIMIT 1", nativeQuery = true)
    Optional<Anniversary> findUpcomingByCoupleId(@Param("coupleId") Long coupleId);

    boolean existsByCoupleIdAndTitleAndStatus(Long coupleId, String title, Integer status);

    boolean existsByCoupleIdAndTitleAndStatusAndIdNot(Long coupleId, String title, Integer status, Long id);
}
