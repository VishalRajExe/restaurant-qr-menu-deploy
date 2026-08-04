package com.restaurantqr.platform.modules.enterprise.repository;

import com.restaurantqr.platform.modules.enterprise.entity.CustomDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomDomainRepository extends JpaRepository<CustomDomain, Long> {

    Optional<CustomDomain> findByRestaurantId(Long restaurantId);

    Optional<CustomDomain> findByCustomDomain(String customDomain);
}
