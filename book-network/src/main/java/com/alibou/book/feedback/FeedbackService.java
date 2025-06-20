package com.alibou.book.feedback;

import com.alibou.book.book.Book;
import com.alibou.book.book.BookRepository;
import com.alibou.book.common.PageResponse;
import com.alibou.book.exception.OperationNotPermittedException;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedBackRepository feedBackRepository;
    private final BookRepository bookRepository;
    private final FeedbackMapper feedbackMapper;
    private final UserRepository userRepository;

    public Long save(FeedbackRequest request, Authentication connectedUser) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:: " + request.bookId()));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("You cannot give a feedback for an archived or non-shareable book");
        }

        if (Objects.equals(book.getCreatedBy(), connectedUser.getName())) {
            throw new OperationNotPermittedException("You cannot give feedback to your own book");
        }

        Feedback feedback = feedbackMapper.toFeedback(request);
        return feedBackRepository.save(feedback).getId();
    }

    @Transactional
    public PageResponse<FeedbackResponse> findAllFeedbacksByBook(Long bookId, int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size);
        User user = getUserFromAuthentication(connectedUser);

        Page<Feedback> feedbacks = feedBackRepository.findAllByBookId(bookId, pageable);
        List<FeedbackResponse> feedbackResponses = feedbacks.stream()
                .map(f -> feedbackMapper.toFeedbackResponse(f, user.getId().longValue()))
                .toList();

        return new PageResponse<>(
                feedbackResponses,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
    }

    private User getUserFromAuthentication(Authentication authentication) {
        String username = authentication.getName();

        // Try to find existing user
        return userRepository.findByEmail(username)
                .orElseGet(() -> {
                    // Create new user if doesn't exist
                    User newUser = new User();
                    newUser.setEmail(username);

                    // Extract additional info from JWT if available
                    if (authentication.getPrincipal() instanceof Jwt) {
                        Jwt jwt = (Jwt) authentication.getPrincipal();
                        String firstName = jwt.getClaimAsString("given_name");
                        String lastName = jwt.getClaimAsString("family_name");

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
}