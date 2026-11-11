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
  CenterTeacherService,
  CenterTeacherItem,
  CreateCenterTeacherPayload,
  UpdateCenterTeacherPayload,
} from '../../../core/services/center-teacher.service';
import { ToastService } from '../../../core/services/toast.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-center-teachers',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './center-teachers.component.html',
  styleUrl: './center-teachers.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CenterTeachersComponent implements OnInit {
  private readonly teacherService = inject(CenterTeacherService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  loading = signal<boolean>(true);
  actionLoading = signal<string | null>(null);
  teachers = signal<CenterTeacherItem[]>([]);

  // Search & Filter
  searchQuery = signal<string>('');
  selectedStatus = signal<string>('ALL');

  // Modals state
  isCreateModalOpen = signal<boolean>(false);
  createForm: CreateCenterTeacherPayload = {
    phone: '',
    fullName: '',
    email: '',
    password: 'Admin@123',
  };

  selectedTeacherForReset = signal<CenterTeacherItem | null>(null);
  newPassword = signal<string>('Admin@123');

  selectedTeacherForToggle = signal<CenterTeacherItem | null>(null);

  // Computed stats
  activeTeachersCount = computed(() => {
    return this.teachers().filter((t) => t.status === 'ACTIVE').length;
  });

  totalAssignedClasses = computed(() => {
    return this.teachers().reduce((acc, t) => acc + (t.assignedClassesCount || 0), 0);
  });

  filteredTeachers = computed(() => {
    let list = this.teachers();
    const query = this.searchQuery().toLowerCase().trim();
    const status = this.selectedStatus();

    if (status !== 'ALL') {
      list = list.filter((t) => t.status === status);
    }

    if (query) {
      list = list.filter(
        (t) =>
          (t.fullName && t.fullName.toLowerCase().includes(query)) ||
          (t.phone && t.phone.toLowerCase().includes(query)) ||
          (t.email && t.email.toLowerCase().includes(query))
      );
    }

    return list;
  });

  ngOnInit(): void {
    this.loadTeachers();
  }

  loadTeachers(): void {
    this.loading.set(true);
    this.teacherService
      .getTeachers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res?.success && res.data) {
            this.teachers.set(res.data);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.toastService.error('Không thể tải danh sách giáo viên. Vui lòng thử lại sau.');
        },
      });
  }

  // ─── Modal Thêm Giáo Viên ───
  openCreateModal(): void {
    this.createForm = {
      phone: '',
      fullName: '',
      email: '',
      password: 'Admin@123',
    };
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  submitCreate(): void {
    if (!this.createForm.phone || !this.createForm.fullName) {
      this.toastService.warning('Vui lòng nhập đầy đủ Số điện thoại và Họ tên giáo viên');
      return;
    }

    this.actionLoading.set('create');
    this.teacherService
      .createTeacher(this.createForm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeCreateModal();
          this.toastService.success(`Đã thêm giáo viên "${res.data.fullName}" thành công!`);
          this.loadTeachers();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Có lỗi xảy ra khi tạo giáo viên');
        },
      });
  }

  // ─── Modal Reset Password ───
  openResetModal(teacher: CenterTeacherItem): void {
    this.selectedTeacherForReset.set(teacher);
    this.newPassword.set('Admin@123');
  }

  closeResetModal(): void {
    this.selectedTeacherForReset.set(null);
  }

  submitResetPassword(): void {
    const teacher = this.selectedTeacherForReset();
    if (!teacher) return;

    if (!this.newPassword() || this.newPassword().trim().length < 6) {
      this.toastService.warning('Mật khẩu mới phải có tối thiểu 6 ký tự');
      return;
    }

    this.actionLoading.set(teacher.id);
    this.teacherService
      .resetPassword(teacher.id, this.newPassword().trim())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.closeResetModal();
          this.toastService.success(`Đã đặt lại mật khẩu cho giáo viên "${teacher.fullName}" thành công!`);
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Không thể đổi mật khẩu');
        },
      });
  }

  // ─── Modal Khóa / Mở Khóa ───
  openToggleModal(teacher: CenterTeacherItem): void {
    this.selectedTeacherForToggle.set(teacher);
  }

  closeToggleModal(): void {
    this.selectedTeacherForToggle.set(null);
  }

  confirmToggleStatus(): void {
    const teacher = this.selectedTeacherForToggle();
    if (!teacher) return;

    const nextStatus = teacher.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    const actionName = nextStatus === 'ACTIVE' ? 'mở khóa' : 'khóa';

    this.actionLoading.set(teacher.id);
    this.teacherService
      .updateTeacherStatus(teacher.id, nextStatus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeToggleModal();
          this.teachers.update((list) =>
            list.map((t) => (t.id === teacher.id ? { ...t, status: nextStatus } : t))
          );
          this.toastService.success(`Đã ${actionName} tài khoản giáo viên "${teacher.fullName}" thành công!`);
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Có lỗi khi cập nhật trạng thái');
        },
      });
  }

  // ─── Xóa Giáo Viên ───
  deleteTeacher(teacher: CenterTeacherItem): void {
    if (teacher.assignedClassesCount > 0) {
      this.toastService.warning(`Giáo viên đang phụ trách ${teacher.assignedClassesCount} lớp học. Vui lòng chuyển giao lớp trước khi xóa.`);
      return;
    }

    if (!confirm(`Bạn có chắc chắn muốn xóa giáo viên "${teacher.fullName}" khỏi trung tâm không?`)) {
      return;
    }

    this.actionLoading.set(teacher.id);
    this.teacherService
      .deleteTeacher(teacher.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.teachers.update((list) => list.filter((t) => t.id !== teacher.id));
          this.toastService.success(`Đã xóa giáo viên "${teacher.fullName}" thành công!`);
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Không thể xóa giáo viên');
        },
      });
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

  getInitials(name: string): string {
    if (!name) return 'GV';
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

