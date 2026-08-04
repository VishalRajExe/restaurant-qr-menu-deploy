package com.restaurantqr.modules.analytics.repository;

import com.restaurantqr.modules.analytics.entity.ScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScanEventRepository extends JpaRepository<ScanEvent, Long> {

    long countByRestaurantIdAndCreatedAtBetween(Long restaurantId,
                                                LocalDateTime from,
                                                LocalDateTime to);

    @Query("SELECT FUNCTION('DATE', s.createdAt) as scanDate, COUNT(s) as count " +
           "FROM ScanEvent s WHERE s.restaurant.id = :restaurantId " +
           "AND s.createdAt >= :from GROUP BY FUNCTION('DATE', s.createdAt) " +
           "ORDER BY scanDate ASC")
    List<Object[]> countDailyScans(Long restaurantId, LocalDateTime from);

    @Query("SELECT s.deviceType, COUNT(s) FROM ScanEvent s " +
           "WHERE s.restaurant.id = :restaurantId " +
           "AND s.createdAt >= :from GROUP BY s.deviceType")
    List<Object[]> countByDeviceType(Long restaurantId, LocalDateTime from);

    @Query("SELECT s.qrCode.id, COUNT(s) FROM ScanEvent s " +
           "WHERE s.restaurant.id = :restaurantId " +
           "AND s.createdAt >= :from GROUP BY s.qrCode.id ORDER BY COUNT(s) DESC")
    List<Object[]> topQrCodes(Long restaurantId, LocalDateTime from);
}
