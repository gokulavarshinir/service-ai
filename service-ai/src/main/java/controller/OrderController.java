package com.mindx.service_ai.controller;

import com.mindx.service_ai.model.Order;
import com.mindx.service_ai.repository.OrderRepository;
import com.mindx.service_ai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    // GET /orders/user/{userId} — all orders for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUser(@PathVariable Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    // GET /orders/{orderId} — by order string id like ORD-1001
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        return orderRepository.findByOrderId(orderId.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /orders — create order (admin use / seed)
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Order order) {
        if (order.getOrderId() == null || order.getUserId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "orderId and userId required"));
        order.setOrderDate(LocalDateTime.now());
        return ResponseEntity.ok(orderRepository.save(order));
    }

    // PATCH /orders/{orderId}/status — update order status
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String orderId,
                                          @RequestBody Map<String, String> body) {
        return orderRepository.findByOrderId(orderId.toUpperCase())
                .map(order -> {
                    order.setStatus(body.get("status"));
                    return ResponseEntity.ok(orderRepository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /orders/seed/{userId} — seed sample orders for a user (for demo/testing)
    @PostMapping("/seed/{userId}")
    public ResponseEntity<?> seedOrders(@PathVariable Long userId) {
        if (!userRepository.existsById(userId))
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));

        List<Order> sample = List.of(
                makeOrder("ORD-1001", userId, "Wireless Earbuds", "SHIPPED",
                        "TRK-887123", "BlueDart", 1299.0,
                        LocalDateTime.now().minusDays(3), LocalDateTime.now().plusDays(2)),
                makeOrder("ORD-1002", userId, "Running Shoes", "DELIVERED",
                        "TRK-992341", "FedEx", 2499.0,
                        LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(3)),
                makeOrder("ORD-1003", userId, "USB-C Hub", "DISPATCHED",
                        "TRK-112233", "Delhivery", 799.0,
                        LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(4)),
                makeOrder("ORD-1004", userId, "Desk Lamp", "PLACED",
                        null, null, 599.0,
                        LocalDateTime.now(), LocalDateTime.now().plusDays(6))
        );

        // Only save orders that don't already exist
        for (Order o : sample) {
            if (orderRepository.findByOrderId(o.getOrderId()).isEmpty()) {
                orderRepository.save(o);
            }
        }
        return ResponseEntity.ok(Map.of("message", "Sample orders seeded", "count", sample.size()));
    }

    private Order makeOrder(String orderId, Long userId, String product, String status,
                            String tracking, String courier, Double amount,
                            LocalDateTime orderDate, LocalDateTime eta) {
        Order o = new Order();
        o.setOrderId(orderId);
        o.setUserId(userId);
        o.setProductName(product);
        o.setStatus(status);
        o.setTrackingId(tracking);
        o.setCourierName(courier);
        o.setAmount(amount);
        o.setOrderDate(orderDate);
        o.setEstimatedDelivery(eta);
        return o;
    }
}