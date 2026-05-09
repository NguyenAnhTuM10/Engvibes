package com.englishapp.notification;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class NotificationMessage {
    NotificationType type;
    String title;
    String message;
    Map<String, Object> data;
    @Builder.Default
    Instant timestamp = Instant.now();
}
