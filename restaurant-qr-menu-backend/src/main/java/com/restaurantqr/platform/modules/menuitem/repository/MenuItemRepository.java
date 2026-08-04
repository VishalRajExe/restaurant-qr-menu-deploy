package com.restaurantqr.platform.modules.menuitem.repository;

import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId " +
           "AND m.isDeleted = false AND m.status = 'ACTIVE' " +
           "ORDER BY m.displayOrder ASC")
    List<MenuItem> findActiveByRestaurantId(Long restaurantId);

    @Query("SELECT m FROM MenuItem m WHERE m.category.id = :categoryId " +
           "AND m.isDeleted = false AND m.status = 'ACTIVE' " +
           "ORDER BY m.displayOrder ASC")
    List<MenuItem> findActiveByCategoryId(Long categoryId);

    // Full-text search with optional veg filter
    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId " +
           "AND m.isDeleted = false AND m.status = 'ACTIVE' " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:foodType IS NULL OR m.vegNonveg = :foodType) " +
           "ORDER BY m.displayOrder ASC")
    Page<MenuItem> searchMenu(Long restaurantId, String search,
                              MenuItem.FoodType foodType, Pageable pageable);

    long countByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId " +
           "AND m.isFeatured = true AND m.isDeleted = false AND m.status = 'ACTIVE'")
    List<MenuItem> findFeaturedByRestaurantId(Long restaurantId);

    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId " +
           "AND (m.isFeatured = true OR m.isPopular = true OR m.isChefSpecial = true) " +
           "AND m.isDeleted = false AND m.status = 'ACTIVE'")
    List<MenuItem> findRecommendedByRestaurantId(Long restaurantId);

    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId " +
           "AND m.isDeleted = false AND m.status = 'ACTIVE' " +
           "ORDER BY m.createdAt DESC")
    List<MenuItem> findRecentlyAddedByRestaurantId(Long restaurantId);

    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId " +
           "AND m.isCombo = true AND m.isDeleted = false AND m.status = 'ACTIVE'")
    List<MenuItem> findCombosByRestaurantId(Long restaurantId);

    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId " +
           "AND m.category.id = :categoryId AND m.id <> :excludeItemId " +
           "AND m.isDeleted = false AND m.status = 'ACTIVE'")
    List<MenuItem> findRelatedItems(Long restaurantId, Long categoryId, Long excludeItemId);
}

