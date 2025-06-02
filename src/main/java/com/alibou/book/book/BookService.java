package com.alibou.book.book;

import com.alibou.book.common.PageResponse;
import com.alibou.book.exception.OperationNotPermittedException;
import com.alibou.book.file.FileStorageService;
import com.alibou.book.history.BookTransactionHistory;
import com.alibou.book.history.BookTransactionHistoryRepository;
import com.alibou.book.recommend.RecommendationService;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
// ONLY dashboard imports - no old audit system
import com.alibou.book.dashboard.DashboardService;
import com.alibou.book.dashboard.DashboardAuditLog;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibou.book.book.BookSpecification.withOwnerId;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final FileStorageService fileStorageService;
    private final VectorStore vectorStore;
    private final UserRepository userRepository;
    private final RecommendationService recommendationService;
    // ONLY dashboard service - unified audit system
    private final DashboardService dashboardService;

    public Long save(BookRequest request, Authentication connectedUser) {
        // Convert DTO to entity
        Book book = bookMapper.toBook(request);

        // Set the owner to the connected user
        User owner = getUserFromAuthentication(connectedUser);
        book.setOwner(owner);

        // Save the book
        book = bookRepository.save(book);

        try {
            Document bookDocument = this.bookMapper.toDocument(book);
            this.vectorStore.add(List.of(bookDocument));
        } catch (Exception e) {
            log.error("Failed to add book to vector store: {}", e.getMessage(), e);
        }

        // Dashboard audit logging - unified system
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.CREATE_BOOK,
                book.getId(),
                book.getTitle()
        );

        // Notify recommendation service about new book
        try {
            if (recommendationService.notifyNewBook(book)) {
                log.info("Successfully notified recommendation service about new book: {}", book.getId());
            } else {
                log.warn("Failed to notify recommendation service about new book: {}", book.getId());
            }
        } catch (Exception e) {
            log.error("Error notifying recommendation service: {}", e.getMessage(), e);
        }

        return book.getId();
    }

    public BookResponse findById(Long bookId) {
        BookResponse response = bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));

        // Log view activity for dashboard analytics
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.VIEW_BOOK,
                bookId,
                response.getTitle()
        );

        return response;
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, connectedUser.getName());
        List<BookResponse> booksResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(booksResponse, books.getNumber(), books.getSize(), books.getTotalElements(), books.getTotalPages(), books.isFirst(), books.isLast());
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAll(withOwnerId(connectedUser.getName()), pageable);
        List<BookResponse> booksResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .filter(book -> !book.getArchived())
                .toList();
        return new PageResponse<>(booksResponse, books.getNumber(), books.getSize(), books.getTotalElements(), books.getTotalPages(), books.isFirst(), books.isLast());
    }

    public PageResponse<BookResponse> findAllArchivedBooks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllByArchived(true, pageable);
        List<BookResponse> booksResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(booksResponse, books.getNumber(), books.getSize(), books.getTotalElements(), books.getTotalPages(), books.isFirst(), books.isLast());
    }

    public PageResponse<BookResponse> vectorStoreSearchBooks(String query) {
        // Log search activity for dashboard
        dashboardService.logActivity(DashboardAuditLog.ActionType.SEARCH);

        if (query == null || query.isBlank()) {
            List<BookResponse> dbBooks = bookRepository.findAll().stream()
                    .filter(book -> !book.isArchived())
                    .map(this.bookMapper::toBookResponse)
                    .toList();

            return new PageResponse<>(dbBooks, 0, 0, dbBooks.size(), 1, true, true);
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(50)
                .similarityThreshold(0.2)
                .build();

        List<Document> searchResults = this.vectorStore.similaritySearch(searchRequest);
        Map<Long, Double> bookIdToScore = new HashMap<>();

        searchResults.forEach(doc -> {
            Long bookId = ((Number) doc.getMetadata().get("bookId")).longValue();
            Double score = doc.getScore();
            bookIdToScore.compute(bookId, (k, existingScore) ->
                    existingScore == null ? score : Math.max(existingScore, score));
        });

        List<Book> books = bookRepository.findAllById(bookIdToScore.keySet());
        List<Book> sortedBooks = books.stream()
                .sorted(Comparator.comparingDouble(book -> -bookIdToScore.getOrDefault(book.getId(), 0.0)))
                .toList();

        List<BookResponse> booksResponse = sortedBooks.stream()
                .map(bookMapper::toBookResponse)
                .toList();

        return new PageResponse<>(booksResponse, 0, 0, books.size(), 1, true, true);
    }

    public Long updateShareableStatus(Long bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        if (!Objects.equals(book.getCreatedBy(), connectedUser.getName())) {
            throw new OperationNotPermittedException("You cannot update others books shareable status");
        }

        boolean newShareableStatus = !book.isShareable();
        book.setShareable(newShareableStatus);
        book = bookRepository.save(book);

        // Dashboard audit logging - unified system
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.UPDATE_BOOK,
                book.getId(),
                book.getTitle() + (newShareableStatus ? " (shared)" : " (unshared)")
        );

        // Recommendation service notification
        if (newShareableStatus) {
            try {
                if (recommendationService.notifyNewBook(book)) {
                    log.info("Successfully notified recommendation service about book status change: {}", book.getId());
                } else {
                    log.warn("Failed to notify recommendation service about book status change: {}", book.getId());
                }
            } catch (Exception e) {
                log.error("Error notifying recommendation service about book status change: {}", e.getMessage(), e);
            }
        }

        return bookId;
    }

    public Long updateArchivedStatus(Long bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        if (!Objects.equals(book.getCreatedBy(), connectedUser.getName())) {
            throw new OperationNotPermittedException("You cannot update others books archived status");
        }

        boolean newArchivedStatus = !book.isArchived();
        book.setArchived(newArchivedStatus);
        book = bookRepository.save(book);

        // Dashboard audit logging - unified system
        if (newArchivedStatus) {
            dashboardService.logBookActivity(
                    DashboardAuditLog.ActionType.ARCHIVE_BOOK,
                    book.getId(),
                    book.getTitle()
            );
        } else {
            dashboardService.logBookActivity(
                    DashboardAuditLog.ActionType.UNARCHIVE_BOOK,
                    book.getId(),
                    book.getTitle()
            );

            // If book is now unarchived, notify recommendation service
            try {
                if (recommendationService.notifyNewBook(book)) {
                    log.info("Successfully notified recommendation service about book status change: {}", book.getId());
                } else {
                    log.warn("Failed to notify recommendation service about book status change: {}", book.getId());
                }
            } catch (Exception e) {
                log.error("Error notifying recommendation service about book status change: {}", e.getMessage(), e);
            }
        }

        return bookId;
    }

    public Long borrowBook(Long bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is archived or not shareable");
        }

        String username = connectedUser.getName();
        if (Objects.equals(book.getCreatedBy(), username)) {
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }

        User user = getUserFromAuthentication(connectedUser);

        if (transactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getId())) {
            throw new OperationNotPermittedException("You already borrowed this book");
        }
        if (transactionHistoryRepository.isAlreadyBorrowed(bookId)) {
            throw new OperationNotPermittedException("The book is already borrowed");
        }

        BookTransactionHistory transaction = BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .borrowedDate(LocalDateTime.now())
                .build();

        Long transactionId = transactionHistoryRepository.save(transaction).getId();

        // Dashboard audit logging - unified system
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.BORROW_BOOK,
                book.getId(),
                book.getTitle()
        );

        return transactionId;
    }

    public Long returnBorrowedBook(Long bookId, Authentication connectedUser) {
        User user = getUserFromAuthentication(connectedUser);
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));

        BookTransactionHistory transaction = transactionHistoryRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book"));

        transaction.setReturned(true);
        transaction.setReturnedDate(LocalDateTime.now());
        Long transactionId = transactionHistoryRepository.save(transaction).getId();

        // Dashboard audit logging - unified system
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.RETURN_BOOK,
                book.getId(),
                book.getTitle()
        );

        return transactionId;
    }

    public Long approveReturnBorrowedBook(Long bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        if (!Objects.equals(book.getCreatedBy(), connectedUser.getName())) {
            throw new OperationNotPermittedException("You cannot approve return for a book you do not own");
        }

        BookTransactionHistory transaction = transactionHistoryRepository.findByBookIdAndOwnerId(bookId, connectedUser.getName())
                .orElseThrow(() -> new OperationNotPermittedException("The book is not returned yet"));

        transaction.setReturnApproved(true);
        Long transactionId = transactionHistoryRepository.save(transaction).getId();

        // Dashboard audit logging - unified system
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.UPDATE_BOOK,
                book.getId(),
                book.getTitle() + " (return approved)"
        );

        return transactionId;
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
        var profilePicture = fileStorageService.saveFile(file, connectedUser.getName());
        book.setBookCover(profilePicture);
        Book updatedBook = bookRepository.save(book);

        // Dashboard audit logging - unified system
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.UPDATE_BOOK,
                book.getId(),
                book.getTitle() + " (cover updated)"
        );

        // Notify recommendation service about updated book
        try {
            if (recommendationService.notifyNewBook(updatedBook)) {
                log.info("Successfully notified recommendation service about updated book cover: {}", book.getId());
            } else {
                log.warn("Failed to notify recommendation service about updated book cover: {}", book.getId());
            }
        } catch (Exception e) {
            log.error("Error notifying recommendation service about updated book: {}", e.getMessage(), e);
        }
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        User user = getUserFromAuthentication(connectedUser);
        Page<BookTransactionHistory> borrowed = transactionHistoryRepository.findAllBorrowedBooks(pageable, user.getId());
        List<BorrowedBookResponse> response = borrowed.stream().map(bookMapper::toBorrowedBookResponse).toList();
        return new PageResponse<>(response, borrowed.getNumber(), borrowed.getSize(), borrowed.getTotalElements(), borrowed.getTotalPages(), borrowed.isFirst(), borrowed.isLast());
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> returned = transactionHistoryRepository.findAllReturnedBooks(pageable, connectedUser.getName());
        List<BorrowedBookResponse> response = returned.stream().map(bookMapper::toBorrowedBookResponse).toList();
        return new PageResponse<>(response, returned.getNumber(), returned.getSize(), returned.getTotalElements(), returned.getTotalPages(), returned.isFirst(), returned.isLast());
    }

    @Transactional
    public void deleteBook(Long bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));

        // Check if the user is the owner of the book
        if (!Objects.equals(book.getCreatedBy(), connectedUser.getName())) {
            throw new OperationNotPermittedException("You cannot delete books that do not belong to you");
        }

        // Get book title before deletion for the log
        String bookTitle = book.getTitle();

        // Since VectorStore doesn't have a remove method, we'll just log this
        log.warn("Book with ID {} will be deleted from database but remains in vector store", bookId);

        // Delete related transaction history records
        transactionHistoryRepository.deleteByBookId(bookId);

        // Finally delete the book
        bookRepository.delete(book);

        // Dashboard audit logging - unified system
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.DELETE_BOOK,
                bookId,
                bookTitle
        );
    }

    // Helper method to log when books are favorited (if you have this feature)
    public void favoriteBook(Long bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));

        // Your favorite logic here...

        // Dashboard audit logging
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.FAVORITE_BOOK,
                book.getId(),
                book.getTitle()
        );
    }

    // Helper method to log when books are unfavorited
    public void unfavoriteBook(Long bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));

        // Your unfavorite logic here...

        // Dashboard audit logging
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.UNFAVORITE_BOOK,
                book.getId(),
                book.getTitle()
        );
    }

    // Helper method to log when books are rated
    public void rateBook(Long bookId, double rating, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));

        // Your rating logic here...

        // Dashboard audit logging
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.RATE_BOOK,
                book.getId(),
                book.getTitle() + " (rated " + rating + ")"
        );
    }

    // Keep all your existing utility methods
    public List<BookResponse> getRecentTopRatedBooks(int days, int count) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        Date threshold = cal.getTime();

        Pageable pageable = PageRequest.of(0, 100, Sort.by("createdDate").descending());

        Page<Book> recentBooks = bookRepository.findAll(
                (root, query, cb) -> {
                    return cb.and(
                            cb.greaterThanOrEqualTo(root.get("createdDate"), threshold),
                            cb.equal(root.get("shareable"), true),
                            cb.equal(root.get("archived"), false)
                    );
                },
                pageable
        );

        List<BookResponse> bookResponses = recentBooks.getContent().stream()
                .map(bookMapper::toBookResponse)
                .sorted((b1, b2) -> Double.compare(b2.getRate(), b1.getRate()))
                .limit(count)
                .toList();

        return bookResponses;
    }

    public Book getBookEntityById(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + bookId));
    }

    public List<Book> getAllActiveBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAll(
                (root, query, cb) -> {
                    return cb.and(
                            cb.equal(root.get("shareable"), true),
                            cb.equal(root.get("archived"), false)
                    );
                },
                pageable
        );
        return books.getContent();
    }

    private User getUserFromAuthentication(Authentication authentication) {
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);

                    if (authentication.getPrincipal() instanceof Jwt) {
                        Jwt jwt = (Jwt) authentication.getPrincipal();
                        String firstName = jwt.getClaimAsString("given_name");
                        String lastName = jwt.getClaimAsString("family_name");

                        String preferredUsername = jwt.getClaimAsString("preferred_username");

                        if (preferredUsername != null &&
                                (preferredUsername.contains("-") && preferredUsername.length() > 30)) {

                            if (email != null && email.contains("@")) {
                                preferredUsername = email.split("@")[0];
                            } else if (firstName != null && !firstName.isEmpty()) {
                                preferredUsername = firstName.toLowerCase().replaceAll("\\s+", "");
                            }
                        }

                        if (preferredUsername != null) {
                            newUser.setUsername(preferredUsername);
                        }

                        if (firstName != null) {
                            newUser.setFirstname(firstName);
                        }
                        if (lastName != null) {
                            newUser.setLastname(lastName);
                        }
                    }

                    return userRepository.save(newUser);
                });
    }

    public List<Book> getBooksReadByUser(Long userId) {
        List<BookTransactionHistory> histories = transactionHistoryRepository.findByUserIdAndReturnedTrue(userId);

        Set<Long> bookIds = histories.stream()
                .map(history -> history.getBook().getId())
                .collect(Collectors.toSet());

        if (bookIds.isEmpty()) {
            return Collections.emptyList();
        }

        return bookRepository.findAllById(bookIds);
    }

    public List<Book> getBooksByGenre(Genre genre) {
        return bookRepository.findByGenreAndArchivedFalse(genre);
    }
}