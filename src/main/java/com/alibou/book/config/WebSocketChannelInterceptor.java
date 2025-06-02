package com.alibou.book.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketChannelInterceptor implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Get username from headers
                    String username = accessor.getFirstNativeHeader("username");
                    log.info("CONNECT: username from header: {}", username);

                    if (username != null) {
                        Principal principal = () -> username;
                        accessor.setUser(principal);
                        log.info("Set user principal: {}", username);
                    }
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    log.info("SUBSCRIBE: destination={}, user={}",
                            accessor.getDestination(),
                            accessor.getUser() != null ? accessor.getUser().getName() : "null");
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    log.info("SEND: destination={}, user={}",
                            accessor.getDestination(),
                            accessor.getUser() != null ? accessor.getUser().getName() : "null");
                }

                return message;
            }
        });
    }
}