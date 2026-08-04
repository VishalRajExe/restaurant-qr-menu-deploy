package com.restaurantqr.platform.modules.menuitem.repository;

import com.restaurantqr.platform.modules.menuitem.entity.CustomerFavorite;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerFavoriteRepository extends JpaRepository<CustomerFavorite, Long> {

    Optional<CustomerFavorite> findByDeviceTokenAndMenuItemId(String deviceToken, Long menuItemId);

    @Query("SELECT f.menuItem FROM CustomerFavorite f " +
           "WHERE f.deviceToken = :deviceToken AND f.restaurant.id = :restaurantId " +
           "AND f.isDeleted = false AND f.menuItem.isDeleted = false AND f.menuItem.status = 'ACTIVE'")
    List<MenuItem> findFavoritesByDeviceTokenAndRestaurant(String deviceToken, Long restaurantId);

    boolean existsByDeviceTokenAndMenuItemIdAndIsDeletedFalse(String deviceToken, Long menuItemId);
}
