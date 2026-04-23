package edu.tcu.cs.projectpulse.notification;

import edu.tcu.cs.projectpulse.system.Result;
import edu.tcu.cs.projectpulse.system.StatusCode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result getUnread(@RequestParam Long userId) {
        return new Result(true, StatusCode.SUCCESS, "Notifications fetched",
                notificationService.findUnread(userId));
    }

    @PatchMapping("/{id}/seen")
    public Result markSeen(@PathVariable Long id, @RequestParam Long userId) {
        notificationService.markSeen(id, userId);
        return new Result(true, StatusCode.SUCCESS, "Notification marked as seen");
    }
}
