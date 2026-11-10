import { Component, DestroyRef, OnInit, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { AuthService } from '../../core/services/auth.service';
import {
  DashboardService,
  DashboardSummary,
  DashboardKpis,
  TodayClassItem,
  RecentTransactionItem,
  DebtorItem,
  MonthlyRevenueChartItem,
} from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  // Tên giáo viên đăng nhập thực tế
  teacherName = computed<string>(() => {
    return this.authService.currentUser()?.fullName || 'Thầy/Cô';
  });

  // Dynamic Dashboard Data loaded from Database API
  summary = signal<DashboardSummary | null>(null);

  // Reminded debtor local state
  remindedList = signal<string[]>([]);

  // Computed properties
  kpis = computed<DashboardKpis>(() => {
    return (
      this.summary()?.kpis ?? {
        expectedTuition: 0,
        actualTuition: 0,
        unpaidTuition: 0,
        attendanceRate: 0,
        totalStudents: 0,
        totalClasses: 0,
        unpaidCount: 0,
        paidCount: 0,
        progressPercent: 0,
        attendedCount: 0,
        permittedLeaveCount: 0,
        unpermittedLeaveCount: 0,
      }
    );
  });

  todayClasses = computed<TodayClassItem[]>(() => {
    return this.summary()?.todayClasses ?? [];
  });

  recentTransactions = computed<RecentTransactionItem[]>(() => {
    return this.summary()?.recentTransactions ?? [];
  });

  debtors = computed<DebtorItem[]>(() => {
    return this.summary()?.debtors ?? [];
  });

  monthlyChart = computed<MonthlyRevenueChartItem[]>(() => {
    return this.summary()?.monthlyChart ?? [];
  });

  maxChartAmount = computed<number>(() => {
    const list = this.monthlyChart();
    if (!list || list.length === 0) return 1000000;
    const max = Math.max(...list.map(m => Math.max(m.expectedAmount || 0, m.actualAmount || 0)));
    return max > 0 ? max : 1000000;
  });

  total6MonthActual = computed<number>(() => {
    return this.monthlyChart().reduce((sum, item) => sum + (item.actualAmount || 0), 0);
  });

  avgCollectionRate = computed<number>(() => {
    const list = this.monthlyChart().filter(m => (m.expectedAmount || 0) > 0);
    if (list.length === 0) return 0;
    const totalExpected = list.reduce((sum, item) => sum + (item.expectedAmount || 0), 0);
    const totalActual = list.reduce((sum, item) => sum + (item.actualAmount || 0), 0);
    return totalExpected > 0 ? Math.round((totalActual / totalExpected * 100) * 10) / 10 : 0;
  });

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.dashboardService
      .getDashboardSummary()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res?.success && res.data) {
            this.summary.set(res.data);
          } else {
            this.summary.set(null);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.summary.set(null);
        },
      });
  }

  money(amount: number | null | undefined): string {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' đ';
  }

  getBarHeight(amount: number | null | undefined): number {
    const max = this.maxChartAmount();
    if (!amount || max <= 0) return 4;
    const pct = Math.round((amount / max) * 100);
    return Math.max(4, Math.min(100, pct));
  }

  remind(name: string): void {
    this.remindedList.update((items) =>
      items.includes(name) ? items : [...items, name]
    );
  }

  isReminded(name: string): boolean {
    return this.remindedList().includes(name);
  }
}
