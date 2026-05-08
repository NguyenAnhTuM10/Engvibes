package com.englishapp.stats;

import com.englishapp.stats.dto.EventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventTrackingService {

    private final UserEventRepository userEventRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void trackBatch(UUID userId, List<EventDTO> events) {
        events.forEach(e -> trackEvent(userId, e.getType(), e.getPayload()));
    }

    public void trackEvent(UUID userId, String eventType, Map<String, Object> payload) {
        try {
            String payloadJson = payload != null ? objectMapper.writeValueAsString(payload) : "{}";
            userEventRepository.save(UserEvent.builder()
                    .userId(userId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to track event {} for user {}: {}", eventType, userId, e.getMessage());
        }
    }
}
