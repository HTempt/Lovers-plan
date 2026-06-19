package com.lovers.repository;

import com.lovers.model.Footprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FootprintRepository extends JpaRepository<Footprint, Long> {

    List<Footprint> findByCoupleIdOrderByCreateTimeDesc(Long coupleId);

    boolean existsByCoupleIdAndCity(Long coupleId, String city);

    long countByCoupleId(Long coupleId);

    long countDistinctCityByCoupleId(Long coupleId);

    @Query("SELECT f.city, COUNT(f) as cnt FROM Footprint f WHERE f.coupleId = :coupleId AND f.city IS NOT NULL AND f.city <> '' GROUP BY f.city ORDER BY cnt DESC")
    List<Object[]> countByCity(@Param("coupleId") Long coupleId);

    @Query("SELECT f.city, COUNT(f) as cnt FROM Footprint f WHERE f.coupleId = :coupleId AND f.city IS NOT NULL AND f.city <> '' GROUP BY f.city ORDER BY cnt DESC")
    List<Object[]> findCityRanking(@Param("coupleId") Long coupleId);

    @Query("SELECT DISTINCT f.city FROM Footprint f WHERE f.coupleId = :coupleId AND f.city IS NOT NULL AND f.city <> ''")
    List<String> findDistinctCities(@Param("coupleId") Long coupleId);
}
