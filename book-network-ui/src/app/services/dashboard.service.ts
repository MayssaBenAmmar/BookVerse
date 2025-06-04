// src/app/services/dashboard.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

// Dashboard DTOs - matching your backend
export interface DashboardResponse {
  stats: DashboardStats;
  monthlyBookActivity: ChartData[];
  dailyActivity: ChartData[];
  progressBars: ProgressData[];
  recentActivities: AuditLogResponse[];
}

export interface DashboardStats {
  totalBooks: number;
  createdBooks: number;
  favoritedBooks: number;
  archivedBooks: number;
  borrowedBooks: number;
  totalUsers: number;
  activeUsers: number;
  todayActivities: number;
}

export interface ChartData {
  label: string;
  created: number;
  favorites: number;
  archived: number;
  borrowed: number;
  returned: number;
  date?: string;
  month?: number;
  year?: number;
}

export interface ProgressData {
  title: string;
  percentage: number;
  description: string;
  color: string;
  icon: string;
}

export interface AuditLogResponse {
  id: number;
  username: string;
  action: string;
  entityType: string;
  entityName: string;
  details: string;
  timestamp: string;
  timeAgo: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  // Get complete dashboard data
  getDashboardData(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(this.apiUrl).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Error fetching dashboard data:', error);
        return throwError(() => new Error('Failed to load dashboard data'));
      })
    );
  }

  // Refresh dashboard data
  refreshDashboard(): Observable<DashboardResponse> {
    return this.http.post<DashboardResponse>(`${this.apiUrl}/refresh`, {}).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Error refreshing dashboard:', error);
        return throwError(() => new Error('Failed to refresh dashboard'));
      })
    );
  }
}
