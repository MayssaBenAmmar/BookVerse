package com.alibou.book.config;

import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationResolver;

public class CustomUserDestinationResolver extends DefaultUserDestinationResolver {

    public CustomUserDestinationResolver(SimpUserRegistry userRegistry) {
        super(userRegistry);
    }

    @Override
    public String toString() {
        return "CustomUserDestinationResolver";
    }
}