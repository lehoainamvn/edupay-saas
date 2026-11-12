import {
  Component,
  OnInit,
  DestroyRef,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AdminService,
  TenantAdminItem,
  PlatformStats,
  TenantExtendPayload,
} from '../../core/services/admin.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  loading = signal<boolean>(true);
  refreshed = signal<boolean>(false);
  actionLoading = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  // Stats & List
  stats = signal<PlatformStats | null>(null);
  tenants = signal<TenantAdminItem[]>([]);

  // Filter & Search
  currentTab = signal<'ALL' | 'CENTER' | 'SOLO_TUTOR' | 'EXPIRED'>('ALL');
  searchQuery = signal<string>('');

  // Extend Modal state
  selectedTenantForExtend = signal<TenantAdminItem | null>(null);
  extendDuration = signal<'1_MONTH' | '1_YEAR'>('1_MONTH');
  extendNote = signal<string>('');

  // Status Toggle Modal state
  selectedTenantForToggle = signal<TenantAdminItem | null>(null);

  // Computed metrics
  centerCount = computed(
    () =>
      this.stats()?.totalCenters ||
      this.tenants().filter((t) => t.tenantType === 'CENTER').length
  );

  tutorCount = computed(
    () =>
      this.stats()?.totalTutors ||
      this.tenants().filter((t) => t.tenantType === 'SOLO_TUTOR').length
  );

  teacherCount = computed(() => this.stats()?.totalTeachers || 0);
  studentCount = computed(() => this.stats()?.totalStudents || 0);
  classCount = computed(() => this.stats()?.totalClasses || 0);

  expiredCount = computed(() => {
    return this.tenants().filter(
      (t) => t.isExpired || t.subscriptionStatus === 'EXPIRED'
    ).length;
  });

  centerPercent = computed(() => {
    const total = this.tenants().length || 1;
    const c = this.centerCount();
    return Math.round((c / total) * 1000) / 10;
  });

  tutorPercent = computed(() => {
    const total = this.tenants().length || 1;
    const t = this.tutorCount();
    return Math.round((t / total) * 1000) / 10;
  });

  // Top Center
  topCenter = computed(() => {
    const centers = this.tenants().filter((t) => t.tenantType === 'CENTER');
    if (!centers.length) return null;
    return centers.slice().sort(
      (a, b) =>
        (b.totalTeachers || 0) +
        (b.totalClasses || 0) +
        (b.totalStudents || 0) -
        ((a.totalTeachers || 0) + (a.totalClasses || 0) + (a.totalStudents || 0))
    )[0];
  });

  todayFormatted = computed(() => {
    const now = new Date();
    const d = String(now.getDate()).padStart(2, '0');
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const y = now.getFullYear();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    return `${d}/${m}/${y} · ${hh}:${mm}`;
  });

  // Filtered tenants computed
  filteredTenants = computed(() => {
    let list = this.tenants();
    const tab = this.currentTab();
    const query = this.searchQuery().toLowerCase().trim();

    if (tab === 'CENTER') {
      list = list.filter((t) => t.tenantType === 'CENTER');
    } else if (tab === 'SOLO_TUTOR') {
      list = list.filter((t) => t.tenantType === 'SOLO_TUTOR');
    } else if (tab === 'EXPIRED') {
      list = list.filter(
        (t) => t.isExpired || t.subscriptionStatus === 'EXPIRED'
      );
    }

    if (query) {
      list = list.filter(
        (t) =>
          (t.name && t.name.toLowerCase().includes(query)) ||
          (t.phone && t.phone.toLowerCase().includes(query)) ||
          (t.email && t.email.toLowerCase().includes(query)) ||
          (t.representativeName &&
            t.representativeName.toLowerCase().includes(query))
      );
    }

    return list;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.adminService
      .getPlatformStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (res?.success && res.data) {
            this.stats.set(res.data);
          }
        },
        error: () => {},
      });

    this.adminService
      .getTenants('ALL', '')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res?.success && res.data) {
            this.tenants.set(res.data);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMessage.set(
            err?.error?.message || 'Không thể tải danh sách tài khoản cơ sở.'
          );
        },
      });
  }

  handleRefresh(): void {
    this.refreshed.set(true);
    this.loadData();
    setTimeout(() => this.refreshed.set(false), 1200);
  }

  setTab(tab: 'ALL' | 'CENTER' | 'SOLO_TUTOR' | 'EXPIRED'): void {
    this.currentTab.set(tab);
  }

  navigateCreateCenter(): void {
    this.router.navigate(['/admin/centers'], { queryParams: { action: 'create' } });
  }

  navigateCreateTutor(): void {
    this.router.navigate(['/admin/tutors'], { queryParams: { action: 'create' } });
  }

  centerInitials(name: string): string {
    const parts = (name || 'Ánh Dương').trim().split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return (name || 'TT').substring(0, 2).toUpperCase();
  }

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '01/01/2100';
    try {
      const d = new Date(dateStr);
      return `${String(d.getDate()).padStart(2, '0')}/${String(
        d.getMonth() + 1
      ).padStart(2, '0')}/${d.getFullYear()}`;
    } catch {
      return dateStr;
    }
  }

  formatMoney(amount: number | null | undefined): string {
    if (amount == null) return '0 đ';
    return (
      new Intl.NumberFormat('vi-VN', {
        maximumFractionDigits: 0,
      }).format(amount) + ' đ'
    );
  }

  // ─── EXTEND MODAL ───────────────────────────────────────────────────────────
  openExtendModal(tenant: TenantAdminItem): void {
    this.selectedTenantForExtend.set(tenant);
    this.extendDuration.set('1_MONTH');
    this.extendNote.set('');
  }

  closeExtendModal(): void {
    this.selectedTenantForExtend.set(null);
  }

  submitExtend(): void {
    const tenant = this.selectedTenantForExtend();
    if (!tenant) return;

    this.actionLoading.set(tenant.id);

    const payload: TenantExtendPayload = {
      duration: this.extendDuration(),
      note: this.extendNote()?.trim() || undefined,
    };

    this.adminService
      .extendSubscription(tenant.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeExtendModal();
          this.toastService.success(
            `Gia hạn thành công cho cơ sở [${tenant.name}]!`
          );
          if (res?.data) {
            this.tenants.update((list) =>
              list.map((t) => (t.id === tenant.id ? res.data! : t))
            );
          }
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(
            err?.error?.message || 'Gia hạn gói dịch vụ thất bại'
          );
        },
      });
  }

  // ─── STATUS TOGGLE MODAL ────────────────────────────────────────────────────
  openStatusModal(tenant: TenantAdminItem): void {
    this.selectedTenantForToggle.set(tenant);
  }

  closeToggleModal(): void {
    this.selectedTenantForToggle.set(null);
  }

  confirmToggleStatus(): void {
    const tenant = this.selectedTenantForToggle();
    if (!tenant) return;

    const newStatus =
      tenant.subscriptionStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    const actionLabel = newStatus === 'ACTIVE' ? 'mở khóa' : 'tạm khóa';

    this.actionLoading.set(tenant.id);

    this.adminService
      .updateTenantStatus(tenant.id, newStatus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeToggleModal();
          this.toastService.success(
            `Đã ${actionLabel} cơ sở [${tenant.name}]!`
          );
          if (res?.data) {
            this.tenants.update((list) =>
              list.map((t) => (t.id === tenant.id ? res.data! : t))
            );
          }
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(
            err?.error?.message || `Không thể ${actionLabel} tài khoản`
          );
        },
      });
  }
}
