package com.alibou.book.config;

import java.security.Principal;

public class CustomUserPrincipal implements Principal {
    private final String name;

    public CustomUserPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}