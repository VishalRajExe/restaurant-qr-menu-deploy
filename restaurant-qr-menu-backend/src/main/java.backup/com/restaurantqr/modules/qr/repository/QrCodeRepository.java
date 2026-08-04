package com.restaurantqr.modules.qr.repository;

import com.restaurantqr.modules.qr.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    Optional<QrCode> findByTokenAndStatus(String token, QrCode.Status status);

    Optional<QrCode> findByToken(String token);

    @Query("SELECT q FROM QrCode q WHERE q.branch.id = :branchId AND q.isDeleted = false")
    List<QrCode> findByBranchId(Long branchId);

    @Query("SELECT q FROM QrCode q WHERE q.restaurant.id = :restaurantId AND q.isDeleted = false " +
           "ORDER BY q.createdAt DESC")
    List<QrCode> findByRestaurantId(Long restaurantId);
}
