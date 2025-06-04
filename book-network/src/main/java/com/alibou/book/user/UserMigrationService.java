package com.alibou.book.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMigrationService {

    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingUsers() {
        log.info("Starting user migration for Keycloak usernames");

        // Handle users with null usernames
        List<User> usersWithoutUsername = userRepository.findAllByUsernameIsNull();

        if (!usersWithoutUsername.isEmpty()) {
            log.info("Found {} users without username", usersWithoutUsername.size());

            for (User user : usersWithoutUsername) {
                // Set username to email as fallback
                user.setUsername(user.getEmail());
                log.debug("Set username for user {} to {}", user.getId(), user.getUsername());
            }

            userRepository.saveAll(usersWithoutUsername);
            log.info("Completed username migration for null usernames: {} users", usersWithoutUsername.size());
        } else {
            log.info("No users found without username");
        }

        // Handle users with UUID-style usernames
        List<User> usersWithUuidUsername = userRepository.findAll().stream()
                .filter(user -> user.getUsername() != null &&
                        user.getUsername().contains("-") &&
                        user.getUsername().length() > 30)
                .collect(Collectors.toList());

        if (!usersWithUuidUsername.isEmpty()) {
            log.info("Found {} users with UUID usernames", usersWithUuidUsername.size());

            for (User user : usersWithUuidUsername) {
                String friendlyUsername = null;

                // Try to extract username from email
                if (user.getEmail() != null && user.getEmail().contains("@")) {
                    friendlyUsername = user.getEmail().split("@")[0];
                }
                // Or use first name
                else if (user.getFirstname() != null && !user.getFirstname().isEmpty()) {
                    friendlyUsername = user.getFirstname().toLowerCase().replaceAll("\\s+", "");
                }
                // Fallback to a shorter UUID
                else {
                    friendlyUsername = "user_" + user.getUsername().split("-")[0];
                }

                log.info("Changing username for user {} from {} to {}",
                        user.getId(), user.getUsername(), friendlyUsername);
                user.setUsername(friendlyUsername);
            }

            userRepository.saveAll(usersWithUuidUsername);
            log.info("Completed username migration for UUID usernames: {} users", usersWithUuidUsername.size());
        } else {
            log.info("No users found with UUID usernames");
        }
    }
}