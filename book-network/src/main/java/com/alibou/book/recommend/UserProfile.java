package com.alibou.book.recommend;

import com.alibou.book.book.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private Long userId;
    private Set<Genre> preferredGenres;
    private Set<String> preferredAuthors;
    private double averageRatingPreference;
    private Map<Genre, Double> genreWeights;
    private Map<String, Double> authorWeights;
    private int totalBooksRead;
}