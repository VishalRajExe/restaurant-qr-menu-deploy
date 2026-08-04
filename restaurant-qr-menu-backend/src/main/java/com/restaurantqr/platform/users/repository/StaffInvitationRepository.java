package com.restaurantqr.platform.users.repository;

import com.restaurantqr.platform.users.entity.StaffInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, Long> {
    Optional<StaffInvitation> findByToken(String token);
    List<StaffInvitation> findByRestaurantId(Long restaurantId);
    Optional<StaffInvitation> findByRestaurantIdAndEmailAndStatus(Long restaurantId, String email, StaffInvitation.Status status);
}
