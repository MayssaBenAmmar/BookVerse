package com.alibou.book.recommend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationFeedback {
    private Long userId;
    private Long bookId;
    private String action; // "liked", "disliked", "not_interested", "saved"
    private Double rating;
    private String comment;
    private Long recommendationId;
}