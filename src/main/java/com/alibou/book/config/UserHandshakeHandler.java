package com.alibou.book.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String username = null;

        // Try to get username from query parameter
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            username = servletRequest.getServletRequest().getParameter("username");
        }

        // If we have a username, create a principal
        if (username != null) {
            log.info("Creating principal for user: {}", username);
            final String finalUsername = username;
            return () -> finalUsername;
        }

        // Fallback to default behavior
        return super.determineUser(request, wsHandler, attributes);
    }
}