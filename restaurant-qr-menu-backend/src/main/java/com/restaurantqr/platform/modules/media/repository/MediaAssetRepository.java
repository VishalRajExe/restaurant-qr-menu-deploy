package com.restaurantqr.platform.modules.media.repository;

import com.restaurantqr.platform.modules.media.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    @Query("SELECT m FROM MediaAsset m WHERE m.restaurant.id = :restaurantId " +
           "AND m.isDeleted = false ORDER BY m.createdAt DESC")
    List<MediaAsset> findGalleryByRestaurantId(Long restaurantId);

    Optional<MediaAsset> findByIdAndRestaurantIdAndIsDeletedFalse(Long id, Long restaurantId);

    Optional<MediaAsset> findByPublicIdAndIsDeletedFalse(String publicId);
}
