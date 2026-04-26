package edu.tcu.cs.projectpulse.notification;

import edu.tcu.cs.projectpulse.notification.dto.NotificationResponse;
import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/users/{userId}/unread")
    public Result getUnread(@PathVariable Long userId) {
        List<NotificationResponse> notifications = notificationService.getUnread(userId);
        return new Result(true, StatusCode.SUCCESS, "Notifications retrieved", notifications);
    }

    @PutMapping("/{id}/read")
    public Result markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return new Result(true, StatusCode.SUCCESS, "Notification marked as read");
    }
}
