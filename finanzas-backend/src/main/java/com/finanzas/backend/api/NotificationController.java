package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.NotificationDtos;
import com.finanzas.backend.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationDtos.NotificationResponse> list(Authentication authentication,
                                                            @PathVariable UUID workspaceId,
                                                            @RequestParam(defaultValue = "false") boolean includeRead) {
        return notifications.list(CurrentUser.id(authentication), workspaceId, includeRead);
    }

    @PostMapping("/refresh")
    public List<NotificationDtos.NotificationResponse> refresh(Authentication authentication, @PathVariable UUID workspaceId) {
        return notifications.refresh(CurrentUser.id(authentication), workspaceId);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationDtos.NotificationResponse markRead(Authentication authentication,
                                                         @PathVariable UUID workspaceId,
                                                         @PathVariable UUID notificationId) {
        return notifications.markRead(CurrentUser.id(authentication), workspaceId, notificationId);
    }
}
