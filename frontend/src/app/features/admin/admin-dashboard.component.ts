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
import { RouterLink } from '@angular/router';
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

  loading = signal<boolean>(true);
  actionLoading = signal<string | null>(null); // tenant id being modified
  errorMessage = signal<string | null>(null);

  // Stats & List
  stats = signal<PlatformStats | null>(null);
  tenants = signal<TenantAdminItem[]>([]);

  // Filter & Search
  currentTab = signal<'ALL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED'>('ALL');
  searchQuery = signal<string>('');
  planFilter = signal<string>('ALL');

  // Extend Modal state
  selectedTenantForExtend = signal<TenantAdminItem | null>(null);
  extendDuration = signal<'1_MONTH' | '1_YEAR'>('1_MONTH');
  extendNote = signal<string>('');

  // Status Toggle Modal state
  selectedTenantForToggle = signal<TenantAdminItem | null>(null);

  // Counts computed
  activeCount = computed(() => {
    return this.tenants().filter((t) => t.subscriptionStatus === 'ACTIVE' && !t.isExpired).length;
  });

  suspendedCount = computed(() => {
    return this.tenants().filter((t) => t.subscriptionStatus === 'SUSPENDED').length;
  });

  expiredCount = computed(() => {
    return this.tenants().filter((t) => t.isExpired || t.subscriptionStatus === 'EXPIRED').length;
  });

  // Filtered tenants computed
  filteredTenants = computed(() => {
    let list = this.tenants();
    const tab = this.currentTab();
    const query = this.searchQuery().toLowerCase().trim();
    const plan = this.planFilter();

    if (tab === 'ACTIVE') {
      list = list.filter((t) => t.subscriptionStatus === 'ACTIVE' && !t.isExpired);
    } else if (tab === 'SUSPENDED') {
      list = list.filter((t) => t.subscriptionStatus === 'SUSPENDED');
    } else if (tab === 'EXPIRED') {
      list = list.filter((t) => t.isExpired || t.subscriptionStatus === 'EXPIRED');
    }

    if (plan !== 'ALL') {
      list = list.filter((t) => (t.subscriptionPlan || '').toUpperCase() === plan);
    }

    if (query) {
      list = list.filter(
        (t) =>
          (t.name && t.name.toLowerCase().includes(query)) ||
          (t.phone && t.phone.toLowerCase().includes(query)) ||
          (t.email && t.email.toLowerCase().includes(query))
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
      .getTenants('ALL')
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
          this.errorMessage.set('Không thể tải danh sách cơ sở giáo viên. Vui lòng thử lại sau.');
          this.toastService.error('Không thể tải dữ liệu nền tảng. Vui lòng kiểm tra lại kết nối.');
        },
      });
  }

  setTab(tab: 'ALL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED'): void {
    this.currentTab.set(tab);
  }

  // ─── Status Toggle Modal Handlers ───
  openToggleModal(tenant: TenantAdminItem): void {
    this.selectedTenantForToggle.set(tenant);
  }

  closeToggleModal(): void {
    this.selectedTenantForToggle.set(null);
  }

  confirmToggleStatus(): void {
    const tenant = this.selectedTenantForToggle();
    if (!tenant) return;

    const nextStatus = tenant.subscriptionStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    const actionName = nextStatus === 'ACTIVE' ? 'mở khóa' : 'khóa';

    this.actionLoading.set(tenant.id);
    this.adminService
      .updateTenantStatus(tenant.id, nextStatus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeToggleModal();
          if (res?.success && res.data) {
            this.updateTenantInList(res.data);
            this.toastService.success(`Đã ${actionName} tài khoản cơ sở "${tenant.name}" thành công!`);
            // Refresh stats
            this.refreshStats();
          }
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(
            `Lỗi khi ${actionName} tài khoản: ${err?.error?.message || 'Có lỗi xảy ra'}`
          );
        },
      });
  }

  // ─── Quick Extend ───
  quickExtend(tenant: TenantAdminItem, duration: '1_MONTH' | '1_YEAR'): void {
    const label = duration === '1_MONTH' ? '+1 Tháng' : '+1 Năm';

    this.actionLoading.set(tenant.id);
    const payload: TenantExtendPayload = {
      duration,
      note: `Gia hạn nhanh ${label} từ SuperAdmin Dashboard`,
    };

    this.adminService
      .extendSubscription(tenant.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          if (res?.success && res.data) {
            this.updateTenantInList(res.data);
            this.toastService.success(`Đã gia hạn thành công ${label} cho "${tenant.name}"!`);
            this.refreshStats();
          }
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(`Lỗi gia hạn: ${err?.error?.message || 'Thất bại'}`);
        },
      });
  }

  // ─── Custom Modal Extend ───
  openExtendModal(tenant: TenantAdminItem): void {
    this.selectedTenantForExtend.set(tenant);
    this.extendDuration.set('1_MONTH');
    this.extendNote.set('');
  }

  closeExtendModal(): void {
    this.selectedTenantForExtend.set(null);
  }

  submitModalExtend(): void {
    const tenant = this.selectedTenantForExtend();
    if (!tenant) return;

    this.actionLoading.set(tenant.id);
    const payload: TenantExtendPayload = {
      duration: this.extendDuration(),
      note: this.extendNote(),
    };

    this.adminService
      .extendSubscription(tenant.id, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeExtendModal();
          if (res?.success && res.data) {
            this.updateTenantInList(res.data);
            this.toastService.success(`Đã gia hạn thành công dịch vụ cho "${tenant.name}"!`);
            this.refreshStats();
          }
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(`Lỗi gia hạn: ${err?.error?.message || 'Thất bại'}`);
        },
      });
  }

  private refreshStats(): void {
    this.adminService
      .getPlatformStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((s) => {
        if (s?.data) this.stats.set(s.data);
      });
  }

  private updateTenantInList(updated: TenantAdminItem): void {
    this.tenants.update((list) =>
      list.map((t) => (t.id === updated.id ? { ...t, ...updated } : t))
    );
  }

  formatMoney(amount: number | null | undefined): string {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' đ';
  }

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  getDaysText(t: TenantAdminItem): string {
    return this.getDaysRemainingInfo(t.subscriptionEndsAt, t.isExpired).text;
  }

  isUrgentExpiry(t: TenantAdminItem): boolean {
    return this.getDaysRemainingInfo(t.subscriptionEndsAt, t.isExpired).isUrgent;
  }

  getDaysRemainingInfo(dateStr: string | null | undefined, isExpired: boolean): { text: string; isUrgent: boolean } {
    if (!dateStr) return { text: 'Chưa xác định', isUrgent: false };
    const end = new Date(dateStr).getTime();
    const now = new Date().getTime();
    const diffDays = Math.ceil((end - now) / (1000 * 60 * 60 * 24));

    if (diffDays <= 0 || isExpired) {
      return { text: 'Đã hết hạn', isUrgent: true };
    }
    if (diffDays <= 7) {
      return { text: `Còn ${diffDays} ngày`, isUrgent: true };
    }
    return { text: `Còn ${diffDays} ngày`, isUrgent: false };
  }

  getInitials(name: string): string {
    if (!name) return 'CS';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[parts.length - 2][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  getAvatarColor(name: string): string {
    const colors = [
      'linear-gradient(135deg, #2563eb, #1d4ed8)',
      'linear-gradient(135deg, #059669, #047857)',
      'linear-gradient(135deg, #7c3aed, #6d28d9)',
      'linear-gradient(135deg, #d97706, #b45309)',
      'linear-gradient(135deg, #0891b2, #0e7490)',
      'linear-gradient(135deg, #e11d48, #be123c)',
    ];
    let hash = 0;
    for (let i = 0; i < (name || '').length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % colors.length;
    return colors[index];
  }
}
