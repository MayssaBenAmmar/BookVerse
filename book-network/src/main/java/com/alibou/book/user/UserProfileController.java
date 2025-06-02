package com.alibou.book.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Get current user profile from JWT token
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getCurrentUserProfile() {
        System.out.println("=== API CALL: GET /profile ===");
        try {
            UserProfileDto profile = userProfileService.getCurrentUserProfile();
            System.out.println("✅ Current profile retrieved successfully");
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            System.err.println("❌ ERROR in getCurrentUserProfile endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get user profile by username with URL decoding
     */
    @GetMapping("/profile/{username}")
    public ResponseEntity<UserProfileDto> getUserProfileByUsername(@PathVariable String username) {
        System.out.println("=== API CALL: GET /profile/" + username + " ===");

        try {
            System.out.println("RAW username received: '" + username + "'");
            System.out.println("Username length: " + username.length());

            if (username == null || username.trim().isEmpty()) {
                System.err.println("❌ ERROR: Username is null or empty");
                return ResponseEntity.badRequest().build();
            }

            // ✅ DECODER l'username pour gérer les caractères spéciaux comme @
            String decodedUsername = null;
            try {
                decodedUsername = URLDecoder.decode(username, StandardCharsets.UTF_8);
                System.out.println("✅ Decoded username: '" + decodedUsername + "' (original: '" + username + "')");
            } catch (Exception e) {
                System.err.println("❌ ERROR decoding username: " + e.getMessage());
                decodedUsername = username;
            }

            System.out.println("Calling service with: " + decodedUsername);
            UserProfileDto profile = userProfileService.getUserProfileByUsername(decodedUsername);
            System.out.println("✅ Profile retrieved successfully for: " + decodedUsername);
            return ResponseEntity.ok(profile);

        } catch (UserNotFoundException e) {
            System.err.println("❌ User not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("❌ UNEXPECTED ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}