package com.alibou.book.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookResponse {
    private Long id;
    private String title;
    private String authorName;
    private String isbn;
    private String synopsis;
    private String cover;
    private Boolean archived;
    private Boolean shareable;
    private Double rate;
    private String owner;  // This is the username string
    private Long ownerId;  // Added field for owner's ID
    private Genre genre;   // Added genre field
}