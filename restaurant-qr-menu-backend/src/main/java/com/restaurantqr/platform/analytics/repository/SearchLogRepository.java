package com.restaurantqr.platform.analytics.repository;

import com.restaurantqr.platform.analytics.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {


    Optional<SearchLog> findByRestaurantIdAndSearchTerm(Long restaurantId, String searchTerm);

    @Query("SELECT s.searchTerm, s.searchCount FROM SearchLog s " +
           "WHERE s.restaurant.id = :restaurantId AND s.isDeleted = false " +
           "ORDER BY s.searchCount DESC")
    List<Object[]> findTopSearchTerms(Long restaurantId);
}
