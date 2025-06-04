// ===== BookRepository.java - Final Complete Interface (FIXED) =====
package com.alibou.book.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    // ===== YOUR EXISTING METHODS (PRESERVED) =====

    @Query("""
            SELECT book
            FROM Book book
            WHERE book.archived = false
            AND book.shareable = true
            """)
    Page<Book> findAllDisplayableBooks(Pageable pageable, String userId);

    Page<Book> findAllByArchived(boolean archived, Pageable pageable);

    /**
     * Find all non-archived books owned by a specific user
     */
    @Query("""
            SELECT b
            FROM Book b
            WHERE b.owner.id = :userId
            AND b.archived = false
            """)
    List<Book> findBooksByOwnerId(@Param("userId") Long userId);

    /**
     * Find all non-archived books by username of the owner
     * This can be more reliable than finding by owner ID
     */
    @Query("""
            SELECT b
            FROM Book b
            WHERE b.owner.username = :username
            AND b.archived = false
            """)
    List<Book> findBooksByUsername(@Param("username") String username);

    /**
     * Count total active books in the system
     */
    @Query("""
            SELECT COUNT(b)
            FROM Book b
            WHERE b.archived = false
            """)
    long countActiveBooks();

    /**
     * Find books by genre and not archived
     */
    @Query("""
            SELECT b
            FROM Book b
            WHERE b.genre = :genre
            AND b.archived = false
            """)
    List<Book> findByGenreAndArchivedFalse(@Param("genre") Genre genre);

    /**
     * Find all books that are not archived with pagination
     */
    Page<Book> findByArchivedFalse(Pageable pageable);

    // ===== NEW OPTIMIZED QUERIES FOR AI RECOMMENDATIONS =====

    /**
     * Get active books sorted by rating with pagination (for recommendations)
     * Using native query to handle rating calculation properly
     */
    @Query(value = """
            SELECT b.* FROM book b 
            LEFT JOIN (
                SELECT book_id, AVG(note) as avg_rating 
                FROM feedback 
                GROUP BY book_id
            ) f ON b.id = f.book_id
            WHERE b.archived = false AND b.shareable = true 
            ORDER BY COALESCE(f.avg_rating, 0.0) DESC
            """, nativeQuery = true)
    Page<Book> findActiveBooksSortedByRate(Pageable pageable);

    /**
     * Get books read by a specific user (optimized single query)
     */
    @Query("""
            SELECT DISTINCT b FROM Book b 
            JOIN BookTransactionHistory h ON b.id = h.book.id 
            WHERE h.user.id = :userId AND h.returned = true AND b.archived = false
            """)
    List<Book> findBooksReadByUser(@Param("userId") Long userId);

    /**
     * Get books by genre, not archived, ordered by rating (enhanced version)
     */
    @Query(value = """
            SELECT b.* FROM book b 
            LEFT JOIN (
                SELECT book_id, AVG(note) as avg_rating 
                FROM feedback 
                GROUP BY book_id
            ) f ON b.id = f.book_id
            WHERE b.genre = :genre AND b.archived = false 
            ORDER BY COALESCE(f.avg_rating, 0.0) DESC
            """, nativeQuery = true)
    List<Book> findByGenreAndArchivedFalseOrderByRateDesc(@Param("genre") String genre);

    /**
     * Get a book by ID if not archived
     */
    Optional<Book> findByIdAndArchivedFalse(Long bookId);

    /**
     * Get candidate books for recommendations based on user preferences
     */
    @Query(value = """
            SELECT DISTINCT b.* FROM book b 
            LEFT JOIN (
                SELECT book_id, AVG(note) as avg_rating 
                FROM feedback 
                GROUP BY book_id
            ) f ON b.id = f.book_id
            WHERE b.archived = false AND b.shareable = true 
            AND (:excludeIdsEmpty = true OR b.id NOT IN :excludeIds)
            AND (b.genre IN :preferredGenres OR b.author_name IN :preferredAuthors) 
            ORDER BY COALESCE(f.avg_rating, 0.0) DESC
            LIMIT :#{#pageable.pageSize}
            """, nativeQuery = true)
    List<Book> findCandidateBooks(@Param("excludeIds") Set<Long> excludeIds,
                                  @Param("excludeIdsEmpty") boolean excludeIdsEmpty,
                                  @Param("preferredGenres") List<String> preferredGenres,
                                  @Param("preferredAuthors") List<String> preferredAuthors,
                                  Pageable pageable);

    /**
     * Get recently active books based on transaction history - FIXED VERSION
     */
    @Query("""
            SELECT DISTINCT b FROM Book b 
            JOIN BookTransactionHistory h ON b.id = h.book.id 
            WHERE h.createdDate >= :since AND b.archived = false 
            ORDER BY b.id DESC
            """)
    List<Book> findRecentlyActiveBooks(@Param("since") LocalDateTime since);

    /**
     * Get books by author with pagination, ordered by rating
     */
    @Query(value = """
            SELECT b.* FROM book b 
            LEFT JOIN (
                SELECT book_id, AVG(note) as avg_rating 
                FROM feedback 
                GROUP BY book_id
            ) f ON b.id = f.book_id
            WHERE b.author_name = :authorName AND b.archived = false 
            ORDER BY COALESCE(f.avg_rating, 0.0) DESC
            LIMIT :#{#pageable.pageSize}
            """, nativeQuery = true)
    List<Book> findByAuthorNameAndArchivedFalseOrderByRateDesc(@Param("authorName") String authorName, Pageable pageable);

    /**
     * Find similar books based on genre and author, excluding the target book
     */
    @Query(value = """
            SELECT b.* FROM book b 
            LEFT JOIN (
                SELECT book_id, AVG(note) as avg_rating 
                FROM feedback 
                GROUP BY book_id
            ) f ON b.id = f.book_id
            WHERE b.id != :excludeId AND b.archived = false 
            AND (b.genre = :genre OR b.author_name = :authorName) 
            ORDER BY COALESCE(f.avg_rating, 0.0) DESC
            LIMIT :#{#pageable.pageSize}
            """, nativeQuery = true)
    List<Book> findSimilarBooks(@Param("excludeId") Long excludeId,
                                @Param("genre") String genre,
                                @Param("authorName") String authorName,
                                Pageable pageable);

    /**
     * Get top-rated books with minimum rating threshold
     */
    @Query(value = """
            SELECT b.* FROM book b 
            LEFT JOIN (
                SELECT book_id, AVG(note) as avg_rating 
                FROM feedback 
                GROUP BY book_id
            ) f ON b.id = f.book_id
            WHERE b.archived = false AND b.shareable = true 
            AND COALESCE(f.avg_rating, 0.0) >= :minRating 
            ORDER BY COALESCE(f.avg_rating, 0.0) DESC
            LIMIT :#{#pageable.pageSize}
            """, nativeQuery = true)
    List<Book> findTopRatedBooks(@Param("minRating") double minRating, Pageable pageable);

    /**
     * Get books by multiple genres
     */
    @Query(value = """
            SELECT b.* FROM book b 
            LEFT JOIN (
                SELECT book_id, AVG(note) as avg_rating 
                FROM feedback 
                GROUP BY book_id
            ) f ON b.id = f.book_id
            WHERE b.archived = false AND b.shareable = true 
            AND b.genre IN :genres 
            ORDER BY COALESCE(f.avg_rating, 0.0) DESC
            LIMIT :#{#pageable.pageSize}
            """, nativeQuery = true)
    List<Book> findByGenresOrderByRateDesc(@Param("genres") List<String> genres, Pageable pageable);

    /**
     * Get books count by genre for analytics
     */
    @Query("""
            SELECT b.genre, COUNT(b) FROM Book b 
            WHERE b.archived = false 
            GROUP BY b.genre
            """)
    List<Object[]> countBooksByGenre();

    /**
     * Get average rating by genre
     */
    @Query(value = """
            SELECT b.genre, AVG(COALESCE(f.avg_rating, 0.0)) 
            FROM book b 
            LEFT JOIN (
                SELECT book_id, AVG(note) as avg_rating 
                FROM feedback 
                GROUP BY book_id
            ) f ON b.id = f.book_id
            WHERE b.archived = false 
            GROUP BY b.genre
            """, nativeQuery = true)
    List<Object[]> getAverageRatingByGenre();
}