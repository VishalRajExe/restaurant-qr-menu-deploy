package com.restaurantqr.platform.modules.subscription.repository;

import com.restaurantqr.platform.modules.subscription.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeAndIsDeletedFalse(String code);
    boolean existsByCode(String code);
    long countByIsDeletedFalse();
}

