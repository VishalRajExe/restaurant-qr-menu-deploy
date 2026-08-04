package com.restaurantqr.platform.analytics.repository;

import com.restaurantqr.platform.analytics.entity.ScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScanEventRepository extends JpaRepository<ScanEvent, Long> {

    long countByRestaurantId(Long restaurantId);

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

    @Query("SELECT COUNT(DISTINCT s.ipAddress) FROM ScanEvent s " +
           "WHERE s.restaurant.id = :restaurantId AND s.createdAt >= :from AND s.createdAt <= :to")
    long countUniqueVisitors(Long restaurantId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT FUNCTION('HOUR', s.createdAt) as hour, COUNT(s) as count " +
           "FROM ScanEvent s WHERE s.restaurant.id = :restaurantId " +
           "AND s.createdAt >= :from GROUP BY FUNCTION('HOUR', s.createdAt) " +
           "ORDER BY hour ASC")
    List<Object[]> countHourlyScans(Long restaurantId, LocalDateTime from);

    @Query("SELECT s.qrCode.id, COUNT(s) FROM ScanEvent s " +
           "WHERE s.restaurant.id = :restaurantId " +
           "AND s.createdAt >= :from GROUP BY s.qrCode.id ORDER BY COUNT(s) DESC")
    List<Object[]> topQrCodes(Long restaurantId, LocalDateTime from);

    @Query("SELECT s.qrCode.branch.name, COUNT(s) FROM ScanEvent s " +
           "WHERE s.restaurant.id = :restaurantId " +
           "AND s.createdAt >= :from GROUP BY s.qrCode.branch.name ORDER BY COUNT(s) DESC")
    List<Object[]> topBranches(Long restaurantId, LocalDateTime from);
}


