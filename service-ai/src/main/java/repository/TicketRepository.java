package com.mindx.service_ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mindx.service_ai.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
