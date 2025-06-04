package com.alibou.book.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private long totalBooks;
    private long createdBooks;
    private long favoritedBooks;
    private long archivedBooks;
    private long borrowedBooks;
    private long totalUsers;
    private long activeUsers;
    private long todayActivities;
}