package com.highlight.nuzip.service;

import com.highlight.nuzip.model.Notification;
import com.highlight.nuzip.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // 새로운 알림 생성
    public void createNotification(String userId, String message, Long articleId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setArticleId(articleId);

        notificationRepository.save(notification);
    }

    // 알림 읽음 처리
    public void markAsRead(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));

        // 본인 알림인지 체크
        if(!notification.getUserId().equals(userId)){
            throw new RuntimeException("다른 사람의 알림을 수정할 수 없습니다.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        if (unread.isEmpty()) {
            return;
        }
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // 안 읽은 알림 조회(컨트롤러에서 호출)
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }
}
