package com.alibou.book.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartData {
    private String label;      // Month name, date, or category
    private long created;      // Created books/activities
    private long favorites;    // Favorited books
    private long archived;     // Archived books
    private long borrowed;     // Borrowed books
    private long returned;     // Returned books
    private LocalDate date;    // For daily charts
    private int month;         // For monthly charts
    private int year;          // For monthly charts
}