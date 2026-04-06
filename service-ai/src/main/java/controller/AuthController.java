package com.mindx.service_ai.controller;

import com.mindx.service_ai.model.User;
import com.mindx.service_ai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // POST /auth/signup
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String name = body.get("name");

        if (email == null || password == null || name == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email and password are required"));

        if (userRepository.existsByEmail(email))
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));

        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(password);
        user.setName(name.trim());
        user.setPhone(body.getOrDefault("phone", ""));
        user.setAddress(body.getOrDefault("address", ""));
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Signup successful",
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail()
        ));
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password required"));

        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(user -> {
                    if (!user.getPassword().equals(password))
                        return ResponseEntity.status(401).<Object>body(Map.of("error", "Invalid password"));

                    return ResponseEntity.ok((Object) Map.of(
                            "message", "Login successful",
                            "userId", user.getId(),
                            "name", user.getName(),
                            "email", user.getEmail(),
                            "phone", user.getPhone() != null ? user.getPhone() : "",
                            "address", user.getAddress() != null ? user.getAddress() : ""
                    ));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "No account found with this email")));
    }

    // GET /auth/user/{id}
    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok((Object) Map.of(
                        "userId", u.getId(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "phone", u.getPhone() != null ? u.getPhone() : "",
                        "address", u.getAddress() != null ? u.getAddress() : "",
                        "createdAt", u.getCreatedAt().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}