package com.restaurantqr.platform.modules.order.repository;

import com.restaurantqr.platform.modules.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :restaurantId AND o.isDeleted = false ORDER BY o.createdAt DESC")
    List<Order> findByRestaurantIdOrdered(@Param("restaurantId") Long restaurantId);

    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber AND o.isDeleted = false")
    Optional<Order> findByOrderNumber(@Param("orderNumber") String orderNumber);

    @Query("SELECT o FROM Order o WHERE (o.customerMobile = :identifier OR o.orderNumber = :identifier) AND o.isDeleted = false ORDER BY o.createdAt DESC")
    List<Order> findByCustomerMobileOrOrderNumber(@Param("identifier") String identifier);

    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :restaurantId AND (o.customerMobile = :identifier OR o.orderNumber = :identifier) AND o.isDeleted = false ORDER BY o.createdAt DESC")
    List<Order> findByRestaurantIdAndCustomerMobileOrOrderNumber(@Param("restaurantId") Long restaurantId, @Param("identifier") String identifier);
}
