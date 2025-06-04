package com.alibou.book.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getCurrentUser() {
        // Get the authenticated user's email/username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseGet(() -> {
                        // Fallback to mock user if not found
                        User mockUser = new User();
                        mockUser.setId(123L);
                        mockUser.setUsername(username);
                        return mockUser;
                    });
        }

        // Default mock user if no authentication found
        User mockUser = new User();
        mockUser.setId(123L);
        mockUser.setUsername("testuser");
        return mockUser;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(id);
                    user.setUsername("user_" + id);
                    return user;
                });
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(456L);
                    user.setUsername(username);
                    return user;
                });
    }
}