package com.alibou.book.user;

import com.alibou.book.user.User;

public interface UserService {
    // Get the current authenticated user
    User getCurrentUser();

    // Get a user by ID - changed from String to Long
    User getUserById(Long id);

    // Get a user by username
    User getUserByUsername(String username);
}