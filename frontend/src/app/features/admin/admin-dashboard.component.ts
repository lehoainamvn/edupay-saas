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
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import {
  AdminService,
  TenantAdminItem,
  PlatformStats,
  TenantExtendPayload,
} from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);

  loading = signal<boolean>(true);
  actionLoading = signal<string | null>(null); // tenant id being modified
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Stats & List
  stats = signal<PlatformStats | null>(null);
  tenants = signal<TenantAdminItem[]>([]);

  // Filter & Search
  currentTab = signal<'ALL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED'>('ALL');
  searchQuery = signal<string>('');

  // Extend Modal state
  selectedTenantForExtend = signal<TenantAdminItem | null>(null);
  extendDuration = signal<'1_MONTH' | '1_YEAR'>('1_MONTH');
  extendNote = signal<string>('');

  // Filtered tenants computed
  filteredTenants = computed(() => {
    let list = this.tenants();
    const tab = this.currentTab();
    const query = this.searchQuery().toLowerCase().trim();

    if (tab === 'ACTIVE') {
      list = list.filter((t) => t.subscriptionStatus === 'ACTIVE' && !t.isExpired);
    } else if (tab === 'SUSPENDED') {
      list = list.filter((t) => t.subscriptionStatus === 'SUSPENDED');
    } else if (tab === 'EXPIRED') {
      list = list.filter((t) => t.isExpired || t.subscriptionStatus === 'EXPIRED');
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
          this.errorMessage.set('Không thể tải danh sách giáo viên. Vui lòng thử lại sau.');
        },
      });
  }

  setTab(tab: 'ALL' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED'): void {
    this.currentTab.set(tab);
  }

  toggleTenantStatus(tenant: TenantAdminItem): void {
    const nextStatus = tenant.subscriptionStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    const actionName = nextStatus === 'ACTIVE' ? 'mở khóa' : 'khóa';

    if (!confirm(`Bạn có chắc chắn muốn ${actionName} tài khoản giáo viên "${tenant.name}"?`)) {
      return;
    }

    this.actionLoading.set(tenant.id);
    this.adminService
      .updateTenantStatus(tenant.id, nextStatus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          if (res?.success && res.data) {
            this.updateTenantInList(res.data);
            this.showToast(`Đã ${actionName} tài khoản "${tenant.name}" thành công!`);
          }
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.showToast(`Lỗi khi ${actionName} tài khoản: ${err?.error?.message || 'Thất bại'}`);
        },
      });
  }

  quickExtend(tenant: TenantAdminItem, duration: '1_MONTH' | '1_YEAR'): void {
    const label = duration === '1_MONTH' ? '+1 Tháng' : '+1 Năm';
    if (!confirm(`Xác nhận gia hạn ${label} cho giáo viên "${tenant.name}"?`)) {
      return;
    }

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
            this.showToast(`Đã gia hạn thành công ${label} cho "${tenant.name}"!`);
            // Refresh stats
            this.adminService.getPlatformStats().subscribe((s) => {
              if (s?.data) this.stats.set(s.data);
            });
          }
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.showToast(`Lỗi gia hạn: ${err?.error?.message || 'Thất bại'}`);
        },
      });
  }

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
            this.showToast(`Đã gia hạn thành công cho "${tenant.name}"!`);
            this.adminService.getPlatformStats().subscribe((s) => {
              if (s?.data) this.stats.set(s.data);
            });
          }
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.showToast(`Lỗi gia hạn: ${err?.error?.message || 'Thất bại'}`);
        },
      });
  }

  private updateTenantInList(updated: TenantAdminItem): void {
    this.tenants.update((list) =>
      list.map((t) => (t.id === updated.id ? { ...t, ...updated } : t))
    );
  }

  private showToast(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => {
      this.successMessage.set(null);
    }, 4000);
  }

  formatMoney(amount: number | null | undefined): string {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' đ';
  }

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return 'Không xác định';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  getInitials(name: string): string {
    if (!name) return 'GV';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[parts.length - 2][0] + parts[parts.length - 1][0]).toUpperCase();
  }
}
