import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminUserItem, CreateUserPayload, UpdateUserPayload, TenantAdminItem } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.css']
})
export class AdminUsersComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);

  // ─── STATE SIGNALS ────────────────────────────────────────────────────────
  loading = signal<boolean>(false);
  users = signal<AdminUserItem[]>([]);
  tenants = signal<TenantAdminItem[]>([]);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // Filters & Pagination
  currentPage = signal<number>(0);
  pageSize = signal<number>(10);
  searchQuery = signal<string>('');
  selectedRole = signal<string>('ALL');
  selectedStatus = signal<string>('ALL');

  // Modals state
  showCreateModal = signal<boolean>(false);
  showEditModal = signal<boolean>(false);
  showResetPasswordModal = signal<boolean>(false);
  showDeleteModal = signal<boolean>(false);

  // Modal forms
  createForm: CreateUserPayload = {
    phone: '',
    fullName: '',
    email: '',
    password: '',
    roleCode: 'TEACHER',
    tenantId: ''
  };

  selectedUser = signal<AdminUserItem | null>(null);

  editForm: UpdateUserPayload = {
    fullName: '',
    email: '',
    status: 'ACTIVE',
    roles: []
  };

  newPasswordInput = signal<string>('');
  isSubmitting = signal<boolean>(false);

  ngOnInit(): void {
    this.loadUsers();
    this.loadTenants();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.adminService.getUsers({
      page: this.currentPage(),
      size: this.pageSize(),
      search: this.searchQuery(),
      role: this.selectedRole(),
      status: this.selectedStatus()
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          this.users.set(res.data.content || []);
          this.totalElements.set(res.data.totalElements || 0);
          this.totalPages.set(res.data.totalPages || 0);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.toastService.error(err.error?.message || 'Không thể tải danh sách người dùng');
      }
    });
  }

  loadTenants(): void {
    this.adminService.getTenants('ALL').subscribe({
      next: (res) => {
        if (res.data) {
          this.tenants.set(res.data);
          if (res.data.length > 0 && !this.createForm.tenantId) {
            this.createForm.tenantId = res.data[0].id;
          }
        }
      }
    });
  }

  onSearch(): void {
    this.currentPage.set(0);
    this.loadUsers();
  }

  onFilterChange(): void {
    this.currentPage.set(0);
    this.loadUsers();
  }

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadUsers();
    }
  }

  // ─── CREATE USER ──────────────────────────────────────────────────────────
  openCreateModal(): void {
    this.createForm = {
      phone: '',
      fullName: '',
      email: '',
      password: '',
      roleCode: 'TEACHER',
      tenantId: this.tenants().length > 0 ? this.tenants()[0].id : ''
    };
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  submitCreate(): void {
    if (!this.createForm.phone || !this.createForm.fullName || !this.createForm.password) {
      this.toastService.warning('Vui lòng điền đầy đủ các thông tin bắt buộc (*)');
      return;
    }

    if (this.createForm.password.length < 6) {
      this.toastService.warning('Mật khẩu phải có tối thiểu 6 ký tự');
      return;
    }

    this.isSubmitting.set(true);
    this.adminService.createUser(this.createForm).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeCreateModal();
        this.toastService.success('Tạo tài khoản người dùng thành công');
        this.loadUsers();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Lỗi khi tạo tài khoản');
      }
    });
  }

  // ─── EDIT USER ────────────────────────────────────────────────────────────
  openEditModal(user: AdminUserItem): void {
    this.selectedUser.set(user);
    this.editForm = {
      fullName: user.fullName,
      email: user.email || '',
      status: user.status,
      roles: [...(user.roles || [])]
    };
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.selectedUser.set(null);
  }

  toggleRoleInEdit(roleCode: string): void {
    const roles = this.editForm.roles || [];
    const index = roles.indexOf(roleCode);
    if (index > -1) {
      if (roles.length > 1) {
        roles.splice(index, 1);
      } else {
        this.toastService.warning('Người dùng phải có ít nhất một vai trò');
      }
    } else {
      roles.push(roleCode);
    }
    this.editForm.roles = [...roles];
  }

  hasRoleInEdit(roleCode: string): boolean {
    return (this.editForm.roles || []).includes(roleCode);
  }

  submitEdit(): void {
    const user = this.selectedUser();
    if (!user) return;

    if (!this.editForm.fullName) {
      this.toastService.warning('Họ và tên không được để trống');
      return;
    }

    this.isSubmitting.set(true);
    this.adminService.updateUser(user.id, this.editForm).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeEditModal();
        this.toastService.success('Cập nhật người dùng thành công');
        this.loadUsers();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Lỗi khi cập nhật');
      }
    });
  }

  // ─── TOGGLE STATUS ────────────────────────────────────────────────────────
  toggleUserStatus(user: AdminUserItem): void {
    const newStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    const actionName = newStatus === 'ACTIVE' ? 'Mở khóa' : 'Khóa';

    this.adminService.updateUserStatus(user.id, newStatus).subscribe({
      next: () => {
        this.toastService.success(`${actionName} tài khoản ${user.fullName} thành công`);
        this.loadUsers();
      },
      error: (err) => {
        this.toastService.error(err.error?.message || `Lỗi khi ${actionName} tài khoản`);
      }
    });
  }

  // ─── RESET PASSWORD ───────────────────────────────────────────────────────
  openResetPasswordModal(user: AdminUserItem): void {
    this.selectedUser.set(user);
    this.newPasswordInput.set('EduPay@' + Math.floor(1000 + Math.random() * 9000));
    this.showResetPasswordModal.set(true);
  }

  closeResetPasswordModal(): void {
    this.showResetPasswordModal.set(false);
    this.selectedUser.set(null);
  }

  generateRandomPassword(): void {
    this.newPasswordInput.set('EduPay@' + Math.floor(100000 + Math.random() * 900000));
  }

  submitResetPassword(): void {
    const user = this.selectedUser();
    const newPass = this.newPasswordInput().trim();
    if (!user || !newPass) return;

    if (newPass.length < 6) {
      this.toastService.warning('Mật khẩu phải từ 6 ký tự trở lên');
      return;
    }

    this.isSubmitting.set(true);
    this.adminService.resetPassword(user.id, newPass).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeResetPasswordModal();
        this.toastService.success(`Đặt lại mật khẩu cho ${user.fullName} thành công`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Lỗi khi đặt lại mật khẩu');
      }
    });
  }

  // ─── DELETE USER ──────────────────────────────────────────────────────────
  openDeleteModal(user: AdminUserItem): void {
    this.selectedUser.set(user);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.selectedUser.set(null);
  }

  submitDelete(): void {
    const user = this.selectedUser();
    if (!user) return;

    this.isSubmitting.set(true);
    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeDeleteModal();
        this.toastService.success(`Đã xóa tài khoản ${user.fullName}`);
        this.loadUsers();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error(err.error?.message || 'Lỗi khi xóa người dùng');
      }
    });
  }

  // ─── HELPERS ──────────────────────────────────────────────────────────────
  getRoleBadgeClass(role: string): string {
    const upper = (role || '').toUpperCase();
    if (upper.includes('SUPER_ADMIN')) return 'badge-superadmin';
    if (upper.includes('OWNER')) return 'badge-owner';
    if (upper.includes('TEACHER')) return 'badge-teacher';
    return 'badge-viewer';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      return dateStr;
    }
  }
}

