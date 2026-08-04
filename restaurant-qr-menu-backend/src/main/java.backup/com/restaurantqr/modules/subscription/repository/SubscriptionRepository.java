package com.restaurantqr.modules.subscription.repository;

import com.restaurantqr.modules.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s WHERE s.restaurant.id = :restaurantId " +
           "AND s.status = 'ACTIVE' AND s.endDate >= :today ORDER BY s.endDate DESC")
    Optional<Subscription> findActiveSubscription(Long restaurantId, LocalDate today);

    @Query("SELECT s FROM Subscription s WHERE s.restaurant.id = :restaurantId " +
           "AND s.isDeleted = false ORDER BY s.createdAt DESC")
    List<Subscription> findByRestaurantId(Long restaurantId);

    // Find subs expiring within N days (for reminder emails)
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.endDate BETWEEN :today AND :expiryDate")
    List<Subscription> findExpiringSoon(LocalDate today, LocalDate expiryDate);
}
