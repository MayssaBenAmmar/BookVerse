package com.alibou.book.dashboard;

import com.alibou.book.book.BookRepository;
import com.alibou.book.user.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final BookRepository bookRepository;

    // ================= ENHANCED AUDIT LOGGING =================

    public void logActivity(DashboardAuditLog.ActionType action,
                            DashboardAuditLog.EntityType entityType,
                            Long entityId,
                            String entityName,
                            String details) {
        try {
            log.info("🔄 Attempting to log activity: {} - {} - {}", action, entityType, entityName);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            HttpServletRequest request = getCurrentRequest();

            String userId = getUserId(auth);
            String username = getUsername(auth);

            log.info("📝 User info - ID: {}, Username: {}", userId, username);

            DashboardAuditLog auditLog = DashboardAuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .details(details)
                    .ipAddress(getClientIP(request))
                    .build();

            DashboardAuditLog saved = dashboardRepository.save(auditLog);
            log.info("✅ Activity logged successfully with ID: {}", saved.getId());

        } catch (Exception e) {
            log.error("❌ Failed to log activity: {}", e.getMessage(), e);
        }
    }

    // ================= CONVENIENCE METHODS FOR BOOK ACTIVITIES =================

    /**
     * Log book creation
     */
    public void logBookCreated(Long bookId, String bookTitle, String authorName) {
        String details = String.format("New book '%s' by %s added to the library", bookTitle, authorName);
        logActivity(DashboardAuditLog.ActionType.CREATE_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log book update
     */
    public void logBookUpdated(Long bookId, String bookTitle, String changes) {
        String details = String.format("Book '%s' updated: %s", bookTitle, changes);
        logActivity(DashboardAuditLog.ActionType.UPDATE_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log book deletion
     */
    public void logBookDeleted(Long bookId, String bookTitle) {
        String details = String.format("Book '%s' removed from the library", bookTitle);
        logActivity(DashboardAuditLog.ActionType.DELETE_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log book favorited
     */
    public void logBookFavorited(Long bookId, String bookTitle) {
        String details = String.format("Added '%s' to favorites", bookTitle);
        logActivity(DashboardAuditLog.ActionType.FAVORITE_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log book unfavorited
     */
    public void logBookUnfavorited(Long bookId, String bookTitle) {
        String details = String.format("Removed '%s' from favorites", bookTitle);
        logActivity(DashboardAuditLog.ActionType.UNFAVORITE_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log book archived
     */
    public void logBookArchived(Long bookId, String bookTitle) {
        String details = String.format("Archived book '%s'", bookTitle);
        logActivity(DashboardAuditLog.ActionType.ARCHIVE_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log book unarchived
     */
    public void logBookUnarchived(Long bookId, String bookTitle) {
        String details = String.format("Unarchived book '%s'", bookTitle);
        logActivity(DashboardAuditLog.ActionType.UNARCHIVE_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log book borrowed
     */
    public void logBookBorrowed(Long bookId, String bookTitle, String borrowerName) {
        String details = String.format("'%s' borrowed by %s", bookTitle, borrowerName);
        logActivity(DashboardAuditLog.ActionType.BORROW_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log book returned
     */
    public void logBookReturned(Long bookId, String bookTitle, String returnerName) {
        String details = String.format("'%s' returned by %s", bookTitle, returnerName);
        logActivity(DashboardAuditLog.ActionType.RETURN_BOOK,
                DashboardAuditLog.EntityType.BOOK,
                bookId, bookTitle, details);
    }

    /**
     * Log user login
     */
    public void logUserLogin(String username) {
        String details = String.format("User %s logged in successfully", username);
        logActivity(DashboardAuditLog.ActionType.LOGIN,
                DashboardAuditLog.EntityType.USER,
                null, username, details);
    }

    /**
     * Log user logout
     */
    public void logUserLogout(String username) {
        String details = String.format("User %s logged out", username);
        logActivity(DashboardAuditLog.ActionType.LOGOUT,
                DashboardAuditLog.EntityType.USER,
                null, username, details);
    }

    // ================= LEGACY METHODS (KEEP FOR COMPATIBILITY) =================

    public void logActivity(DashboardAuditLog.ActionType action) {
        logActivity(action, DashboardAuditLog.EntityType.SYSTEM, null, null, null);
    }

    public void logBookActivity(DashboardAuditLog.ActionType action, Long bookId, String bookTitle) {
        logActivity(action, DashboardAuditLog.EntityType.BOOK, bookId, bookTitle, null);
    }

    // ================= MAIN DASHBOARD DATA =================

    public DashboardResponse getDashboardData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime last30Days = now.minusDays(30);
        LocalDateTime last6Months = now.minusMonths(6);

        return DashboardResponse.builder()
                .stats(getDashboardStats(startOfToday, last30Days))
                .monthlyBookActivity(getMonthlyBookActivity(last6Months))
                .progressBars(getProgressBars())
                .recentActivities(getRecentActivities())
                .build();
    }

    public DashboardResponse getDashboardDataSafe() {
        try {
            log.info("Getting dashboard data safely...");

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfToday = now.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime last30Days = now.minusDays(30);
            LocalDateTime last6Months = now.minusMonths(6);

            return DashboardResponse.builder()
                    .stats(getDashboardStatsSafe(startOfToday, last30Days))
                    .monthlyBookActivity(getMonthlyBookActivitySafe(last6Months))
                    .progressBars(getProgressBarsSafe())
                    .recentActivities(getRecentActivitiesSafe())
                    .build();

        } catch (Exception e) {
            log.error("Error in getDashboardDataSafe", e);
            throw e;
        }
    }

    // ================= STATISTICS CARDS =================

    private DashboardStats getDashboardStats(LocalDateTime startOfToday, LocalDateTime last30Days) {
        List<Object[]> actionCounts = dashboardRepository.countActivitiesByAction(last30Days);
        Map<DashboardAuditLog.ActionType, Long> actionMap = actionCounts.stream()
                .collect(Collectors.toMap(
                        obj -> (DashboardAuditLog.ActionType) obj[0],
                        obj -> (Long) obj[1]
                ));

        // NET CALCULATIONS: Positive actions - Negative actions
        long createCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.CREATE_BOOK, 0L);
        long deleteCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.DELETE_BOOK, 0L);
        long netCreatedBooks = Math.max(0, createCount - deleteCount);

        long favoriteCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.FAVORITE_BOOK, 0L);
        long unfavoriteCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.UNFAVORITE_BOOK, 0L);
        long netFavoritedBooks = Math.max(0, favoriteCount - unfavoriteCount);

        long archiveCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.ARCHIVE_BOOK, 0L);
        long unarchiveCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.UNARCHIVE_BOOK, 0L);
        long netArchivedBooks = Math.max(0, archiveCount - unarchiveCount);

        long borrowCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.BORROW_BOOK, 0L);
        long returnCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.RETURN_BOOK, 0L);
        long netBorrowedBooks = Math.max(0, borrowCount - returnCount);

        return DashboardStats.builder()
                .totalBooks(bookRepository.count())
                .createdBooks(netCreatedBooks)
                .favoritedBooks(netFavoritedBooks)
                .archivedBooks(netArchivedBooks)
                .borrowedBooks(netBorrowedBooks)
                .totalUsers(countUniqueUsers())
                .activeUsers(countActiveUsers(last30Days))
                .todayActivities(dashboardRepository.countTodayActivities(startOfToday))
                .build();
    }

    private DashboardStats getDashboardStatsSafe(LocalDateTime startOfToday, LocalDateTime last30Days) {
        try {
            log.info("Getting dashboard stats safely...");

            long totalBooks = 0;
            try {
                totalBooks = bookRepository.count();
                log.info("Total books: {}", totalBooks);
            } catch (Exception e) {
                log.warn("Could not get book count", e);
            }

            long todayActivities = 0;
            long createdBooks = 0;
            long favoritedBooks = 0;
            long archivedBooks = 0;
            long borrowedBooks = 0;

            try {
                todayActivities = dashboardRepository.countTodayActivities(startOfToday);
                log.info("Today's activities: {}", todayActivities);
            } catch (Exception e) {
                log.warn("Could not get today's activities", e);
            }

            try {
                List<Object[]> actionCounts = dashboardRepository.countActivitiesByAction(last30Days);
                log.info("Found {} action count records", actionCounts.size());

                Map<DashboardAuditLog.ActionType, Long> actionMap = actionCounts.stream()
                        .collect(Collectors.toMap(
                                obj -> (DashboardAuditLog.ActionType) obj[0],
                                obj -> (Long) obj[1],
                                (existing, replacement) -> existing
                        ));

                // NET CALCULATIONS: Positive actions - Negative actions
                long createCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.CREATE_BOOK, 0L);
                long deleteCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.DELETE_BOOK, 0L);
                createdBooks = Math.max(0, createCount - deleteCount);

                long favoriteCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.FAVORITE_BOOK, 0L);
                long unfavoriteCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.UNFAVORITE_BOOK, 0L);
                favoritedBooks = Math.max(0, favoriteCount - unfavoriteCount);

                long archiveCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.ARCHIVE_BOOK, 0L);
                long unarchiveCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.UNARCHIVE_BOOK, 0L);
                archivedBooks = Math.max(0, archiveCount - unarchiveCount);

                long borrowCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.BORROW_BOOK, 0L);
                long returnCount = actionMap.getOrDefault(DashboardAuditLog.ActionType.RETURN_BOOK, 0L);
                borrowedBooks = Math.max(0, borrowCount - returnCount);

                log.info("NET Action counts - Created: {} ({}−{}), Favorited: {} ({}−{}), Archived: {} ({}−{}), Borrowed: {} ({}−{})",
                        createdBooks, createCount, deleteCount,
                        favoritedBooks, favoriteCount, unfavoriteCount,
                        archivedBooks, archiveCount, unarchiveCount,
                        borrowedBooks, borrowCount, returnCount);

            } catch (Exception e) {
                log.warn("Could not get action counts", e);
            }

            return DashboardStats.builder()
                    .totalBooks(totalBooks)
                    .createdBooks(createdBooks)
                    .favoritedBooks(favoritedBooks)
                    .archivedBooks(archivedBooks)
                    .borrowedBooks(borrowedBooks)
                    .totalUsers(countUniqueUsersSafe())
                    .activeUsers(countActiveUsersSafe(last30Days))
                    .todayActivities(todayActivities)
                    .build();

        } catch (Exception e) {
            log.error("Error getting dashboard stats", e);
            return DashboardStats.builder()
                    .totalBooks(0L).createdBooks(0L).favoritedBooks(0L)
                    .archivedBooks(0L).borrowedBooks(0L).totalUsers(0L)
                    .activeUsers(0L).todayActivities(0L)
                    .build();
        }
    }

    // ================= PROGRESS BARS =================

    private List<ProgressData> getProgressBars() {
        String currentUserId = getCurrentUserId();
        long totalBooks = bookRepository.count();

        long userCreatedBooks = getUserNetActionCount(currentUserId,
                DashboardAuditLog.ActionType.CREATE_BOOK,
                DashboardAuditLog.ActionType.DELETE_BOOK);

        long userFavoritedBooks = getUserNetActionCount(currentUserId,
                DashboardAuditLog.ActionType.FAVORITE_BOOK,
                DashboardAuditLog.ActionType.UNFAVORITE_BOOK);

        long userBorrowedBooks = getUserNetActionCount(currentUserId,
                DashboardAuditLog.ActionType.BORROW_BOOK,
                DashboardAuditLog.ActionType.RETURN_BOOK);

        List<ProgressData> progressBars = new ArrayList<>();

        progressBars.add(ProgressData.builder()
                .title("Books Contribution")
                .percentage(totalBooks > 0 ? (userCreatedBooks * 100.0) / totalBooks : 0.0)
                .description(userCreatedBooks + " of " + totalBooks + " books in the system")
                .color("blue")
                .icon("book")
                .build());

        progressBars.add(ProgressData.builder()
                .title("Favorites Ratio")
                .percentage(totalBooks > 0 ? (userFavoritedBooks * 100.0) / totalBooks : 0.0)
                .description(userFavoritedBooks + " books currently favorited")
                .color("red")
                .icon("heart")
                .build());

        progressBars.add(ProgressData.builder()
                .title("Reading Activity")
                .percentage(totalBooks > 0 ? (userBorrowedBooks * 100.0) / totalBooks : 0.0)
                .description(userBorrowedBooks + " books currently borrowed")
                .color("green")
                .icon("check")
                .build());

        long recentActivities = dashboardRepository.countTodayActivities(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        progressBars.add(ProgressData.builder()
                .title("Activity Level")
                .percentage(Math.min(recentActivities * 10.0, 100.0))
                .description("Your recent activity compared to other users")
                .color("orange")
                .icon("activity")
                .build());

        return progressBars;
    }

    private long getUserNetActionCount(String userId, DashboardAuditLog.ActionType positiveAction, DashboardAuditLog.ActionType negativeAction) {
        try {
            long positiveCount = dashboardRepository.countUserActionsByType(userId, positiveAction);
            long negativeCount = dashboardRepository.countUserActionsByType(userId, negativeAction);
            return Math.max(0, positiveCount - negativeCount);
        } catch (Exception e) {
            log.warn("Could not calculate net action count for {} - {} vs {}", userId, positiveAction, negativeAction, e);
            return 0;
        }
    }

    // ================= SAFE HELPER METHODS =================

    private List<ChartData> getMonthlyBookActivitySafe(LocalDateTime last6Months) {
        try {
            log.info("Getting monthly book activity safely...");
            return getMonthlyBookActivity(last6Months);
        } catch (Exception e) {
            log.warn("Could not get monthly activity, returning empty list", e);
            return new ArrayList<>();
        }
    }

    private List<ProgressData> getProgressBarsSafe() {
        try {
            log.info("Getting progress bars safely...");
            return getProgressBars();
        } catch (Exception e) {
            log.warn("Could not get progress bars, returning empty list", e);
            return new ArrayList<>();
        }
    }

    private List<AuditLogResponse> getRecentActivitiesSafe() {
        try {
            log.info("Getting recent activities safely...");
            return getRecentActivities();
        } catch (Exception e) {
            log.warn("Could not get recent activities, returning empty list", e);
            return new ArrayList<>();
        }
    }

    private long countUniqueUsersSafe() {
        try {
            List<Object[]> users = dashboardRepository.getMostActiveUsers(
                    LocalDateTime.now().minusMonths(12),
                    PageRequest.of(0, 1000)
            );
            long count = users.size();
            log.info("Unique users count: {}", count);
            return count;
        } catch (Exception e) {
            log.warn("Could not count unique users", e);
            return 0;
        }
    }

    private long countActiveUsersSafe(LocalDateTime since) {
        try {
            List<Object[]> activeUsers = dashboardRepository.getMostActiveUsers(since, PageRequest.of(0, 100));
            long count = activeUsers.size();
            log.info("Active users count: {}", count);
            return count;
        } catch (Exception e) {
            log.warn("Could not count active users", e);
            return 0;
        }
    }

    // ================= MONTHLY CHART DATA =================

    private List<ChartData> getMonthlyBookActivity(LocalDateTime last6Months) {
        List<Object[]> monthlyData = dashboardRepository.getMonthlyBookActivities(last6Months);

        Map<String, Map<DashboardAuditLog.ActionType, Long>> monthlyMap = new HashMap<>();

        for (Object[] data : monthlyData) {
            int month = (Integer) data[0];
            int year = (Integer) data[1];
            DashboardAuditLog.ActionType action = (DashboardAuditLog.ActionType) data[2];
            Long count = (Long) data[3];

            String monthKey = year + "-" + String.format("%02d", month);
            monthlyMap.computeIfAbsent(monthKey, k -> new HashMap<>()).put(action, count);
        }

        return monthlyMap.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    Map<DashboardAuditLog.ActionType, Long> actions = entry.getValue();

                    return ChartData.builder()
                            .label(Month.of(month).name().substring(0, 3) + " " + year)
                            .month(month)
                            .year(year)
                            .created(actions.getOrDefault(DashboardAuditLog.ActionType.CREATE_BOOK, 0L))
                            .favorites(actions.getOrDefault(DashboardAuditLog.ActionType.FAVORITE_BOOK, 0L))
                            .archived(actions.getOrDefault(DashboardAuditLog.ActionType.ARCHIVE_BOOK, 0L))
                            .borrowed(actions.getOrDefault(DashboardAuditLog.ActionType.BORROW_BOOK, 0L))
                            .returned(actions.getOrDefault(DashboardAuditLog.ActionType.RETURN_BOOK, 0L))
                            .build();
                })
                .sorted((a, b) -> {
                    if (a.getYear() != b.getYear()) return Integer.compare(a.getYear(), b.getYear());
                    return Integer.compare(a.getMonth(), b.getMonth());
                })
                .collect(Collectors.toList());
    }

    // ================= RECENT ACTIVITIES =================

    private List<AuditLogResponse> getRecentActivities() {
        Page<DashboardAuditLog> recentLogs = dashboardRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 15));

        return recentLogs.getContent().stream()
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .username(log.getUsername())
                        .action(formatAction(log.getAction()))
                        .entityType(log.getEntityType().toString())
                        .entityName(log.getEntityName())
                        .details(log.getDetails())
                        .timestamp(log.getCreatedAt())
                        .timeAgo(getTimeAgo(log.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    // ================= HELPER METHODS =================

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    // CORRECTED: Fixed user identification
    private String getUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User) {
                return ((User) principal).getId().toString(); // Use actual user ID
            }
            return auth.getName();
        }
        return "anonymous";
    }

    // CORRECTED: Fixed username extraction
    private String getUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                return user.getFirstname() + " " + user.getLastname();
            }
            return auth.getName();
        }
        return "Anonymous User";
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getUserId(auth);
    }

    private String getClientIP(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private long countUniqueUsers() {
        return dashboardRepository.getMostActiveUsers(
                LocalDateTime.now().minusMonths(12),
                PageRequest.of(0, 1000)
        ).size();
    }

    private long countActiveUsers(LocalDateTime since) {
        return dashboardRepository.getMostActiveUsers(since, PageRequest.of(0, 1000)).size();
    }

    // CORRECTED: Better action formatting
    private String formatAction(DashboardAuditLog.ActionType action) {
        switch (action) {
            case CREATE_BOOK: return "created book";
            case UPDATE_BOOK: return "updated book";
            case DELETE_BOOK: return "deleted book";
            case FAVORITE_BOOK: return "favorited book";
            case UNFAVORITE_BOOK: return "unfavorited book";
            case ARCHIVE_BOOK: return "archived book";
            case UNARCHIVE_BOOK: return "unarchived book";
            case BORROW_BOOK: return "borrowed book";
            case RETURN_BOOK: return "returned book";
            case LOGIN: return "logged in";
            case LOGOUT: return "logged out";
            case REGISTER: return "registered";
            case UPDATE_PROFILE: return "updated profile";
            case RATE_BOOK: return "rated book";
            case VIEW_BOOK: return "viewed book";
            case SEARCH: return "searched";
            case EXPORT_DATA: return "exported data";
            default: return action.toString().replace("_", " ").toLowerCase();
        }
    }

    // CORRECTED: Better time formatting
    private String getTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";

        long weeks = days / 7;
        if (weeks < 4) return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";

        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
}