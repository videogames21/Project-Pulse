package edu.tcu.cs.projectpulse.notification;

import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void create(Long recipientId, String message) {
        UserEntity recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("User not found with id " + recipientId));
        NotificationEntity n = new NotificationEntity();
        n.setRecipient(recipient);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findUnread(Long recipientId) {
        return notificationRepository
                .findByRecipientIdAndSeenFalseOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(n -> new NotificationResponse(n.getId(), n.getMessage(), n.getCreatedAt()))
                .toList();
    }

    public void markSeen(Long notificationId, Long recipientId) {
        NotificationEntity n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id " + notificationId));
        if (!n.getRecipient().getId().equals(recipientId)) {
            throw new RuntimeException("Not authorized to mark this notification as seen.");
        }
        n.setSeen(true);
    }
}
