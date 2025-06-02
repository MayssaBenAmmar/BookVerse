package com.alibou.book.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private DashboardStats stats;
    private List<ChartData> monthlyBookActivity;
    private List<ProgressData> progressBars;
    private List<AuditLogResponse> recentActivities;
}