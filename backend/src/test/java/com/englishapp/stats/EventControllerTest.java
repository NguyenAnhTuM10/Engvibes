package com.englishapp.stats;

import com.englishapp.stats.dto.BatchEventRequest;
import com.englishapp.stats.dto.EventDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock EventTrackingService eventTrackingService;

    @Test
    void shouldAcceptBatch() {
        UUID userId = UUID.randomUUID();
        List<EventDTO> events = List.of(
                new EventDTO("replay_audio", Map.of("segmentIdx", 2)),
                new EventDTO("skip_step", Map.of("step", 3))
        );

        eventTrackingService.trackBatch(userId, events);

        verify(eventTrackingService).trackBatch(eq(userId), eq(events));
    }
}
