package com.alibou.book.recommend;

import com.alibou.book.book.Book;
import com.alibou.book.book.BookRepository;
import com.alibou.book.book.Genre;
import com.alibou.book.history.BookTransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookRecommendationHelper {

    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository historyRepository;

    public List<Book> getTopRatedBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findActiveBooksSortedByRate(pageable).getContent();
    }

    public List<Book> getBooksReadByUser(Long userId) {
        return bookRepository.findBooksReadByUser(userId);
    }

    public List<Book> getBooksByGenre(Genre genre) {
        // Convert Genre enum to String for native query
        return bookRepository.findByGenreAndArchivedFalseOrderByRateDesc(genre.name());
    }

    public Optional<Book> getBookById(Long bookId) {
        return bookRepository.findByIdAndArchivedFalse(bookId);
    }

    public List<Book> getCandidateBooks(UserProfile userProfile, List<Book> alreadyRead, int limit) {
        Set<Long> readBookIds = alreadyRead.stream()
                .map(Book::getId)
                .collect(Collectors.toSet());

        Set<Genre> preferredGenres = userProfile.getPreferredGenres();
        Set<String> preferredAuthors = userProfile.getPreferredAuthors();

        // Convert Genre enums to Strings for native query
        List<String> genreStrings = preferredGenres.stream()
                .map(Genre::name)
                .collect(Collectors.toList());

        List<String> authorsList = new ArrayList<>(preferredAuthors);
        Pageable pageable = PageRequest.of(0, limit);

        // FIXED: Added the missing boolean parameter for excludeIdsEmpty
        return bookRepository.findCandidateBooks(
                readBookIds,
                readBookIds.isEmpty(), // Added this parameter
                genreStrings,
                authorsList,
                pageable
        );
    }

    public List<Book> getRecentlyActiveBooks(LocalDateTime since) {
        return bookRepository.findRecentlyActiveBooks(since);
    }

    public List<Book> getBooksByAuthor(String authorName, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findByAuthorNameAndArchivedFalseOrderByRateDesc(authorName, pageable);
    }

    public List<Book> getSimilarBooksByGenreAndAuthor(Book targetBook, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        // Convert Genre enum to String for native query
        String genreString = targetBook.getGenre() != null ? targetBook.getGenre().name() : "";

        return bookRepository.findSimilarBooks(
                targetBook.getId(),
                genreString,
                targetBook.getAuthorName(),
                pageable
        );
    }

    // Additional helper methods for better recommendations
    public List<Book> getTopRatedBooksByGenres(List<Genre> genres, int limit) {
        // Convert Genre enums to Strings
        List<String> genreStrings = genres.stream()
                .map(Genre::name)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findByGenresOrderByRateDesc(genreStrings, pageable);
    }

    public List<Book> getHighlyRatedBooks(double minRating, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findTopRatedBooks(minRating, pageable);
    }

    public Map<Genre, Long> getGenreStatistics() {
        List<Object[]> results = bookRepository.countBooksByGenre();
        return results.stream()
                .collect(Collectors.toMap(
                        row -> Genre.valueOf((String) row[0]),
                        row -> (Long) row[1]
                ));
    }

    public Map<Genre, Double> getAverageRatingsByGenre() {
        List<Object[]> results = bookRepository.getAverageRatingByGenre();
        return results.stream()
                .collect(Collectors.toMap(
                        row -> Genre.valueOf((String) row[0]),
                        row -> (Double) row[1]
                ));
    }
}