package com.alibou.book.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String username;
    private String action;
    private String entityType;
    private String entityName;
    private String details;
    private LocalDateTime timestamp;
    private String timeAgo;        // e.g., "2 hours ago"
}