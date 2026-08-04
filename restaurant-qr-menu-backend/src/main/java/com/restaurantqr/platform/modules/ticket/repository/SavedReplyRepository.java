package com.restaurantqr.platform.modules.ticket.repository;

import com.restaurantqr.platform.modules.ticket.entity.SavedReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedReplyRepository extends JpaRepository<SavedReply, Long> {

    List<SavedReply> findByIsDeletedFalse();
}
