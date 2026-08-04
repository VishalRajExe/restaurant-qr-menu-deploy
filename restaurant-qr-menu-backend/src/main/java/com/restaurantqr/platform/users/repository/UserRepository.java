// ─── UserRepository ────────────────────────────────────────────────────────────
package com.restaurantqr.platform.users.repository;

import com.restaurantqr.platform.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);


    @Query("SELECT u FROM User u WHERE u.restaurant.id = :restaurantId AND u.isDeleted = false")
    Page<User> findByRestaurantId(Long restaurantId, Pageable pageable);

    long countByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    Optional<User> findByResetTokenAndIsDeletedFalse(String resetToken);
}

