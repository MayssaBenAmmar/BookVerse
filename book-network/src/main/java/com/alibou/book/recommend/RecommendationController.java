package com.alibou.book.recommend;

import com.alibou.book.book.Genre;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Book Recommendations")
@Slf4j
@CrossOrigin(origins = "*") // Configure for your Angular app
public class RecommendationController {

    private final RecommendationService service;

    @GetMapping("/{userId}")
    @Operation(summary = "Get personalized AI recommendations for user")
    public ResponseEntity<List<Recommendation>> getUserRecommendations(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @RequestParam(name = "limit", defaultValue = "10") @Min(1) @Max(50) int limit,
            Authentication connectedUser
    ) {
        log.info("Getting recommendations for user: {}", userId);
        List<Recommendation> recommendations = service.getRecommendationsForUser(userId, limit, connectedUser);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured books with AI scoring")
    public ResponseEntity<List<Recommendation>> getFeaturedBooks(
            @RequestParam(name = "count", defaultValue = "6") @Min(1) @Max(20) int count,
            Authentication connectedUser
    ) {
        List<Recommendation> featured = service.getFeaturedBooks(count, connectedUser);
        return ResponseEntity.ok(featured);
    }

    @GetMapping("/genre/{genre}")
    @Operation(summary = "Get AI recommendations by genre")
    public ResponseEntity<List<Recommendation>> getRecommendationsByGenre(
            @PathVariable Genre genre,
            @RequestParam(name = "count", defaultValue = "6") @Min(1) @Max(20) int count,
            Authentication connectedUser
    ) {
        List<Recommendation> recommendations = service.getRecommendationsByGenre(genre, count, connectedUser);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/similar/{bookId}")
    @Operation(summary = "Get books similar to a specific book")
    public ResponseEntity<List<Recommendation>> getSimilarBooks(
            @PathVariable Long bookId,
            @RequestParam(name = "count", defaultValue = "5") @Min(1) @Max(10) int count,
            Authentication connectedUser
    ) {
        List<Recommendation> similar = service.getSimilarBooks(bookId, count, connectedUser);
        return ResponseEntity.ok(similar);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending books based on recent activity")
    public ResponseEntity<List<Recommendation>> getTrendingBooks(
            @RequestParam(name = "count", defaultValue = "8") @Min(1) @Max(20) int count,
            Authentication connectedUser
    ) {
        List<Recommendation> trending = service.getTrendingBooks(count, connectedUser);
        return ResponseEntity.ok(trending);
    }

    @PostMapping("/feedback")
    @Operation(summary = "Submit recommendation feedback for AI learning")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @RequestBody RecommendationFeedback feedback,
            Authentication connectedUser
    ) {
        Map<String, Object> result = service.processFeedback(feedback, connectedUser);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    @Operation(summary = "Check recommendation service status")
    public ResponseEntity<Map<String, Object>> getRecommendationServiceStatus() {
        boolean available = service.isRecommendationServiceAvailable();
        Map<String, Object> response = new HashMap<>();
        response.put("available", available);
        response.put("status", available ? "UP" : "DOWN");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-book/{bookId}")
    @Operation(summary = "Update book in recommendation system")
    public ResponseEntity<Map<String, Object>> updateBookInRecommendationSystem(
            @PathVariable Long bookId,
            Authentication connectedUser
    ) {
        Map<String, Object> result = service.manuallyUpdateBookInRecommendationService(bookId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh recommendation cache")
    public ResponseEntity<Map<String, Object>> refreshAllRecommendations(
            @RequestParam(defaultValue = "100") int limit
    ) {
        Map<String, Object> result = service.refreshAllRecommendations(limit);
        return ResponseEntity.ok(result);
    }
}