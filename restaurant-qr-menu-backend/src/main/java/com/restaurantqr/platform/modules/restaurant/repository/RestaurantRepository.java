package com.restaurantqr.platform.modules.restaurant.repository;

import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalse(String slug);

    @Query("SELECT r FROM Restaurant r WHERE r.isDeleted = false " +
           "AND (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Restaurant> findAllActive(String search, Pageable pageable);

    long countByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(Restaurant.Status status);

    long countByIsTrialTrueAndIsDeletedFalse();
}

