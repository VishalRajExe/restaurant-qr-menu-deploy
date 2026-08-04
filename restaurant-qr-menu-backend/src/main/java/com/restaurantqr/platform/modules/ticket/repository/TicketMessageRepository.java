package com.restaurantqr.platform.modules.ticket.repository;

import com.restaurantqr.platform.modules.ticket.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    @Query("SELECT m FROM TicketMessage m WHERE m.ticket.id = :ticketId " +
           "AND m.isDeleted = false ORDER BY m.createdAt ASC")
    List<TicketMessage> findAllMessagesForTicket(Long ticketId);

    @Query("SELECT m FROM TicketMessage m WHERE m.ticket.id = :ticketId " +
           "AND m.isInternalNote = false AND m.isDeleted = false ORDER BY m.createdAt ASC")
    List<TicketMessage> findCustomerMessagesForTicket(Long ticketId);
}
