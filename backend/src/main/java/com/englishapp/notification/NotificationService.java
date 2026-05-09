package com.englishapp.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(UUID userId, NotificationMessage notification) {
        String destination = "/topic/users/" + userId + "/notifications";
        messagingTemplate.convertAndSend(destination, notification);
        log.debug("Sent notification {} to user {}", notification.getType(), userId);
    }

    public void sendToAdmins(NotificationMessage notification) {
        messagingTemplate.convertAndSend("/topic/admin/pipeline", notification);
        log.debug("Sent admin notification {}", notification.getType());
    }
}
