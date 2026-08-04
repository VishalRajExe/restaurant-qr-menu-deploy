package com.restaurantqr.platform.modules.category.repository;

import com.restaurantqr.platform.modules.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.restaurant.id = :restaurantId " +
           "AND c.isDeleted = false ORDER BY c.displayOrder ASC")
    List<Category> findByRestaurantIdOrdered(Long restaurantId);

    @Query("SELECT c FROM Category c WHERE c.restaurant.id = :restaurantId " +
           "AND c.status = 'ACTIVE' AND c.isDeleted = false ORDER BY c.displayOrder ASC")
    List<Category> findActiveByRestaurantId(Long restaurantId);

    long countByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    @Modifying
    @Query("UPDATE Category c SET c.displayOrder = :order WHERE c.id = :id")
    void updateDisplayOrder(Long id, Integer order);
}
