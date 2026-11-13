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
  MenuItem,
  RoleItem,
  CreateMenuPayload,
  UpdateMenuPayload,
} from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-menus',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-menus.component.html',
  styleUrls: ['./admin-menus.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminMenusComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  activeTab = signal<'CATALOG' | 'MATRIX'>('CATALOG');
  loading = signal<boolean>(true);
  actionLoading = signal<string | null>(null);

  // ─── TAB 1: MENU CATALOG ──────────────────────────────────────────────────
  menus = signal<MenuItem[]>([]);
  createModalOpen = signal<boolean>(false);
  createForm: CreateMenuPayload = {
    code: '',
    name: '',
    icon: 'folder',
    route: '',
    sortOrder: 10,
    isActive: true,
  };

  editModalOpen = signal<boolean>(false);
  editingMenu = signal<MenuItem | null>(null);
  editForm: UpdateMenuPayload = {
    name: '',
    icon: 'folder',
    route: '',
    sortOrder: 10,
    isActive: true,
  };

  deleteModalOpen = signal<boolean>(false);
  deletingMenu = signal<MenuItem | null>(null);

  // ─── TAB 2: ROLE MENU MATRIX ──────────────────────────────────────────────
  roles = signal<RoleItem[]>([]);
  selectedRole = signal<RoleItem | null>(null);
  roleMenuMap = signal<Record<string, string[]>>({});
  savingMatrix = signal<boolean>(false);

  // Selected menu IDs for the currently selected role
  currentRoleMenuIds = signal<Set<string>>(new Set());

  ngOnInit(): void {
    this.loadCatalog();
    this.loadMatrix();
  }

  loadCatalog(): void {
    this.loading.set(true);
    this.adminService
      .getMenus()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res?.success && res.data) {
            this.menus.set(res.data);
          }
        },
        error: (err) => {
          this.loading.set(false);
          this.toastService.error(err?.error?.message || 'Không thể tải danh sách Menu');
        },
      });
  }

  loadMatrix(): void {
    this.adminService
      .getRoleMenuMatrix()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          if (res?.success && res.data) {
            this.roles.set(res.data.roles);
            this.roleMenuMap.set(res.data.roleMenuMap || {});

            // Mặc định chọn role đầu tiên nếu chưa chọn
            if (!this.selectedRole() && res.data.roles.length > 0) {
              this.selectRole(res.data.roles[0]);
            } else if (this.selectedRole()) {
              const currentId = this.selectedRole()!.id;
              const assigned = (res.data.roleMenuMap && res.data.roleMenuMap[currentId]) || [];
              this.currentRoleMenuIds.set(new Set(assigned));
            }
          }
        },
        error: (err) => {
          this.toastService.error(err?.error?.message || 'Không thể tải ma trận phân quyền Menu');
        },
      });
  }

  selectRole(role: RoleItem): void {
    this.selectedRole.set(role);
    const assigned = this.roleMenuMap()[role.id] || [];
    this.currentRoleMenuIds.set(new Set(assigned));
  }

  isMenuChecked(menuId: string): boolean {
    return this.currentRoleMenuIds().has(menuId);
  }

  toggleMenuChecked(menuId: string): void {
    this.currentRoleMenuIds.update((set) => {
      const next = new Set(set);
      if (next.has(menuId)) {
        next.delete(menuId);
      } else {
        next.add(menuId);
      }
      return next;
    });
  }

  saveRoleMenus(): void {
    const role = this.selectedRole();
    if (!role) return;

    this.savingMatrix.set(true);
    const menuIds = Array.from(this.currentRoleMenuIds());

    this.adminService
      .updateRoleMenus(role.id, menuIds)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingMatrix.set(false);
          this.toastService.success(`Đã cập nhật phân quyền Menu cho vai trò [${role.name}]!`);
          // Cập nhật local map
          this.roleMenuMap.update((map) => ({
            ...map,
            [role.id]: menuIds,
          }));
        },
        error: (err) => {
          this.savingMatrix.set(false);
          this.toastService.error(err?.error?.message || 'Cập nhật phân quyền Menu thất bại');
        },
      });
  }

  // ─── CREATE MENU ──────────────────────────────────────────────────────────
  openCreateModal(): void {
    this.createForm = {
      code: '',
      name: '',
      icon: 'folder',
      route: '',
      sortOrder: (this.menus().length + 1) * 1,
      isActive: true,
    };
    this.createModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.createModalOpen.set(false);
  }

  submitCreate(): void {
    if (!this.createForm.code.trim() || !this.createForm.name.trim() || !this.createForm.route.trim()) {
      this.toastService.error('Vui lòng điền đầy đủ Mã Menu, Tên hiển thị và Đường dẫn Route');
      return;
    }

    this.actionLoading.set('create');
    this.adminService
      .createMenu(this.createForm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.actionLoading.set(null);
          this.closeCreateModal();
          this.toastService.success(`Đã thêm Menu [${res.data?.name}] thành công!`);
          this.loadCatalog();
          this.loadMatrix();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Tạo Menu mới thất bại');
        },
      });
  }

  // ─── EDIT MENU ────────────────────────────────────────────────────────────
  openEditModal(menu: MenuItem): void {
    this.editingMenu.set(menu);
    this.editForm = {
      name: menu.name,
      icon: menu.icon,
      route: menu.route,
      sortOrder: menu.sortOrder,
      isActive: menu.isActive,
    };
    this.editModalOpen.set(true);
  }

  closeEditModal(): void {
    this.editModalOpen.set(false);
    this.editingMenu.set(null);
  }

  submitEdit(): void {
    const menu = this.editingMenu();
    if (!menu) return;

    if (!this.editForm.name.trim() || !this.editForm.route.trim()) {
      this.toastService.error('Tên menu và Route không được để trống');
      return;
    }

    this.actionLoading.set(menu.id);
    this.adminService
      .updateMenu(menu.id, this.editForm)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.closeEditModal();
          this.toastService.success('Cập nhật Menu thành công!');
          this.loadCatalog();
          this.loadMatrix();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Cập nhật Menu thất bại');
        },
      });
  }

  // ─── TOGGLE ACTIVE ────────────────────────────────────────────────────────
  toggleActive(menu: MenuItem): void {
    const updatedStatus = !menu.isActive;
    this.actionLoading.set(menu.id);
    this.adminService
      .updateMenu(menu.id, {
        name: menu.name,
        icon: menu.icon,
        route: menu.route,
        sortOrder: menu.sortOrder,
        isActive: updatedStatus,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.toastService.success(`Đã ${updatedStatus ? 'bật' : 'tắt'} Menu [${menu.name}]`);
          this.loadCatalog();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Không thể đổi trạng thái');
        },
      });
  }

  // ─── DELETE MENU ──────────────────────────────────────────────────────────
  openDeleteModal(menu: MenuItem): void {
    this.deletingMenu.set(menu);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    this.deleteModalOpen.set(false);
    this.deletingMenu.set(null);
  }

  confirmDelete(): void {
    const menu = this.deletingMenu();
    if (!menu) return;

    this.actionLoading.set(menu.id);
    this.adminService
      .deleteMenu(menu.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.actionLoading.set(null);
          this.closeDeleteModal();
          this.toastService.success(`Đã xóa Menu [${menu.name}]`);
          this.loadCatalog();
          this.loadMatrix();
        },
        error: (err) => {
          this.actionLoading.set(null);
          this.toastService.error(err?.error?.message || 'Xóa Menu thất bại');
        },
      });
  }
}

