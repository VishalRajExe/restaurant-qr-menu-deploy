package com.restaurantqr.platform.modules.enterprise.repository;

import com.restaurantqr.platform.modules.enterprise.entity.SystemBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemBackupRepository extends JpaRepository<SystemBackup, Long> {

    List<SystemBackup> findByIsDeletedFalseOrderByCreatedAtDesc();
}
