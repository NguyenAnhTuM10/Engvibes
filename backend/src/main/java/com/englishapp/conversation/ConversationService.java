package com.englishapp.conversation;

import com.englishapp.conversation.dto.ScenarioResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Realtime conversation chạy qua WS proxy (ConversationProxyWebSocketHandler) +
 * persist qua ConversationRealtimeService. Service này chỉ còn cung cấp danh sách
 * scenario cho UI. Logic turn-based REST cũ (luồng B) đã gỡ bỏ.
 */
@Service
public class ConversationService {

    public List<ScenarioResponse> getScenarios() {
        return Arrays.stream(ConversationScenario.values())
                .map(s -> new ScenarioResponse(
                        s.name(), s.displayName, s.description, s.aiRole, s.userGoal, s.openingLine))
                .toList();
    }
}
