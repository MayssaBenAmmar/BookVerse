package com.alibou.book.book;

import com.alibou.book.file.FileUtils;
import com.alibou.book.history.BookTransactionHistory;
import com.alibou.book.user.User;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class BookMapper {

    public Document toDocument(Book book) {
        String bookContent = """
                Title: %s
                Author: %s
                Synopsis: %s
                Genre: %s
                """.formatted(
                book.getTitle(),
                book.getAuthorName(),
                book.getSynopsis(),
                book.getGenre() != null ? book.getGenre().toString() : "Unknown"
        );

        // Create a mutable map to add genre if it exists
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bookId", book.getId());
        metadata.put("isbn", book.getIsbn());
        metadata.put("archived", book.isArchived());
        metadata.put("shareable", book.isShareable());

        // Add genre if available
        if (book.getGenre() != null) {
            metadata.put("genre", book.getGenre().toString());
        }

        return new Document(bookContent, metadata);
    }

    public Book toBook(BookRequest request) {
        return Book.builder()
                .id(request.id() != null ? request.id().longValue() : null)
                .title(request.title())
                .isbn(request.isbn())
                .authorName(request.authorName())
                .synopsis(request.synopsis())
                .archived(false)
                .shareable(request.shareable())
                .genre(request.genre()) // The genre is already of the correct type
                .build();
    }

    public BookResponse toBookResponse(Book book) {
        // Get owner information
        String ownerUsername = null;
        Long ownerId = null;

        if (book.getOwner() != null) {
            User owner = book.getOwner();

            // Use username first, fall back to email
            ownerUsername = owner.getUsername() != null ? owner.getUsername() : owner.getEmail();

            // Get the owner ID field directly
            ownerId = owner.getId();

            // Additional logging to debug
            System.out.println("Owner for book " + book.getId() + ": " + ownerUsername);
        }

        // Handle cover data properly
        String coverData = null;
        if (book.getBookCover() != null) {
            byte[] coverBytes = FileUtils.readFileFromLocation(book.getBookCover());
            if (coverBytes != null && coverBytes.length > 0) {
                coverData = Base64.getEncoder().encodeToString(coverBytes);
            }
        }

        return BookResponse.builder()
                .id(book.getId() != null ? book.getId().longValue() : null)
                .title(book.getTitle())
                .authorName(book.getAuthorName())
                .isbn(book.getIsbn())
                .synopsis(book.getSynopsis())
                .rate(book.getRate())
                .archived(book.isArchived())
                .shareable(book.isShareable())
                .cover(coverData)  // Use the properly encoded cover data
                .owner(ownerUsername)
                .ownerId(ownerId)
                .genre(book.getGenre()) // Add genre to response
                .build();
    }

    public BorrowedBookResponse toBorrowedBookResponse(BookTransactionHistory history) {
        return BorrowedBookResponse.builder()
                .id(history.getBook().getId() != null ? history.getBook().getId().longValue() : null)
                .title(history.getBook().getTitle())
                .authorName(history.getBook().getAuthorName())
                .isbn(history.getBook().getIsbn())
                .rate(history.getBook().getRate())
                .returned(history.isReturned())
                .returnApproved(history.isReturnApproved())
                .genre(history.getBook().getGenre()) // Add genre to borrowed book response
                .borrowedDate(history.getBorrowedDate()) // NEW: Add borrowed date
                .returnedDate(history.getReturnedDate()) // NEW: Add returned date
                .build();
    }
}