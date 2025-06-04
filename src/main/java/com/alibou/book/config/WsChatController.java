package com.alibou.book.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WsChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    // Store online users
    private final Map<String, UserInfo> onlineUsers = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public WsChatMessage sendMessage(@Payload WsChatMessage chatMessage) {
        log.info("=== PUBLIC MESSAGE ===");
        log.info("Sender: {}", chatMessage.getSender());
        log.info("Content: {}", chatMessage.getContent());
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public WsChatMessage addUser(@Payload WsChatMessage chatMessage,
                                 SimpMessageHeaderAccessor headerAccessor,
                                 Principal principal) {
        String username = chatMessage.getSender();
        String sessionId = headerAccessor.getSessionId();

        log.info("=== USER JOINING ===");
        log.info("Username: {}", username);
        log.info("SessionId: {}", sessionId);
        log.info("Principal: {}", principal);

        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", username);
        sessionToUsername.put(sessionId, username);

        // Add user to online users
        UserInfo userInfo = UserInfo.builder()
                .username(username)
                .status("Active now")
                .build();
        onlineUsers.put(username, userInfo);

        log.info("Current online users: {}", onlineUsers.keySet());

        // Log current user registry
        log.info("=== USER REGISTRY ===");
        log.info("Total users in registry: {}", userRegistry.getUserCount());
        userRegistry.getUsers().forEach(user -> {
            log.info("User: {}, Sessions: {}", user.getName(), user.getSessions().size());
        });

        // Broadcast updated online users list
        broadcastOnlineUsers();

        return chatMessage;
    }

    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload WsChatMessage chatMessage,
                                   SimpMessageHeaderAccessor headerAccessor,
                                   Principal principal) {
        log.info("=== PRIVATE MESSAGE ATTEMPT ===");
        log.info("Principal: {}", principal);
        log.info("Principal Name: {}", principal != null ? principal.getName() : "null");
        log.info("Sender: {}", chatMessage.getSender());
        log.info("Recipient: {}", chatMessage.getRecipient());
        log.info("Content: {}", chatMessage.getContent());

        // Add timestamp if not present
        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(new Date().toString());
        }

        String recipient = chatMessage.getRecipient();

        // Send to recipient
        log.info("Sending to recipient: {}", recipient);
        messagingTemplate.convertAndSendToUser(
                recipient,
                "/queue/messages",
                chatMessage,
                createHeaders(headerAccessor.getSessionId())
        );

        // Send to sender
        log.info("Sending to sender: {}", chatMessage.getSender());
        messagingTemplate.convertAndSendToUser(
                chatMessage.getSender(),
                "/queue/messages",
                chatMessage,
                createHeaders(headerAccessor.getSessionId())
        );
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }

    public void handleUserDisconnect(String username) {
        log.info("=== USER DISCONNECT ===");
        log.info("Username: {}", username);
        onlineUsers.remove(username);
        broadcastOnlineUsers();
    }

    private void broadcastOnlineUsers() {
        List<UserInfo> usersList = new ArrayList<>(onlineUsers.values());
        log.info("Broadcasting online users: {}", usersList);
        messagingTemplate.convertAndSend("/topic/users", usersList);
    }
}