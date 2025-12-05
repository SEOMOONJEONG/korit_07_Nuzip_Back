package com.highlight.nuzip.controller;

import com.highlight.nuzip.model.Notification;
import com.highlight.nuzip.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 로그인한 사용자의 안 읽은 알림 조회
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Principal principal) {
        log.debug("[NotificationController] principal={}", principal);
        if (principal == null) {
            log.warn("[NotificationController] principal null → 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.getUnreadNotifications(principal.getName()));
    }

    // 알림 읽음 처리
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, Principal principal) {
        log.debug("[NotificationController] markAsRead principal={}", principal);
        if (principal == null) {
            log.warn("[NotificationController] markAsRead principal null → 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.markAsRead(notificationId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        log.debug("[NotificationController] markAllAsRead principal={}", principal);
        if (principal == null) {
            log.warn("[NotificationController] markAllAsRead principal null → 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        notificationService.markAllAsRead(principal.getName());
        return ResponseEntity.ok().build();
    }

}
