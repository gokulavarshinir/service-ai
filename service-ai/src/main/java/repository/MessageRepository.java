package com.mindx.service_ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mindx.service_ai.model.Message;
import java.util.List;


public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByTicketId(Long ticketId);
}
