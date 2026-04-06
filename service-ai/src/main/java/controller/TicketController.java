package com.mindx.service_ai.controller;

import com.mindx.service_ai.model.Message;
import com.mindx.service_ai.model.Ticket;
import com.mindx.service_ai.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    // POST /tickets  — body: { query, userId }
    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        Long userId = request.get("userId") != null
                ? Long.valueOf(request.get("userId").toString()) : null;

        if (query == null || query.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Query cannot be empty"));

        try {
            return ResponseEntity.ok(ticketService.createTicketWithAI(query, userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /tickets/{id}/messages — body: { query, userId }
    @PostMapping("/{id}/messages")
    public ResponseEntity<?> addMessage(@PathVariable Long id,
                                        @RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        Long userId = request.get("userId") != null
                ? Long.valueOf(request.get("userId").toString()) : null;

        if (query == null || query.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));

        try {
            return ResponseEntity.ok(ticketService.addMessage(id, query, userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /tickets
    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketService.getAllTickets();
    }

    // GET /tickets/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getTicket(@PathVariable Long id) {
        return ticketService.getTicketById(id)
                .map(ticket -> {
                    List<Message> messages = ticketService.getMessagesByTicketId(id);
                    return ResponseEntity.ok(Map.of("ticket", ticket, "messages", messages));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // PATCH /tickets/{id}/status
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(ticketService.updateTicketStatus(id, request.get("status")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /tickets/{id}/admin-message
    @PostMapping("/{id}/admin-message")
    public ResponseEntity<?> adminMessage(@PathVariable Long id,
                                          @RequestBody Map<String, String> request) {
        String msg = request.get("message");
        if (msg == null || msg.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        try {
            return ResponseEntity.ok(ticketService.addAdminMessage(id, msg));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /tickets/analytics
    @GetMapping("/analytics")
    public Map<String, Object> analytics() {
        return ticketService.getAnalytics();
    }
}