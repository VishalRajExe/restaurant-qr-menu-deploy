package com.restaurantqr.platform.modules.enterprise.repository;

import com.restaurantqr.platform.modules.enterprise.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByRestaurantIdAndIsActiveTrueAndIsDeletedFalse(Long restaurantId);
}
