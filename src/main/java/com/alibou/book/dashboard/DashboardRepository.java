package com.alibou.book.dashboard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<DashboardAuditLog, Long> {

    // Recent activities for audit log section
    Page<DashboardAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Count activities for today (for stats cards)
    @Query("SELECT COUNT(d) FROM DashboardAuditLog d WHERE d.createdAt >= :startOfDay")
    Long countTodayActivities(@Param("startOfDay") LocalDateTime startOfDay);

    // Count total activities by action type (for stats cards)
    @Query("SELECT d.action, COUNT(d) FROM DashboardAuditLog d WHERE d.createdAt >= :startDate GROUP BY d.action")
    List<Object[]> countActivitiesByAction(@Param("startDate") LocalDateTime startDate);

    // Monthly activities by action type for bar chart (last 6 months)
    @Query("SELECT MONTH(d.createdAt), YEAR(d.createdAt), d.action, COUNT(d) " +
            "FROM DashboardAuditLog d " +
            "WHERE d.createdAt >= :startDate AND d.entityType = 'BOOK' " +
            "GROUP BY MONTH(d.createdAt), YEAR(d.createdAt), d.action " +
            "ORDER BY YEAR(d.createdAt), MONTH(d.createdAt)")
    List<Object[]> getMonthlyBookActivities(@Param("startDate") LocalDateTime startDate);

    // Count specific book actions for progress bars
    @Query("SELECT COUNT(d) FROM DashboardAuditLog d WHERE d.action = :action AND d.createdAt >= :startDate")
    Long countByAction(@Param("action") DashboardAuditLog.ActionType action, @Param("startDate") LocalDateTime startDate);

    // Count activities by user (for most active users)
    @Query("SELECT d.username, COUNT(d) FROM DashboardAuditLog d " +
            "WHERE d.createdAt >= :startDate " +
            "GROUP BY d.username " +
            "ORDER BY COUNT(d) DESC")
    List<Object[]> getMostActiveUsers(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    // Get user's own activities for personal progress
    @Query("SELECT COUNT(d) FROM DashboardAuditLog d WHERE d.userId = :userId AND d.action = :action")
    Long countUserActionsByType(@Param("userId") String userId, @Param("action") DashboardAuditLog.ActionType action);
}