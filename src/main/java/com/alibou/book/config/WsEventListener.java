package com.alibou.book.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WsEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final WsChatController chatController;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("=== NEW CONNECTION ===");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("SessionId: {}", headerAccessor.getSessionId());
        log.info("User: {}", event.getUser());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        log.info("=== DISCONNECT EVENT ===");
        log.info("SessionId: {}", headerAccessor.getSessionId());
        log.info("Username: {}", username);
        log.info("User Principal: {}", event.getUser());

        if (username != null) {
            log.info("User Disconnected: {}", username);

            WsChatMessage chatMessage = WsChatMessage.builder()
                    .type("LEAVE")
                    .sender(username)
                    .build();

            messagingTemplate.convertAndSend("/topic/public", chatMessage);
            chatController.handleUserDisconnect(username);
        }
    }
}