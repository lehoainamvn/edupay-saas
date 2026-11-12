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
  CreateTutorPayload,
  UpdateTutorPayload,
} from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-tutors',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-tutors.component.html',
  styleUrls: ['./admin-tutors.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminTutorsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  loading = signal<boolean>(true);
  actionLoading = signal<string | null>(null);
  tutors = signal<TenantAdminItem[]>([]);

  // Search & Filter
  searchQuery = signal<string>('');
  statusFilter = signal<string>('ALL');

  // Create Tutor Modal
  createModalOpen = signal<boolean>(false);
  createForm: CreateTutorPayload = {
    fullName: '',
    phone: '',
    email: '',
    password: 'Admin@123',
    subscriptionPlan: 'MONTHLY',
    durationMonths: 1,
  };

  // Edit Tutor Modal
  editModalOpen = signal<boolean>(false);
  editingTutor = signal<TenantAdminItem | null>(null);
  editForm: UpdateTutorPayload = {
    fullName: '',
    email: '',
    subscriptionPlan: 'MONTHLY',
    subscriptionStatus: 'ACTIVE',
  };

  // Delete Confirm Modal
  deleteModalOpen = signal<boolean>(false);
  deletingTutor = signal<TenantAdminItem | null>(null);

  // Computed filtered tutors
  filteredTutors = computed(() => {
    let list = this.tutors();
    const status = this.statusFilter();
    const query = this.searchQuery().toLowerCase().trim();

    if (status !== 'ALL') {
      list = list.filter((t) => t.subscriptionStatus === status);
    }

    if (query) {
      list = list.filter(
        (t) =>
          t.name.toLowerCase().includes(query) ||
          t.phone.toLowerCase().includes(query) ||
          (t.email && t.email.toLowerCase().includes(query))
      );
    }

    return list;
  });

  ngOnInit(): void {
    this.loadTutors();
  }

  loadTutors(): void {
    this.loading.set(true);
    this.adminService
      .getTutors(this.statusFilter(), this.searchQuery())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res?.success && res.data) {
            this.tutors.set(res.data);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.toastService.error(err?.error?.message || 'Không thể tải danh sách gia sư');
        },
      });
  }

  // ─── CREATE TUTOR ───────────────────────────────────────────────────────────
  openCreateModal(): void {
    this.createForm = {
      fullName: '',
      phone: '',
      email: '',
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
    if (!this.createForm.fullName.trim() || !this.createForm.phone.trim()) {
      this.toastService.error('Vui lòng điền Họ tên và Số điện thoại gia sư');
      return;
    }

    this.actionLoading.set('create');
    this.adminService
      .createTutor(this.createForm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeCreateModal();
          this.toastService.success(`Đã tạo tài khoản Gia sư [${res.data?.name}] thành công!`);
          this.loadTutors();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Tạo gia sư thất bại');
        },
      });
  }

  // ─── EDIT TUTOR ─────────────────────────────────────────────────────────────
  openEditModal(tutor: TenantAdminItem): void {
    this.editingTutor.set(tutor);
    this.editForm = {
      fullName: tutor.name,
      email: tutor.email || '',
      subscriptionPlan: tutor.subscriptionPlan || 'MONTHLY',
      subscriptionStatus: tutor.subscriptionStatus || 'ACTIVE',
    };
    this.editModalOpen.set(true);
  }

  closeEditModal(): void {
    this.editModalOpen.set(false);
    this.editingTutor.set(null);
  }

  submitEdit(): void {
    const tutor = this.editingTutor();
    if (!tutor) return;

    if (!this.editForm.fullName.trim()) {
      this.toastService.error('Họ tên gia sư không được để trống');
      return;
    }

    this.actionLoading.set(tutor.id);
    this.adminService
      .updateTutor(tutor.id, this.editForm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.closeEditModal();
          this.toastService.success('Cập nhật thông tin gia sư thành công!');
          this.loadTutors();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Cập nhật gia sư thất bại');
        },
      });
  }

  // ─── TOGGLE STATUS ──────────────────────────────────────────────────────────
  toggleStatus(tutor: TenantAdminItem): void {
    const newStatus = tutor.subscriptionStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    const actionLabel = newStatus === 'ACTIVE' ? 'mở khóa' : 'tạm khóa';

    this.actionLoading.set(tutor.id);
    this.adminService
      .updateTenantStatus(tutor.id, newStatus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.toastService.success(`Đã ${actionLabel} Gia sư [${tutor.name}]`);
          this.loadTutors();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || `Thao tác ${actionLabel} thất bại`);
        },
      });
  }

  // ─── DELETE TUTOR ───────────────────────────────────────────────────────────
  openDeleteModal(tutor: TenantAdminItem): void {
    this.deletingTutor.set(tutor);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    this.deleteModalOpen.set(false);
    this.deletingTutor.set(null);
  }

  confirmDelete(): void {
    const tutor = this.deletingTutor();
    if (!tutor) return;

    this.actionLoading.set(tutor.id);
    this.adminService
      .deleteTutor(tutor.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.closeDeleteModal();
          this.toastService.success(`Đã xóa tài khoản Gia sư [${tutor.name}]`);
          this.loadTutors();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Không thể xóa gia sư này');
        },
      });
  }
}

