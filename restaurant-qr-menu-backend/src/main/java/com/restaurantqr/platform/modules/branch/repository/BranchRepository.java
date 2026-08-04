package com.restaurantqr.platform.modules.branch.repository;

import com.restaurantqr.platform.modules.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    @Query("SELECT b FROM Branch b WHERE b.restaurant.id = :restaurantId AND b.isDeleted = false")
    List<Branch> findByRestaurantId(Long restaurantId);

    long countByRestaurantIdAndIsDeletedFalse(Long restaurantId);
}
