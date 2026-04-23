package edu.tcu.cs.projectpulse.notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String message, LocalDateTime createdAt) {}
