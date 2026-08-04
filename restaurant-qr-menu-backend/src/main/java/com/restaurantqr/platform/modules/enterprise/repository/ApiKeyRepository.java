package com.restaurantqr.platform.modules.enterprise.repository;

import com.restaurantqr.platform.modules.enterprise.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    @Query("SELECT k FROM ApiKey k WHERE k.restaurant.id = :restaurantId " +
           "AND k.isDeleted = false ORDER BY k.createdAt DESC")
    List<ApiKey> findByRestaurantId(Long restaurantId);
}
