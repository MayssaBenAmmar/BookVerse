package com.alibou.book.recommend;

import com.alibou.book.book.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    @JsonProperty("book_id")
    private Long bookId;
    private double score;
    private String title;
    private String authorName;
    private String cover;
    private Double rate;
    private Genre genre;
    private String description;
    private Integer pageCount;

    // AI-related fields for enhanced recommendations
    private String recommendationReason;
    private Double relevanceScore;
    private String[] similarBooks;
}