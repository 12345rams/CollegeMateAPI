package com.collegemate.controller;

import com.collegemate.model.ChatMessage;
import com.collegemate.repository.ChatMessageRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate, ChatMessageRepository chatMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    // REST Endpoint to fetch historical chat messages for a session
    @GetMapping("/api/chat/history/{sessionId}")
    public List<ChatMessage> getChatHistory(@PathVariable String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    // WebSocket Message Mapping for Real-time Chat
    @MessageMapping("/chat/{sessionId}")
    public void handleChatMessage(@DestinationVariable String sessionId, @Payload ChatMessage message) {
        message.setSessionId(sessionId);
        message.setTimestamp(Instant.now());
        
        // Save to Database
        ChatMessage saved = chatMessageRepository.save(message);
        
        // Broadcast to all subscribers of this session topic
        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, saved);
    }

    // WebSocket Message Mapping for WebRTC Signaling (relaying sdp/candidates)
    @MessageMapping("/signal/{sessionId}")
    public void handleWebRTCSignaling(@DestinationVariable String sessionId, @Payload Map<String, Object> signal) {
        // Relay the signal (offer/answer/candidate) as-is to the other party subscribed to this session signal topic
        messagingTemplate.convertAndSend((String) ("/topic/signal/" + sessionId), (Object) signal);
    }
}
