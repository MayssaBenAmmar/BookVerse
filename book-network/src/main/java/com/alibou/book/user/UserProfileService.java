package com.alibou.book.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;

    /**
     * Get user profile by username OR email - GÈRE LES DOUBLONS
     */
    public UserProfileDto getUserProfileByUsername(String usernameOrEmail) {
        System.out.println("=== SERVICE: getUserProfileByUsername ===");
        System.out.println("Input parameter: '" + usernameOrEmail + "'");

        try {
            if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
                System.err.println("ERROR: Username/email is null or empty");
                throw new UserNotFoundException("Username/email cannot be null or empty");
            }

            User foundUser = null;

            // ✅ Chercher par username
            System.out.println("Searching by username...");
            try {
                List<User> usersByUsername = userRepository.findAllByUsername(usernameOrEmail);
                System.out.println("Found " + usersByUsername.size() + " users by username");

                if (!usersByUsername.isEmpty()) {
                    foundUser = usersByUsername.get(0);
                    System.out.println("✅ Using user: " + foundUser.getUsername());
                }
            } catch (Exception e) {
                System.err.println("ERROR searching by username: " + e.getMessage());
                e.printStackTrace();
            }

            // ✅ Chercher par email si pas trouvé
            if (foundUser == null && usernameOrEmail.contains("@")) {
                System.out.println("Searching by email...");
                try {
                    List<User> usersByEmail = userRepository.findAllByEmail(usernameOrEmail);
                    System.out.println("Found " + usersByEmail.size() + " users by email");

                    if (!usersByEmail.isEmpty()) {
                        foundUser = usersByEmail.get(0);
                        System.out.println("✅ Using user: " + foundUser.getEmail());
                    }
                } catch (Exception e) {
                    System.err.println("ERROR searching by email: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (foundUser == null) {
                System.err.println("❌ No user found for: " + usernameOrEmail);
                throw new UserNotFoundException("User not found: " + usernameOrEmail);
            }

            // ✅ CONSTRUCTION DU DTO
            System.out.println("Building DTO...");
            UserProfileDto result = UserProfileDto.builder()
                    .username(foundUser.getUsername() != null ? foundUser.getUsername() : "")
                    .firstName(foundUser.getFirstname() != null ? foundUser.getFirstname() : "")
                    .lastName(foundUser.getLastname() != null ? foundUser.getLastname() : "")
                    .email(foundUser.getEmail() != null ? foundUser.getEmail() : "")
                    .bio(foundUser.getBio() != null ? foundUser.getBio() : "")
                    .profilePictureUrl(foundUser.getProfileImageUrl() != null ? foundUser.getProfileImageUrl() : "")
                    .id(foundUser.getId() != null ? foundUser.getId().toString() : "")
                    .booksCount(foundUser.getBooks() != null ? foundUser.getBooks().size() : 0)
                    .build();

            System.out.println("✅ DTO created successfully");
            System.out.println("Result - Username: " + result.getUsername() + ", Email: " + result.getEmail());
            return result;

        } catch (UserNotFoundException e) {
            System.err.println("UserNotFoundException: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("UNEXPECTED ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Service error: " + e.getMessage(), e);
        }
    }

    /**
     * Get current user profile from JWT token
     */
    public UserProfileDto getCurrentUserProfile() {
        System.out.println("=== DÉBUT getCurrentUserProfile ===");

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                throw new RuntimeException("No authentication found");
            }

            if (!(authentication.getPrincipal() instanceof Jwt)) {
                throw new RuntimeException("Invalid authentication type: " + authentication.getPrincipal().getClass());
            }

            Jwt jwt = (Jwt) authentication.getPrincipal();
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");

            System.out.println("JWT claims - username: " + username + ", email: " + email);

            User user = findOrCreateUser(username, email, firstName, lastName);

            UserProfileDto result = UserProfileDto.builder()
                    .username(user.getUsername() != null ? user.getUsername() : "")
                    .firstName(user.getFirstname() != null ? user.getFirstname() : "")
                    .lastName(user.getLastname() != null ? user.getLastname() : "")
                    .email(user.getEmail() != null ? user.getEmail() : "")
                    .bio(user.getBio() != null ? user.getBio() : "")
                    .profilePictureUrl(user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "")
                    .id(user.getId() != null ? user.getId().toString() : "")
                    .booksCount(user.getBooks() != null ? user.getBooks().size() : 0)
                    .build();

            System.out.println("✅ Current user profile created successfully");
            return result;

        } catch (Exception e) {
            System.err.println("ERROR in getCurrentUserProfile: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error getting current user profile: " + e.getMessage(), e);
        }
    }

    private User findOrCreateUser(String username, String email, String firstName, String lastName) {
        System.out.println("=== findOrCreateUser - username: " + username + ", email: " + email + " ===");

        try {
            // ✅ Chercher par username
            if (username != null && !username.trim().isEmpty()) {
                List<User> existingUsers = userRepository.findAllByUsername(username);
                if (!existingUsers.isEmpty()) {
                    System.out.println("✅ Found existing user by username");
                    return existingUsers.get(0);
                }
            }

            // ✅ Chercher par email
            if (email != null && !email.trim().isEmpty()) {
                List<User> existingUsers = userRepository.findAllByEmail(email);
                if (!existingUsers.isEmpty()) {
                    User user = existingUsers.get(0);
                    user.setUsername(username);
                    System.out.println("✅ Found existing user by email, updating username");
                    return userRepository.save(user);
                }
            }

            // Créer nouvel utilisateur
            User newUser = User.builder()
                    .username(username != null ? username : "user")
                    .email(email != null ? email : "")
                    .firstname(firstName != null ? firstName : "User")
                    .lastname(lastName != null ? lastName : "")
                    .enabled(true)
                    .accountLocked(false)
                    .build();

            System.out.println("✅ Creating new user: " + username);
            return userRepository.save(newUser);

        } catch (Exception e) {
            System.err.println("ERROR in findOrCreateUser: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error finding/creating user", e);
        }
    }
}