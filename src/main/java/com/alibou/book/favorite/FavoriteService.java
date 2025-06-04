package com.alibou.book.favorite;

import com.alibou.book.book.Book;
import com.alibou.book.book.BookMapper;
import com.alibou.book.book.BookRepository;
import com.alibou.book.book.BookResponse;
import com.alibou.book.dashboard.DashboardService;
import com.alibou.book.dashboard.DashboardAuditLog;
import com.alibou.book.exception.BusinessException;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Optional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookMapper bookMapper;
    // ADD THIS: Dashboard service for logging
    private final DashboardService dashboardService;

    public List<BookResponse> getUserFavorites(Authentication connectedUser) {
        User user = getUserFromAuthentication(connectedUser);

        List<Book> favoriteBooks = favoriteRepository.findByUserId(user.getId())
                .stream()
                .map(Favorite::getBook)
                .collect(Collectors.toList());

        return favoriteBooks.stream()
                .map(bookMapper::toBookResponse)
                .collect(Collectors.toList());
    }

    public void addToFavorites(Long bookId, Authentication connectedUser) {
        User user = getUserFromAuthentication(connectedUser);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with ID: " + bookId));

        // Check if the book is already in favorites
        if (favoriteRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            return; // Already favorited, just return
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .book(book)
                .build();

        favoriteRepository.save(favorite);

        // ADD THIS: Log the favorite activity to dashboard
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.FAVORITE_BOOK,
                book.getId(),
                book.getTitle()
        );

        log.info("Book '{}' added to favorites by user {}", book.getTitle(), user.getEmail());
    }

    public void removeFromFavorites(Long bookId, Authentication connectedUser) {
        User user = getUserFromAuthentication(connectedUser);

        // Get the book for logging before deleting
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with ID: " + bookId));

        Favorite favorite = favoriteRepository.findByUserIdAndBookId(user.getId(), bookId)
                .orElseThrow(() -> new EntityNotFoundException("Favorite not found"));

        favoriteRepository.delete(favorite);

        // ADD THIS: Log the unfavorite activity to dashboard
        dashboardService.logBookActivity(
                DashboardAuditLog.ActionType.UNFAVORITE_BOOK,
                book.getId(),
                book.getTitle()
        );

        log.info("Book '{}' removed from favorites by user {}", book.getTitle(), user.getEmail());
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException("Authentication required");
        }

        // Extract the username from the JWT token
        String username = null;

        // This might be a JWT from Keycloak
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();

            // Try different claim names that Keycloak might use
            username = jwt.getClaimAsString("preferred_username");
            if (username == null) {
                username = jwt.getClaimAsString("email");
            }
            if (username == null) {
                username = jwt.getClaimAsString("sub");
            }
        } else {
            // Fallback to getName()
            username = authentication.getName();
        }

        if (username == null) {
            throw new BusinessException("Username not found in authentication");
        }

        // Check if user exists in the database, if not, create a new user
        Optional<User> existingUser = userRepository.findByEmail(username);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // User doesn't exist yet, create a new one based on Keycloak information
        User newUser = new User();
        newUser.setEmail(username);

        // Set other user properties if available in the token
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();

            // Extract additional user information if available
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");

            if (firstName != null) {
                newUser.setFirstname(firstName);
            }

            if (lastName != null) {
                newUser.setLastname(lastName);
            }
        }

        // Save the new user
        return userRepository.save(newUser);
    }
}