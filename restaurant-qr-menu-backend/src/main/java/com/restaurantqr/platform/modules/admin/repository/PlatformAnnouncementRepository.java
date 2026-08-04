package com.restaurantqr.platform.modules.admin.repository;

import com.restaurantqr.platform.modules.admin.entity.PlatformAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformAnnouncementRepository extends JpaRepository<PlatformAnnouncement, Long> {

    @Query("SELECT a FROM PlatformAnnouncement a WHERE a.isActive = true AND a.isDeleted = false " +
           "ORDER BY a.createdAt DESC")
    List<PlatformAnnouncement> findActiveAnnouncements();
}
