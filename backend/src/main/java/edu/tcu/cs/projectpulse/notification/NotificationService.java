package edu.tcu.cs.projectpulse.notification;

import edu.tcu.cs.projectpulse.notification.dto.NotificationResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void create(Long userId, String message) {
        NotificationEntity n = new NotificationEntity();
        n.setUserId(userId);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    public List<NotificationResponse> getUnread(Long userId) {
        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new NotificationResponse(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public void markRead(Long id) {
        NotificationEntity n = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
        n.setRead(true);
        notificationRepository.save(n);
    }
}
