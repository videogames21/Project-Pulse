package edu.tcu.cs.projectpulse.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByRecipientIdAndSeenFalseOrderByCreatedAtDesc(Long recipientId);
}
