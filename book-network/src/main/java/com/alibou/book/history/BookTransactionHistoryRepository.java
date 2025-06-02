package com.alibou.book.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookTransactionHistoryRepository extends JpaRepository<BookTransactionHistory, Integer> {

    @Query("""
            SELECT (COUNT(*) > 0) 
            FROM BookTransactionHistory history
            WHERE history.user.id = :userId
            AND history.book.id = :bookId
            AND history.returnApproved = false
            """)
    boolean isAlreadyBorrowedByUser(@Param("bookId") Long bookId, @Param("userId") Long userId);

    @Query("""
            SELECT (COUNT(*) > 0) 
            FROM BookTransactionHistory history
            WHERE history.book.id = :bookId
            AND history.returnApproved = false
            """)
    boolean isAlreadyBorrowed(@Param("bookId") Long bookId);

    @Query("""
            SELECT history
            FROM BookTransactionHistory history
            WHERE history.user.id = :userId
            AND history.book.id = :bookId
            AND history.returned = false
            AND history.returnApproved = false
            """)
    Optional<BookTransactionHistory> findByBookIdAndUserId(@Param("bookId") Long bookId, @Param("userId") Long userId);

    @Query("""
            SELECT history
            FROM BookTransactionHistory history
            WHERE history.book.createdBy = :userId
            AND history.book.id = :bookId
            AND history.returned = true
            AND history.returnApproved = false
            """)
    Optional<BookTransactionHistory> findByBookIdAndOwnerId(@Param("bookId") Long bookId, @Param("userId") String userId);

    @Query("""
            SELECT history
            FROM BookTransactionHistory history
            WHERE history.user.id = :userId
            """)
    Page<BookTransactionHistory> findAllBorrowedBooks(Pageable pageable, @Param("userId") Long userId);

    @Query("""
            SELECT history
            FROM BookTransactionHistory history
            WHERE history.book.createdBy = :userId
            """)
    Page<BookTransactionHistory> findAllReturnedBooks(Pageable pageable, @Param("userId") String userId);

    // Add this delete method for the delete book functionality
    @Modifying
    @Query("DELETE FROM BookTransactionHistory b WHERE b.book.id = :bookId")
    void deleteByBookId(Long bookId);
    // Add this method to your BookTransactionHistoryRepository interface
    List<BookTransactionHistory> findByUserIdAndReturnedTrue(Long userId);
}

