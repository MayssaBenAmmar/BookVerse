// src/app/pages/dashboard/dashboard.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import {
  DashboardService,
  DashboardResponse,
  DashboardStats,
  ChartData,
  ProgressData,
  AuditLogResponse
} from 'src/app/services/dashboard.service';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {

  // Dashboard data
  dashboardData: DashboardResponse | null = null;
  stats: DashboardStats | null = null;
  progressBars: ProgressData[] = [];
  recentActivities: AuditLogResponse[] = [];

  // Loading states
  isLoading = true;
  isRefreshing = false;

  // Chart (only monthly now)
  monthlyChart: Chart | null = null;

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    // Clean up chart
    if (this.monthlyChart) {
      this.monthlyChart.destroy();
    }
  }

  loadDashboardData(): void {
    this.isLoading = true;

    this.dashboardService.getDashboardData().subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.stats = data.stats;
        this.progressBars = data.progressBars;
        this.recentActivities = data.recentActivities;

        // Create monthly chart after data is loaded
        setTimeout(() => {
          this.createMonthlyChart(data.monthlyBookActivity);
        }, 100);

        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);
        this.isLoading = false;
      }
    });
  }

  refreshDashboard(): void {
    this.isRefreshing = true;

    this.dashboardService.refreshDashboard().subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.stats = data.stats;
        this.progressBars = data.progressBars;
        this.recentActivities = data.recentActivities;

        // Update monthly chart
        this.updateMonthlyChart(data.monthlyBookActivity);
        this.isRefreshing = false;
      },
      error: (error) => {
        console.error('Error refreshing dashboard:', error);
        this.isRefreshing = false;
      }
    });
  }

  private createMonthlyChart(data: ChartData[]): void {
    const ctx = document.getElementById('monthlyChart') as HTMLCanvasElement;
    if (!ctx) return;

    // Destroy existing chart
    if (this.monthlyChart) {
      this.monthlyChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'bar' as ChartType,
      data: {
        labels: data.map(d => d.label),
        datasets: [
          {
            label: 'Created',
            data: data.map(d => d.created),
            backgroundColor: 'rgba(59, 130, 246, 0.8)',
            borderColor: 'rgb(59, 130, 246)',
            borderWidth: 1
          },
          {
            label: 'Favorites',
            data: data.map(d => d.favorites),
            backgroundColor: 'rgba(239, 68, 68, 0.8)',
            borderColor: 'rgb(239, 68, 68)',
            borderWidth: 1
          },
          {
            label: 'Archived',
            data: data.map(d => d.archived),
            backgroundColor: 'rgba(245, 158, 11, 0.8)',
            borderColor: 'rgb(245, 158, 11)',
            borderWidth: 1
          },
          {
            label: 'Borrowed',
            data: data.map(d => d.borrowed),
            backgroundColor: 'rgba(34, 197, 94, 0.8)',
            borderColor: 'rgb(34, 197, 94)',
            borderWidth: 1
          },
          {
            label: 'Returned',
            data: data.map(d => d.returned),
            backgroundColor: 'rgba(168, 85, 247, 0.8)',
            borderColor: 'rgb(168, 85, 247)',
            borderWidth: 1
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Monthly Book Activity'
          },
          legend: {
            position: 'top'
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              stepSize: 1
            }
          }
        }
      }
    };

    this.monthlyChart = new Chart(ctx, config);
  }

  private updateMonthlyChart(monthlyData: ChartData[]): void {
    if (this.monthlyChart) {
      this.monthlyChart.data.labels = monthlyData.map(d => d.label);
      this.monthlyChart.data.datasets[0].data = monthlyData.map(d => d.created);
      this.monthlyChart.data.datasets[1].data = monthlyData.map(d => d.favorites);
      this.monthlyChart.data.datasets[2].data = monthlyData.map(d => d.archived);
      this.monthlyChart.data.datasets[3].data = monthlyData.map(d => d.borrowed);
      this.monthlyChart.data.datasets[4].data = monthlyData.map(d => d.returned);
      this.monthlyChart.update();
    }
  }

  getProgressBarColor(color: string): string {
    const colors: { [key: string]: string } = {
      'blue': 'bg-blue-500',
      'red': 'bg-red-500',
      'green': 'bg-green-500',
      'orange': 'bg-orange-500',
      'purple': 'bg-purple-500'
    };
    return colors[color] || 'bg-gray-500';
  }

  getIconClass(icon: string): string {
    const icons: { [key: string]: string } = {
      'book': 'fas fa-book',
      'heart': 'fas fa-heart',
      'check': 'fas fa-check',
      'activity': 'fas fa-chart-line'
    };
    return icons[icon] || 'fas fa-info';
  }
}
