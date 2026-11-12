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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AdminService,
  TenantAdminItem,
  CenterTeacherItem,
  CreateCenterPayload,
  UpdateCenterPayload,
} from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-centers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-centers.component.html',
  styleUrls: ['./admin-centers.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminCentersComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  loading = signal<boolean>(true);
  actionLoading = signal<string | null>(null);
  centers = signal<TenantAdminItem[]>([]);

  // Search & Filter
  searchQuery = signal<string>('');
  statusFilter = signal<string>('ALL');

  // Teachers Modal
  viewingCenter = signal<TenantAdminItem | null>(null);
  centerTeachers = signal<CenterTeacherItem[]>([]);
  teachersLoading = signal<boolean>(false);

  // Create Center Modal
  createModalOpen = signal<boolean>(false);
  createForm: CreateCenterPayload = {
    name: '',
    phone: '',
    email: '',
    representativeName: '',
    password: 'Admin@123',
    subscriptionPlan: 'MONTHLY',
    durationMonths: 1,
  };

  // Edit Center Modal
  editModalOpen = signal<boolean>(false);
  editingCenter = signal<TenantAdminItem | null>(null);
  editForm: UpdateCenterPayload = {
    name: '',
    email: '',
    subscriptionPlan: 'MONTHLY',
    subscriptionStatus: 'ACTIVE',
    representativeName: '',
  };

  // Delete Confirm Modal
  deleteModalOpen = signal<boolean>(false);
  deletingCenter = signal<TenantAdminItem | null>(null);

  // Computed filtered centers
  filteredCenters = computed(() => {
    let list = this.centers();
    const status = this.statusFilter();
    const query = this.searchQuery().toLowerCase().trim();

    if (status !== 'ALL') {
      list = list.filter((c) => c.subscriptionStatus === status);
    }

    if (query) {
      list = list.filter(
        (c) =>
          c.name.toLowerCase().includes(query) ||
          c.phone.toLowerCase().includes(query) ||
          (c.email && c.email.toLowerCase().includes(query)) ||
          (c.representativeName && c.representativeName.toLowerCase().includes(query))
      );
    }

    return list;
  });

  ngOnInit(): void {
    this.loadCenters();
  }

  loadCenters(): void {
    this.loading.set(true);
    this.adminService
      .getCenters(this.statusFilter(), this.searchQuery())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res?.success && res.data) {
            this.centers.set(res.data);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.toastService.error(err?.error?.message || 'Không thể tải danh sách trung tâm');
        },
      });
  }

  // ─── TEACHERS MODAL ─────────────────────────────────────────────────────────
  openTeachersModal(center: TenantAdminItem): void {
    this.viewingCenter.set(center);
    this.centerTeachers.set([]);
    this.teachersLoading.set(true);

    this.adminService
      .getCenterTeachers(center.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.teachersLoading.set(false);
          if (res?.success && res.data) {
            this.centerTeachers.set(res.data);
          }
        },
        error: (err) => {
          this.teachersLoading.set(false);
          this.toastService.error(err?.error?.message || 'Không thể tải danh sách giáo viên trung tâm');
        },
      });
  }

  closeTeachersModal(): void {
    this.viewingCenter.set(null);
    this.centerTeachers.set([]);
  }

  // ─── CREATE CENTER ──────────────────────────────────────────────────────────
  openCreateModal(): void {
    this.createForm = {
      name: '',
      phone: '',
      email: '',
      representativeName: '',
      password: 'Admin@123',
      subscriptionPlan: 'MONTHLY',
      durationMonths: 1,
    };
    this.createModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.createModalOpen.set(false);
  }

  submitCreate(): void {
    if (!this.createForm.name.trim() || !this.createForm.phone.trim() || !this.createForm.representativeName.trim()) {
      this.toastService.error('Vui lòng điền đầy đủ Tên cơ sở, SĐT và Tên người đại diện');
      return;
    }

    this.actionLoading.set('create');
    this.adminService
      .createCenter(this.createForm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeCreateModal();
          this.toastService.success(`Đã tạo thành công Trung tâm [${res.data?.name}]!`);
          this.loadCenters();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Tạo trung tâm thất bại');
        },
      });
  }

  // ─── EDIT CENTER ────────────────────────────────────────────────────────────
  openEditModal(center: TenantAdminItem): void {
    this.editingCenter.set(center);
    this.editForm = {
      name: center.name,
      email: center.email || '',
      subscriptionPlan: center.subscriptionPlan || 'MONTHLY',
      subscriptionStatus: center.subscriptionStatus || 'ACTIVE',
      representativeName: center.representativeName || '',
    };
    this.editModalOpen.set(true);
  }

  closeEditModal(): void {
    this.editModalOpen.set(false);
    this.editingCenter.set(null);
  }

  submitEdit(): void {
    const center = this.editingCenter();
    if (!center) return;

    if (!this.editForm.name.trim()) {
      this.toastService.error('Tên trung tâm không được để trống');
      return;
    }

    this.actionLoading.set(center.id);
    this.adminService
      .updateCenter(center.id, this.editForm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeEditModal();
          this.toastService.success('Cập nhật thông tin trung tâm thành công!');
          this.loadCenters();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Cập nhật trung tâm thất bại');
        },
      });
  }

  // ─── TOGGLE STATUS ──────────────────────────────────────────────────────────
  toggleStatus(center: TenantAdminItem): void {
    const newStatus = center.subscriptionStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    const actionLabel = newStatus === 'ACTIVE' ? 'mở khóa' : 'tạm khóa';

    this.actionLoading.set(center.id);
    this.adminService
      .updateTenantStatus(center.id, newStatus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.toastService.success(`Đã ${actionLabel} Trung tâm [${center.name}]`);
          this.loadCenters();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || `Thao tác ${actionLabel} thất bại`);
        },
      });
  }

  // ─── DELETE CENTER ──────────────────────────────────────────────────────────
  openDeleteModal(center: TenantAdminItem): void {
    this.deletingCenter.set(center);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    this.deleteModalOpen.set(false);
    this.deletingCenter.set(null);
  }

  confirmDelete(): void {
    const center = this.deletingCenter();
    if (!center) return;

    this.actionLoading.set(center.id);
    this.adminService
      .deleteCenter(center.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.closeDeleteModal();
          this.toastService.success(`Đã xóa vĩnh viễn Trung tâm [${center.name}]`);
          this.loadCenters();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Không thể xóa trung tâm này');
        },
      });
  }
}

