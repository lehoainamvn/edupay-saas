import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, RoleItem, PermissionGroupItem } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-roles.component.html',
  styleUrls: ['./admin-roles.component.css']
})
export class AdminRolesComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly toastService = inject(ToastService);

  loading = signal<boolean>(false);
  isSaving = signal<boolean>(false);

  roles = signal<RoleItem[]>([]);
  permissionGroups = signal<PermissionGroupItem[]>([]);

  selectedRole = signal<RoleItem | null>(null);
  selectedPermissions = signal<Set<string>>(new Set());

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.adminService.getRoles().subscribe({
      next: (resRoles) => {
        const rolesList = resRoles.data || [];
        this.roles.set(rolesList);

        this.adminService.getPermissions().subscribe({
          next: (resPerms) => {
            this.loading.set(false);
            const groups = resPerms.data || [];
            this.permissionGroups.set(groups);

            // Default select the first role (e.g. SUPER_ADMIN or OWNER)
            if (rolesList.length > 0) {
              this.selectRole(rolesList[0]);
            }
          },
          error: (err) => {
            this.loading.set(false);
            this.toastService.error(err.error?.message || 'Không thể tải danh mục quyền');
          }
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.toastService.error(err.error?.message || 'Không thể tải danh sách vai trò');
      }
    });
  }

  selectRole(role: RoleItem): void {
    this.selectedRole.set(role);
    this.selectedPermissions.set(new Set(role.permissions || []));
  }

  isPermissionGranted(code: string): boolean {
    return this.selectedPermissions().has(code);
  }

  togglePermission(code: string): void {
    const current = new Set(this.selectedPermissions());
    if (current.has(code)) {
      current.delete(code);
    } else {
      current.add(code);
    }
    this.selectedPermissions.set(current);
  }

  isGroupAllChecked(group: PermissionGroupItem): boolean {
    if (!group.permissions || group.permissions.length === 0) return false;
    return group.permissions.every(p => this.selectedPermissions().has(p.code));
  }

  toggleGroup(group: PermissionGroupItem): void {
    const current = new Set(this.selectedPermissions());
    const allChecked = this.isGroupAllChecked(group);

    group.permissions.forEach(p => {
      if (allChecked) {
        current.delete(p.code);
      } else {
        current.add(p.code);
      }
    });

    this.selectedPermissions.set(current);
  }

  selectAllPermissions(): void {
    const allCodes = new Set<string>();
    this.permissionGroups().forEach(g => {
      g.permissions.forEach(p => allCodes.add(p.code));
    });
    this.selectedPermissions.set(allCodes);
  }

  deselectAllPermissions(): void {
    this.selectedPermissions.set(new Set());
  }

  savePermissions(): void {
    const role = this.selectedRole();
    if (!role) return;

    this.isSaving.set(true);
    const codes = Array.from(this.selectedPermissions());

    this.adminService.updateRolePermissions(role.id, codes).subscribe({
      next: (res) => {
        this.isSaving.set(false);
        this.toastService.success(`Đã cập nhật ${codes.length} quyền cho vai trò ${role.name}`);

        // Update local role item in the list
        if (res.data) {
          const updated = this.roles().map(r => r.id === role.id ? res.data : r);
          this.roles.set(updated);
          this.selectedRole.set(res.data);
        }
      },
      error: (err) => {
        this.isSaving.set(false);
        this.toastService.error(err.error?.message || 'Lỗi khi lưu phân quyền');
      }
    });
  }

  getRoleBadgeClass(roleCode: string): string {
    const upper = (roleCode || '').toUpperCase();
    if (upper.includes('SUPER_ADMIN')) return 'badge-superadmin';
    if (upper.includes('OWNER')) return 'badge-owner';
    if (upper.includes('TEACHER')) return 'badge-teacher';
    return 'badge-viewer';
  }
}

