package com.alibou.book.favorite;

import com.alibou.book.book.Book;
import com.alibou.book.book.BookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorites")  // explicit API prefix
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{bookId}")
    public ResponseEntity<Void> addFavorite(@PathVariable Long bookId, Authentication connectedUser) {
        favoriteService.addToFavorites(bookId, connectedUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long bookId, Authentication connectedUser) {
        favoriteService.removeFromFavorites(bookId, connectedUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> getFavorites(Authentication connectedUser) {
        return ResponseEntity.ok(favoriteService.getUserFavorites(connectedUser));
    }
}