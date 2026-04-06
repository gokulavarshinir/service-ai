package com.mindx.service_ai.service;

import com.mindx.service_ai.model.*;
import com.mindx.service_ai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Service
public class TicketService {

    @Autowired private TicketRepository ticketRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─── Extract order ID like ORD-1001 from message ─────────────────────
    private Optional<String> extractOrderId(String query) {
        Matcher m = Pattern.compile("ORD-?\\d+", Pattern.CASE_INSENSITIVE).matcher(query);
        return m.find() ? Optional.of(m.group().toUpperCase().replace("ORD", "ORD-").replaceAll("-{2,}", "-")) : Optional.empty();
    }

    // ─── Smart AI Response ────────────────────────────────────────────────
    private String generateAIResponse(String query, boolean needsHuman, Long userId) {
        String q = query.toLowerCase();

        // 1. Check if user mentioned an order ID — fetch real data
        Optional<String> maybeOrderId = extractOrderId(query);
        if (maybeOrderId.isPresent()) {
            Optional<Order> orderOpt = orderRepository.findByOrderId(maybeOrderId.get());
            if (orderOpt.isPresent()) {
                Order o = orderOpt.get();
                String eta = o.getEstimatedDelivery() != null ? o.getEstimatedDelivery().format(FMT) : "TBD";
                String tracking = o.getTrackingId() != null ? o.getTrackingId() : "Not yet assigned";
                String courier = o.getCourierName() != null ? o.getCourierName() : "TBD";

                return switch (o.getStatus()) {
                    case "PLACED"     -> "✅ Order " + o.getOrderId() + " for *" + o.getProductName() + "* has been placed successfully! It will be dispatched soon. Estimated delivery: " + eta + ".";
                    case "DISPATCHED" -> "📦 Order " + o.getOrderId() + " (*" + o.getProductName() + "*) has been dispatched! Courier: " + courier + " | Tracking ID: " + tracking + ". Estimated delivery: " + eta + ".";
                    case "SHIPPED"    -> "🚚 Order " + o.getOrderId() + " (*" + o.getProductName() + "*) is on its way! Courier: " + courier + " | Tracking: " + tracking + ". Expected delivery: " + eta + ".";
                    case "DELIVERED"  -> "✅ Order " + o.getOrderId() + " (*" + o.getProductName() + "*) was delivered on " + o.getEstimatedDelivery().format(FMT) + ". Hope you're enjoying it! Need help with anything else?";
                    case "CANCELLED"  -> "❌ Order " + o.getOrderId() + " (*" + o.getProductName() + "*) was cancelled. If you'd like to reorder or need a refund, please let me know.";
                    default           -> "Order " + o.getOrderId() + " status: " + o.getStatus() + ". Estimated delivery: " + eta + ".";
                };
            } else {
                return "I couldn't find order " + maybeOrderId.get() + " in our system. Please double-check the order ID and try again, or contact support.";
            }
        }

        // 2. User asks about "my orders" — fetch from DB
        if ((q.contains("my order") || q.contains("all order") || q.contains("list order")) && userId != null) {
            List<Order> orders = orderRepository.findByUserId(userId);
            if (orders.isEmpty())
                return "You don't have any orders yet. Start shopping at mindx.com!";
            StringBuilder sb = new StringBuilder("Here are your recent orders:\n");
            for (Order o : orders) {
                sb.append("• ").append(o.getOrderId()).append(" — ").append(o.getProductName())
                        .append(" (").append(o.getStatus()).append(")\n");
            }
            sb.append("\nShare an Order ID for detailed tracking info.");
            return sb.toString();
        }

        // 3. Escalated topics — AI gives acknowledgment, admin handles
        if (needsHuman) {
            if (q.contains("refund") || q.contains("money back"))
                return "I've raised a high-priority refund request for you. A human agent will contact you within 2 hours. Your ticket ID has been noted.";
            if (q.contains("payment") || q.contains("charge") || q.contains("bill"))
                return "I see you have a payment concern. This has been escalated to our billing team — they'll reach out shortly with a resolution.";
            if (q.contains("complaint") || q.contains("angry") || q.contains("terrible"))
                return "I sincerely apologize for your experience. A senior agent will personally contact you within 2 hours.";
            if (q.contains("fraud") || q.contains("legal"))
                return "This has been flagged as urgent and escalated to our compliance team. You'll be contacted within 1 hour.";
            return "Your concern has been escalated to a human agent who will contact you shortly. We apologize for any inconvenience.";
        }

        // 4. Normal keyword responses
        if (q.contains("where") || q.contains("track") || q.contains("status"))
            return "Please share your Order ID (e.g. ORD-1001) and I'll fetch the live status for you right away!";
        if (q.contains("cancel"))
            return "To cancel your order, please share your Order ID. If it hasn't shipped yet, we can cancel it immediately.";
        if (q.contains("delivery") || q.contains("shipping") || q.contains("dispatch"))
            return "Shipping usually takes 3–5 business days. Share your Order ID for exact delivery estimates.";
        if (q.contains("broken") || q.contains("damage") || q.contains("defect") || q.contains("wrong item"))
            return "Sorry to hear that! Please share your Order ID and describe the issue — we'll arrange a replacement or refund promptly.";
        if (q.contains("return"))
            return "We accept returns within 7 days of delivery. Share your Order ID to start the return process.";
        if (q.contains("account") || q.contains("login") || q.contains("password"))
            return "For account issues, try resetting your password at mindx.com/reset. If it persists, share your registered email and I'll escalate.";
        if (q.contains("discount") || q.contains("coupon") || q.contains("offer") || q.contains("promo"))
            return "You can apply discount codes at checkout. Current offers are live at mindx.com/offers.";
        if (q.contains("hello") || q.contains("hi") || q.contains("hey"))
            return "Hello! Welcome to MindX Support 👋 How can I help you today? Ask me about orders, shipping, returns, or account issues.";
        if (q.contains("thank"))
            return "You're welcome! Is there anything else I can help you with?";

        return "Thanks for reaching out! Could you share more details? If this is about an order, please mention your Order ID (e.g. ORD-1001).";
    }

    // ─── Escalation check ─────────────────────────────────────────────────
    private boolean isEscalated(String query) {
        String q = query.toLowerCase();
        return q.contains("refund") || q.contains("complaint") || q.contains("angry") ||
                q.contains("payment") || q.contains("fraud") || q.contains("legal") ||
                q.contains("terrible") || q.contains("money back");
    }

    // ─── Create Ticket ────────────────────────────────────────────────────
    public Map<String, Object> createTicketWithAI(String query, Long userId) {
        if (query == null || query.trim().isEmpty())
            throw new IllegalArgumentException("Query cannot be empty");

        boolean escalated = isEscalated(query);

        Ticket ticket = new Ticket();
        ticket.setQuery(query.trim());
        ticket.setStatus(escalated ? "NEEDS_HUMAN" : "OPEN");
        ticket.setUserId(userId);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        saveMessage(ticket.getId(), "USER", query.trim());

        String aiResponse = generateAIResponse(query, escalated, userId);
        saveMessage(ticket.getId(), "AI", aiResponse);

        // Fetch user name for response
        String userName = userId != null
                ? userRepository.findById(userId).map(User::getName).orElse("Customer")
                : "Customer";

        Map<String, Object> result = new HashMap<>();
        result.put("ticketId", ticket.getId());
        result.put("status", ticket.getStatus());
        result.put("aiResponse", aiResponse);
        result.put("escalated", escalated);
        result.put("userName", userName);
        return result;
    }

    // ─── Follow-up message ────────────────────────────────────────────────
    public Map<String, Object> addMessage(Long ticketId, String query, Long userId) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        saveMessage(ticketId, "USER", query.trim());

        boolean escalated = isEscalated(query);
        String aiResponse = generateAIResponse(query, escalated, userId);
        saveMessage(ticketId, "AI", aiResponse);

        Map<String, Object> result = new HashMap<>();
        result.put("ticketId", ticketId);
        result.put("aiResponse", aiResponse);
        result.put("escalated", escalated);
        return result;
    }

    // ─── Admin reply ──────────────────────────────────────────────────────
    public Message addAdminMessage(Long ticketId, String text) {
        return saveMessage(ticketId, "ADMIN", text.trim());
    }

    // ─── Status update ────────────────────────────────────────────────────
    public Ticket updateTicketStatus(Long id, String status) {
        List<String> valid = List.of("OPEN", "RESOLVED", "NEEDS_HUMAN");
        if (!valid.contains(status)) throw new IllegalArgumentException("Invalid status: " + status);
        Ticket t = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        t.setStatus(status);
        return ticketRepository.save(t);
    }

    // ─── Analytics ────────────────────────────────────────────────────────
    public Map<String, Object> getAnalytics() {
        List<Ticket> all = ticketRepository.findAll();
        Map<String, Object> a = new HashMap<>();
        a.put("total", all.size());
        a.put("open", all.stream().filter(t -> "OPEN".equals(t.getStatus())).count());
        a.put("resolved", all.stream().filter(t -> "RESOLVED".equals(t.getStatus())).count());
        a.put("needsHuman", all.stream().filter(t -> "NEEDS_HUMAN".equals(t.getStatus())).count());
        return a;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────
    private Message saveMessage(Long ticketId, String sender, String text) {
        Message m = new Message();
        m.setTicketId(ticketId);
        m.setSender(sender);
        m.setMessage(text);
        m.setTimestamp(LocalDateTime.now());
        return messageRepository.save(m);
    }

    public List<Ticket> getAllTickets() { return ticketRepository.findAll(); }
    public Optional<Ticket> getTicketById(Long id) { return ticketRepository.findById(id); }
    public List<Message> getMessagesByTicketId(Long id) { return messageRepository.findByTicketId(id); }
}