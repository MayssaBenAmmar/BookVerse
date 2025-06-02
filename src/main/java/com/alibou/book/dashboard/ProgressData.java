package com.alibou.book.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressData {
    private String title;           // e.g., "Books Contribution"
    private double percentage;      // e.g., 0.0 for 0%
    private String description;     // e.g., "0 of 38 books in the system"
    private String color;          // e.g., "blue", "green", "red", "orange"
    private String icon;           // e.g., "book", "heart", "check", "activity"
}