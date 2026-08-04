package com.restaurantqr.modules.offer.repository;

import com.restaurantqr.modules.offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    @Query("SELECT o FROM Offer o WHERE o.restaurant.id = :restaurantId " +
           "AND o.isDeleted = false AND o.status = 'ACTIVE' " +
           "AND o.startDate <= :today AND o.endDate >= :today")
    List<Offer> findActiveOffers(Long restaurantId, LocalDate today);

    @Query("SELECT o FROM Offer o WHERE o.restaurant.id = :restaurantId AND o.isDeleted = false " +
           "ORDER BY o.createdAt DESC")
    List<Offer> findAllByRestaurantId(Long restaurantId);
}
