package com.alibou.book.recommend;

import com.alibou.book.book.Book;
import com.alibou.book.book.Genre;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final BookRecommendationHelper helper;
    private final RecommendationAIEngine aiEngine;

    @Cacheable(value = "userRecommendations", key = "#userId + '_' + #limit")
    public List<Recommendation> getRecommendationsForUser(Long userId, int limit, Authentication connectedUser) {
        log.info("Generating recommendations for user: {} with limit: {}", userId, limit);

        // Get user's reading history
        List<Book> userBooks = helper.getBooksReadByUser(userId);

        if (userBooks.isEmpty()) {
            log.info("No reading history found for user: {}, returning popular books", userId);
            return getHighestRatedBooks(limit);
        }

        // AI-enhanced recommendation logic
        UserProfile userProfile = aiEngine.buildUserProfile(userId, userBooks);
        List<Book> candidateBooks = helper.getCandidateBooks(userProfile, userBooks, limit * 3);

        return aiEngine.scoreAndRankBooks(candidateBooks, userProfile, limit);
    }

    @Cacheable(value = "featuredBooks", key = "#count")
    public List<Recommendation> getFeaturedBooks(int count, Authentication connectedUser) {
        log.info("Getting {} featured books", count);
        return getHighestRatedBooks(count);
    }

    @Cacheable(value = "genreRecommendations", key = "#genre + '_' + #count")
    public List<Recommendation> getRecommendationsByGenre(Genre genre, int count, Authentication connectedUser) {
        log.info("Getting {} recommendations for genre: {}", count, genre);

        List<Book> genreBooks = helper.getBooksByGenre(genre);
        return aiEngine.rankBooksByPopularityAndQuality(genreBooks, count);
    }

    @Cacheable(value = "similarBooks", key = "#bookId + '_' + #count")
    public List<Recommendation> getSimilarBooks(Long bookId, int count, Authentication connectedUser) {
        log.info("Getting {} similar books for book ID: {}", count, bookId);

        Optional<Book> targetBook = helper.getBookById(bookId);
        if (targetBook.isEmpty()) {
            return Collections.emptyList();
        }

        return aiEngine.findSimilarBooks(targetBook.get(), count);
    }

    @Cacheable(value = "trendingBooks", key = "#count")
    public List<Recommendation> getTrendingBooks(int count, Authentication connectedUser) {
        log.info("Getting {} trending books", count);

        LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<Book> recentlyActiveBooks = helper.getRecentlyActiveBooks(weekAgo);

        return aiEngine.calculateTrendingScore(recentlyActiveBooks, count);
    }

    public Map<String, Object> processFeedback(RecommendationFeedback feedback, Authentication connectedUser) {
        log.info("Processing recommendation feedback: {}", feedback);

        // Store feedback for AI learning
        aiEngine.processFeedback(feedback);

        // Invalidate relevant caches
        invalidateUserCache(feedback.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Feedback processed successfully");
        return response;
    }

    @Cacheable(value = "highestRatedBooks", key = "#count")
    public List<Recommendation> getHighestRatedBooks(int count) {
        return helper.getTopRatedBooks(count).stream()
                .map(this::toRecommendation)
                .collect(Collectors.toList());
    }

    public Recommendation toRecommendation(Book book) {
        return toRecommendation(book, book.getRate(), "High rated book");
    }

    public Recommendation toRecommendation(Book book, double score, String reason) {
        return Recommendation.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .authorName(book.getAuthorName())
                .cover(convertCoverToBase64(book.getBookCover())) // Fixed cover conversion
                .rate(book.getRate())
                .score(score)
                .genre(book.getGenre())
                .description(book.getSynopsis())
                .recommendationReason(reason)
                .relevanceScore(score)
                .build();
    }

    /**
     * Convert file path covers to base64 format to match favorites API
     * Handles all edge cases: null, empty, -1, file paths, and existing base64
     */
    private String convertCoverToBase64(String bookCover) {
        // Handle null, empty, or invalid values that should show placeholder
        if (bookCover == null ||
                bookCover.trim().isEmpty() ||
                bookCover.equals("-1") ||
                bookCover.equals("null") ||
                bookCover.equals("0") ||
                bookCover.length() < 3) {

            log.debug("Invalid cover data, returning null for placeholder: {}", bookCover);
            return null; // Frontend will show placeholder
        }

        // If it's already a valid base64 string, return as-is
        if (isValidBase64(bookCover)) {
            log.debug("Cover is already valid base64, returning as-is");
            return bookCover;
        }

        // If it's a file path, try to convert to base64
        if (isFilePath(bookCover)) {
            return convertFilePathToBase64(bookCover);
        }

        // If it's a URL, return as-is (frontend will handle)
        if (bookCover.startsWith("http://") || bookCover.startsWith("https://")) {
            log.debug("Cover is URL, returning as-is: {}", bookCover);
            return bookCover;
        }

        // For any other case, log and return null for placeholder
        log.warn("Unknown cover format, returning null for placeholder: {}", bookCover);
        return null;
    }

    /**
     * Check if the string is a valid base64 format
     */
    private boolean isValidBase64(String str) {
        // Check common base64 prefixes for images
        return str.length() > 100 && (
                str.startsWith("/9j/") ||      // JPEG
                        str.startsWith("iVBOR") ||     // PNG
                        str.startsWith("UklGR") ||     // WebP
                        str.startsWith("R0lGOD") ||    // GIF
                        str.matches("^[A-Za-z0-9+/]+=*$") // General base64 pattern
        );
    }

    /**
     * Check if the string is a file path
     */
    private boolean isFilePath(String str) {
        return str.startsWith("./uploads") ||
                str.contains("uploads") ||
                str.contains("\\") ||
                str.contains("/") && str.contains(".");
    }

    /**
     * Convert file path to base64
     */
    private String convertFilePathToBase64(String filePath) {
        try {
            // Clean the path (handle Windows backslashes)
            String cleanPath = filePath.replace("\\", "/");

            // Remove leading "./" if present
            if (cleanPath.startsWith("./")) {
                cleanPath = cleanPath.substring(2);
            }

            Path path = Paths.get(cleanPath);

            // Check if file exists
            if (!Files.exists(path)) {
                log.warn("Cover file not found: {}", cleanPath);
                return null;
            }

            // Check file size (avoid loading huge files)
            long fileSize = Files.size(path);
            if (fileSize > 5 * 1024 * 1024) { // 5MB limit
                log.warn("Cover file too large ({}MB), skipping: {}", fileSize / (1024 * 1024), cleanPath);
                return null;
            }

            // Read file and convert to base64
            byte[] fileBytes = Files.readAllBytes(path);
            String base64 = Base64.getEncoder().encodeToString(fileBytes);

            log.debug("Successfully converted cover to base64 for file: {} (size: {}KB)",
                    cleanPath, fileBytes.length / 1024);
            return base64;

        } catch (Exception e) {
            log.error("Error converting cover to base64 for path '{}': {}", filePath, e.getMessage());
            return null; // Will show placeholder
        }
    }

    @CacheEvict(value = {"userRecommendations", "similarBooks"}, allEntries = true)
    public void invalidateUserCache(Long userId) {
        log.info("Invalidating cache for user: {}", userId);
    }

    public boolean isRecommendationServiceAvailable() {
        return true;
    }

    public boolean notifyNewBook(Book book) {
        log.info("Notifying recommendation system about new book: {}", book.getTitle());

        // Invalidate relevant caches since we have a new book
        invalidateAllCaches();

        // In a real AI system, this would:
        // 1. Add book to recommendation index
        // 2. Update similarity matrices
        // 3. Trigger model retraining if needed

        return true;
    }

    public Map<String, Object> manuallyUpdateBookInRecommendationService(Long bookId) {
        log.info("Manually updating book in recommendation system: {}", bookId);

        // Invalidate related caches
        invalidateAllCaches();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Book updated and caches refreshed");
        response.put("bookId", bookId);
        return response;
    }

    @CacheEvict(value = {"userRecommendations", "featuredBooks", "genreRecommendations",
            "similarBooks", "trendingBooks", "highestRatedBooks"}, allEntries = true)
    public Map<String, Object> refreshAllRecommendations(int limit) {
        log.info("Refreshing all recommendation caches");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All recommendation caches refreshed");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    public void invalidateAllCaches() {
        refreshAllRecommendations(0);
    }
}