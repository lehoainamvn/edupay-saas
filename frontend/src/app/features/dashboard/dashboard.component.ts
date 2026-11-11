import { Component, DestroyRef, OnInit, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { AuthService } from '../../core/services/auth.service';
import { ClassService } from '../../core/services/class.service';
import { ClassItem } from '../../shared/models/class.model';
import {
  DashboardService,
  DashboardSummary,
  DashboardKpis,
  TodayClassItem,
  RecentTransactionItem,
  DebtorItem,
  MonthlyRevenueChartItem,
} from '../../core/services/dashboard.service';

export interface WeekDayItem {
  code: string;
  label: string;
  short: string;
}

export interface UpcomingClassInfo {
  dayLabel: string;
  classItem: TodayClassItem;
}

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
  private readonly classService = inject(ClassService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  // Tên giáo viên đăng nhập thực tế
  teacherName = computed<string>(() => {
    return this.authService.currentUser()?.fullName || 'Thầy/Cô';
  });

  // Thứ hôm nay bằng tiếng Việt (VD: Thứ Tư)
  currentWeekdayName = computed<string>(() => {
    const days = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
    return days[new Date().getDay()];
  });

  // Ngày tháng thực tế động
  currentDateStr = computed<string>(() => {
    const now = new Date();
    const dayName = this.currentWeekdayName();
    const day = String(now.getDate()).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const year = now.getFullYear();
    return `${dayName}, ngày ${day}/${month}/${year}`;
  });

  currentMonthLabel = computed<string>(() => {
    const now = new Date();
    return `THÁNG ${now.getMonth() + 1}/${now.getFullYear()}`;
  });

  // Dynamic Dashboard Data loaded from Database API
  summary = signal<DashboardSummary | null>(null);
  classesList = signal<ClassItem[]>([]);

  // Reminded debtor local state
  remindedList = signal<string[]>([]);
  remindSuccessMessage = signal<string | null>(null);

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

  readonly weekDays: WeekDayItem[] = [
    { code: 'MONDAY', label: 'Thứ 2', short: 'T2' },
    { code: 'TUESDAY', label: 'Thứ 3', short: 'T3' },
    { code: 'WEDNESDAY', label: 'Thứ 4', short: 'T4' },
    { code: 'THURSDAY', label: 'Thứ 5', short: 'T5' },
    { code: 'FRIDAY', label: 'Thứ 6', short: 'T6' },
    { code: 'SATURDAY', label: 'Thứ 7', short: 'T7' },
    { code: 'SUNDAY', label: 'Chủ Nhật', short: 'CN' }
  ];

  todayDayCode = computed<string>(() => {
    const days = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    return days[new Date().getDay()];
  });

  // Danh sách toàn bộ các lớp học (ưu tiên từ classesList, fallback summary)
  allClasses = computed<TodayClassItem[]>(() => {
    if (this.classesList().length > 0) {
      return this.classesList()
        .filter(c => c.isActive)
        .map(c => ({
          id: c.id,
          name: c.name,
          time: c.startTime || '17:30',
          end: c.endTime || '19:00',
          room: this.formatRoom(c.description),
          scheduleDays: c.scheduleDays || c.description || '',
          count: c.studentCount || 0,
          fee: c.defaultFee || 0,
          feeType: c.feeType || 'PER_LESSON',
          state: 'upcoming' as const,
          text: 'Sắp diễn ra'
        }));
    }

    if (this.summary()?.allClasses && this.summary()!.allClasses!.length > 0) {
      return this.summary()!.allClasses!;
    }

    return this.summary()?.todayClasses ?? [];
  });

  // Tự động kiểm tra một lớp có lịch dạy vào một thứ cụ thể hay không
  isClassOnDay(item: TodayClassItem, dayCode: string): boolean {
    const sched = (item.scheduleDays || item.room || '').toUpperCase();
    const name = (item.name || '').toUpperCase();
    const combined = `${sched} ${name}`;

    switch (dayCode) {
      case 'MONDAY':
        return combined.includes('MONDAY') || combined.includes('THỨ 2') || combined.includes('T2');
      case 'TUESDAY':
        return combined.includes('TUESDAY') || combined.includes('THỨ 3') || combined.includes('T3');
      case 'WEDNESDAY':
        return combined.includes('WEDNESDAY') || combined.includes('THỨ 4') || combined.includes('T4');
      case 'THURSDAY':
        return combined.includes('THURSDAY') || combined.includes('THỨ 5') || combined.includes('T5');
      case 'FRIDAY':
        return combined.includes('FRIDAY') || combined.includes('THỨ 6') || combined.includes('T6');
      case 'SATURDAY':
        return combined.includes('SATURDAY') || combined.includes('THỨ 7') || combined.includes('T7');
      case 'SUNDAY':
        return combined.includes('SUNDAY') || combined.includes('CHỦ NHẬT') || combined.includes('CN');
      default:
        return false;
    }
  }

  // Tự động lọc các lớp học có lịch ĐÚNG VÀO THỨ HÔM NAY
  todayClasses = computed<TodayClassItem[]>(() => {
    const list = this.allClasses();
    const todayCode = this.todayDayCode();

    return list.filter(item => {
      const sched = (item.scheduleDays || item.room || '').toLowerCase();
      // Nếu lớp không hề có thông tin lịch học nào thì mặc định không ép
      const hasSchedule = sched.includes('thứ') || sched.includes('chủ nhật') || sched.includes('cn') ||
                          sched.includes('monday') || sched.includes('tuesday') || sched.includes('wednesday') ||
                          sched.includes('thursday') || sched.includes('friday') || sched.includes('saturday') || sched.includes('sunday') ||
                          sched.includes('t2') || sched.includes('t3') || sched.includes('t4') || sched.includes('t5') || sched.includes('t6') || sched.includes('t7');
      if (!hasSchedule) {
        return true;
      }
      return this.isClassOnDay(item, todayCode);
    });
  });

  // Lấy các lớp cho từng cột ngày trong tuần
  getClassesForDay(dayCode: string): TodayClassItem[] {
    const list = this.allClasses();
    return list.filter(item => this.isClassOnDay(item, dayCode));
  }

  // Tìm ca dạy kế tiếp trong tuần nếu hôm nay không có ca dạy
  upcomingNextClass = computed<UpcomingClassInfo | null>(() => {
    if (this.todayClasses().length > 0) return null;
    const all = this.allClasses();
    if (all.length === 0) return null;

    const dayCodes = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    const currentDayIdx = new Date().getDay();
    const dayLabelsVi: Record<string, string> = {
      MONDAY: 'Thứ Hai',
      TUESDAY: 'Thứ Ba',
      WEDNESDAY: 'Thứ Tư',
      THURSDAY: 'Thứ Năm',
      FRIDAY: 'Thứ Sáu',
      SATURDAY: 'Thứ Bảy',
      SUNDAY: 'Chủ Nhật'
    };

    // Quét từ ngày mai đến 6 ngày tiếp theo
    for (let offset = 1; offset <= 7; offset++) {
      const checkIdx = (currentDayIdx + offset) % 7;
      const code = dayCodes[checkIdx];
      const classesOnDay = this.getClassesForDay(code);
      if (classesOnDay.length > 0) {
        return {
          dayLabel: dayLabelsVi[code] || code,
          classItem: classesOnDay[0]
        };
      }
    }
    return null;
  });

  formatRoom(roomStr: string | undefined): string {
    if (!roomStr) return 'Phòng trực tiếp';
    if (roomStr.includes('·')) {
      const parts = roomStr.split('·').map(s => s.trim());
      const lastPart = parts[parts.length - 1];
      if (lastPart && !lastPart.toLowerCase().includes('thứ') && !lastPart.includes(':')) {
        return lastPart;
      }
    }
    if (roomStr.toLowerCase().includes('thứ') || roomStr.includes(':')) {
      return 'Phòng học trực tiếp';
    }
    return roomStr;
  }

  nextClass = computed<TodayClassItem | null>(() => {
    const list = this.todayClasses();
    return list.length > 0 ? list[0] : null;
  });

  laterClasses = computed<TodayClassItem[]>(() => {
    const list = this.todayClasses();
    return list.length > 1 ? list.slice(1) : [];
  });

  collectionRate = computed<number>(() => {
    const exp = this.kpis().expectedTuition;
    const act = this.kpis().actualTuition;
    if (!exp || exp <= 0) return 0;
    return Math.min(100, Math.round((act / exp) * 100));
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

    // 1. Tải Dashboard Summary (KPIs, Transactions, Debtors, Chart)
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

    // 2. Tải danh sách lớp thực tế để xây dựng thời khóa biểu 7 ngày
    this.classService
      .getClasses(false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (res?.success && res.data) {
            this.classesList.set(res.data);
          }
        },
        error: () => {
          // Bỏ qua lỗi, dùng dữ liệu từ summary
        }
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

  remindZalo(debtor: DebtorItem): void {
    const name = debtor.name;
    this.remindedList.update((items) =>
      items.includes(name) ? items : [...items, name]
    );

    const message = `Kính gửi Phụ huynh em ${debtor.name}, EduPay xin gửi thông báo học phí ${debtor.due}: ${debtor.amount}. Phụ huynh vui lòng quét mã VietQR Napas247 hoặc chuyển khoản theo cú pháp để hệ thống đối soát tự động. Trân trọng cảm ơn!`;

    navigator.clipboard?.writeText(message);
    this.remindSuccessMessage.set(`Đã sao chép tin nhắn Zalo kèm thông tin VietQR cho ${debtor.name}!`);
    setTimeout(() => {
      this.remindSuccessMessage.set(null);
    }, 4500);

    // Mở Zalo chat nếu có số điện thoại
    if (debtor.parent && debtor.parent.includes(':')) {
      const parts = debtor.parent.split(':');
      const phone = parts[parts.length - 1].trim().replace(/\D/g, '');
      if (phone.length >= 9) {
        window.open(`https://zalo.me/${phone}`, '_blank');
      }
    }
  }

  isReminded(name: string): boolean {
    return this.remindedList().includes(name);
  }
}