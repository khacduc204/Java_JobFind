package com.example.JobFinder.controller;

import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.UserRepository;
import com.example.JobFinder.service.NotificationService;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/poll")
    public ResponseEntity<Map<String, Object>> poll(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of(
                "unread", 0,
                "items", List.of()
            ));
        }

        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of(
                "unread", 0,
                "items", List.of()
            ));
        }

        long unread = notificationService.countUnread(user.getId());
        List<NotificationService.NotificationView> items = notificationService.getRecentNotifications(user.getId(), 5);

        Map<String, Object> payload = new HashMap<>();
        payload.put("unread", unread);
        payload.put("items", items);

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markAllRead(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("unread", 0));
        }

        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of("unread", 0));
        }

        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("unread", 0));
    }
}
