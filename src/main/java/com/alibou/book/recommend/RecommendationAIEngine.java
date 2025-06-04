package com.alibou.book.recommend;

import com.alibou.book.book.Book;
import com.alibou.book.book.Genre;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationAIEngine {

    private final BookRecommendationHelper helper;

    public UserProfile buildUserProfile(Long userId, List<Book> readBooks) {
        log.info("Building user profile for user: {} with {} books", userId, readBooks.size());

        // Analyze genre preferences
        Map<Genre, Long> genreFrequency = readBooks.stream()
                .filter(book -> book.getGenre() != null)
                .collect(Collectors.groupingBy(Book::getGenre, Collectors.counting()));

        // Analyze author preferences
        Map<String, Long> authorFrequency = readBooks.stream()
                .filter(book -> book.getAuthorName() != null)
                .collect(Collectors.groupingBy(Book::getAuthorName, Collectors.counting()));

        // Calculate average rating preference
        double avgRatingPreference = readBooks.stream()
                .mapToDouble(Book::getRate)
                .average()
                .orElse(4.0);

        return UserProfile.builder()
                .userId(userId)
                .preferredGenres(genreFrequency.keySet())
                .preferredAuthors(authorFrequency.keySet())
                .averageRatingPreference(avgRatingPreference)
                .genreWeights(calculateGenreWeights(genreFrequency))
                .authorWeights(calculateAuthorWeights(authorFrequency))
                .build();
    }

    public List<Recommendation> scoreAndRankBooks(List<Book> candidates, UserProfile userProfile, int limit) {
        log.info("Scoring and ranking {} candidate books", candidates.size());

        return candidates.parallelStream()
                .map(book -> {
                    double score = calculateBookScore(book, userProfile);
                    String reason = generateRecommendationReason(book, userProfile);
                    return toRecommendation(book, score, reason);
                })
                .sorted(Comparator.comparingDouble(Recommendation::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Recommendation> rankBooksByPopularityAndQuality(List<Book> books, int limit) {
        return books.stream()
                .sorted(Comparator.comparingDouble(Book::getRate).reversed())
                .limit(limit)
                .map(book -> toRecommendation(book, book.getRate(), "Popular in " + book.getGenre()))
                .collect(Collectors.toList());
    }

    public List<Recommendation> findSimilarBooks(Book targetBook, int limit) {
        log.info("Finding similar books to: {}", targetBook.getTitle());

        List<Book> similarBooks = helper.getSimilarBooksByGenreAndAuthor(targetBook, limit * 2);

        return similarBooks.stream()
                .map(book -> {
                    double similarity = calculateSimilarityScore(targetBook, book);
                    String reason = "Similar to " + targetBook.getTitle();
                    return toRecommendation(book, similarity, reason);
                })
                .sorted(Comparator.comparingDouble(Recommendation::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Recommendation> calculateTrendingScore(List<Book> books, int limit) {
        log.info("Calculating trending scores for {} books", books.size());

        return books.stream()
                .map(book -> {
                    double trendingScore = book.getRate() * 0.7 + calculateRecentPopularity(book) * 0.3;
                    return toRecommendation(book, trendingScore, "Trending now");
                })
                .sorted(Comparator.comparingDouble(Recommendation::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void processFeedback(RecommendationFeedback feedback) {
        log.info("Processing AI feedback: {}", feedback);
        // In a real implementation, this would update ML models
        // For now, we just log the feedback for future learning
    }

    private double calculateBookScore(Book book, UserProfile userProfile) {
        double score = book.getRate() * 0.4; // Base quality score

        // Genre preference boost
        if (userProfile.getPreferredGenres().contains(book.getGenre())) {
            Double genreWeight = userProfile.getGenreWeights().get(book.getGenre());
            score += (genreWeight != null ? genreWeight : 0.5) * 0.3;
        }

        // Author preference boost
        if (userProfile.getPreferredAuthors().contains(book.getAuthorName())) {
            Double authorWeight = userProfile.getAuthorWeights().get(book.getAuthorName());
            score += (authorWeight != null ? authorWeight : 0.5) * 0.2;
        }

        // Rating alignment bonus
        double ratingDiff = Math.abs(book.getRate() - userProfile.getAverageRatingPreference());
        score += Math.max(0, (5 - ratingDiff) / 5) * 0.1;

        return Math.min(5.0, score);
    }

    private double calculateSimilarityScore(Book book1, Book book2) {
        double score = 0.0;

        // Genre similarity
        if (Objects.equals(book1.getGenre(), book2.getGenre())) {
            score += 0.4;
        }

        // Author similarity
        if (Objects.equals(book1.getAuthorName(), book2.getAuthorName())) {
            score += 0.3;
        }

        // Rating similarity
        double ratingDiff = Math.abs(book1.getRate() - book2.getRate());
        score += Math.max(0, (5 - ratingDiff) / 5) * 0.3;

        return score;
    }

    private double calculateRecentPopularity(Book book) {
        // Simplified popularity calculation
        // In real implementation, this would consider recent views, borrows, etc.
        return Math.random() * 2; // Random factor for demo
    }

    private String generateRecommendationReason(Book book, UserProfile userProfile) {
        List<String> reasons = new ArrayList<>();

        if (userProfile.getPreferredGenres().contains(book.getGenre())) {
            reasons.add("matches your " + book.getGenre() + " preference");
        }

        if (userProfile.getPreferredAuthors().contains(book.getAuthorName())) {
            reasons.add("by your favorite author " + book.getAuthorName());
        }

        if (book.getRate() >= 4.5) {
            reasons.add("highly rated (" + book.getRate() + "⭐)");
        }

        return reasons.isEmpty() ? "Recommended for you" : "Because it " + String.join(" and ", reasons);
    }

    private Map<Genre, Double> calculateGenreWeights(Map<Genre, Long> genreFrequency) {
        long total = genreFrequency.values().stream().mapToLong(Long::longValue).sum();
        return genreFrequency.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (double) entry.getValue() / total
                ));
    }

    private Map<String, Double> calculateAuthorWeights(Map<String, Long> authorFrequency) {
        long total = authorFrequency.values().stream().mapToLong(Long::longValue).sum();
        return authorFrequency.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (double) entry.getValue() / total
                ));
    }

    // FIXED: Added the same cover conversion logic as RecommendationService
    private Recommendation toRecommendation(Book book, double score, String reason) {
        return Recommendation.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .authorName(book.getAuthorName())
                .cover(convertCoverToBase64(book.getBookCover())) // ✅ FIXED: Convert cover properly
                .rate(book.getRate())
                .score(score)
                .genre(book.getGenre())
                .description(book.getSynopsis())
                .recommendationReason(reason)
                .relevanceScore(score)
                .build();
    }

    /**
     * ADDED: Same cover conversion logic as RecommendationService
     * Convert file path covers to base64 format to match favorites API
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

            log.debug("AI Engine: Successfully converted cover to base64 for file: {} (size: {}KB)",
                    cleanPath, fileBytes.length / 1024);
            return base64;

        } catch (Exception e) {
            log.error("AI Engine: Error converting cover to base64 for path '{}': {}", filePath, e.getMessage());
            return null; // Will show placeholder
        }
    }
}