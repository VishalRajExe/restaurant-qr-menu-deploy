package com.restaurantqr.platform.modules.ticket.repository;

import com.restaurantqr.platform.modules.ticket.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    Page<SupportTicket> findByRestaurantIdAndIsDeletedFalse(Long restaurantId, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE t.isDeleted = false ORDER BY t.createdAt DESC")
    Page<SupportTicket> findAllTickets(Pageable pageable);

    long countByStatusAndIsDeletedFalse(SupportTicket.Status status);

    long countByPriorityAndIsDeletedFalse(SupportTicket.Priority priority);

    long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime date);

    long countByStatusAndUpdatedAtAfterAndIsDeletedFalse(SupportTicket.Status status, LocalDateTime date);

    @Query("SELECT t FROM SupportTicket t WHERE t.status <> 'RESOLVED' AND t.status <> 'CLOSED' " +
           "AND (t.slaResponseDeadline < CURRENT_TIMESTAMP OR t.slaResolutionDeadline < CURRENT_TIMESTAMP) " +
           "AND t.isDeleted = false")
    List<SupportTicket> findSlaOverdueTickets();

    @Query("SELECT AVG(t.rating) FROM SupportTicket t WHERE t.rating > 0 AND t.isDeleted = false")
    Double getAverageCustomerRating();
}
