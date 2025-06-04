package com.alibou.book.dashboard;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dashboard_audit_logs")
@EntityListeners(AuditingEntityListener.class)
public class DashboardAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType action;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name")
    private String entityName;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address")
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // All possible actions in your book social network
    public enum ActionType {
        // Book actions
        CREATE_BOOK,
        UPDATE_BOOK,
        DELETE_BOOK,
        VIEW_BOOK,
        BORROW_BOOK,
        RETURN_BOOK,
        FAVORITE_BOOK,
        UNFAVORITE_BOOK,
        ARCHIVE_BOOK,
        UNARCHIVE_BOOK,
        RATE_BOOK,

        // User actions
        LOGIN,
        LOGOUT,
        REGISTER,
        UPDATE_PROFILE,

        // System actions
        SEARCH,
        EXPORT_DATA
    }

    public enum EntityType {
        BOOK,
        USER,
        FEEDBACK,
        BORROWING_RECORD,
        AUTHENTICATION,
        SYSTEM
    }
}