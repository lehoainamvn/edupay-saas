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
  actionLoading = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  // Stats & List
  stats = signal<PlatformStats | null>(null);
  tenants = signal<TenantAdminItem[]>([]);

  // Filter & Search
  currentTab = signal<'ALL' | 'CENTER' | 'SOLO_TUTOR' | 'EXPIRED'>('ALL');
  searchQuery = signal<string>('');
  planFilter = signal<string>('ALL');

  // Extend Modal state
  selectedTenantForExtend = signal<TenantAdminItem | null>(null);
  extendDuration = signal<'1_MONTH' | '1_YEAR'>('1_MONTH');
  extendNote = signal<string>('');

  // Status Toggle Modal state
  selectedTenantForToggle = signal<TenantAdminItem | null>(null);

  // Computed metrics
  centerCount = computed(() => this.stats()?.totalCenters || this.tenants().filter(t => t.tenantType === 'CENTER').length);
  tutorCount = computed(() => this.stats()?.totalTutors || this.tenants().filter(t => t.tenantType === 'SOLO_TUTOR').length);
  teacherCount = computed(() => this.stats()?.totalTeachers || 0);
  studentCount = computed(() => this.stats()?.totalStudents || 0);
  classCount = computed(() => this.stats()?.totalClasses || 0);

  activeCount = computed(() => {
    return this.tenants().filter((t) => t.subscriptionStatus === 'ACTIVE' && !t.isExpired).length;
  });

  suspendedCount = computed(() => {
    return this.tenants().filter((t) => t.subscriptionStatus === 'SUSPENDED').length;
  });

  expiredCount = computed(() => {
    return this.tenants().filter((t) => t.isExpired || t.subscriptionStatus === 'EXPIRED').length;
  });

  // Top Centers computed
  topCenters = computed(() => {
    return this.tenants()
      .filter((t) => t.tenantType === 'CENTER')
      .sort((a, b) => (b.totalStudents + b.totalClasses) - (a.totalStudents + a.totalClasses))
      .slice(0, 5);
  });

  // Filtered tenants computed
  filteredTenants = computed(() => {
    let list = this.tenants();
    const tab = this.currentTab();
    const query = this.searchQuery().toLowerCase().trim();
    const plan = this.planFilter();

    if (tab === 'CENTER') {
      list = list.filter((t) => t.tenantType === 'CENTER');
    } else if (tab === 'SOLO_TUTOR') {
      list = list.filter((t) => t.tenantType === 'SOLO_TUTOR');
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
          (t.email && t.email.toLowerCase().includes(query)) ||
          (t.representativeName && t.representativeName.toLowerCase().includes(query))
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

  setTab(tab: 'ALL' | 'CENTER' | 'SOLO_TUTOR' | 'EXPIRED'): void {
    this.currentTab.set(tab);
  }

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

  openToggleModal(tenant: TenantAdminItem): void {
    this.selectedTenantForToggle.set(tenant);
  }

  closeToggleModal(): void {
    this.selectedTenantForToggle.set(null);
  }

  confirmToggleStatus(): void {
    const tenant = this.selectedTenantForToggle();
    if (!tenant) return;

    const newStatus = tenant.subscriptionStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
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

  formatMoney(amount: number | null | undefined): string {
    if (amount == null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}
