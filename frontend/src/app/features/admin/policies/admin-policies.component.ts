import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AdminService, TenantTeacherPolicy, UpdateTenantTeacherPolicyPayload } from '../../../core/services/admin.service';
import { CenterTeacherService, CenterTeacherItem } from '../../../core/services/center-teacher.service';
import { TeacherPolicyService } from '../../../core/services/teacher-policy.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-policies',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-policies.component.html',
  styleUrls: ['./admin-policies.component.css'],
})
export class AdminPoliciesComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly teacherService = inject(CenterTeacherService);
  private readonly teacherPolicyService = inject(TeacherPolicyService);
  private readonly toastService = inject(ToastService);

  loading = signal<boolean>(true);
  saving = signal<boolean>(false);
  teachers = signal<CenterTeacherItem[]>([]);

  policy = signal<TenantTeacherPolicy>({
    id: '',
    tenantId: '',
    canEditStudentFee: false,
    canCreateClass: false,
    canCreateStudent: false,
    canViewBilling: false,
    updatedAt: '',
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);

    this.adminService.getCenterPolicy().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.policy.set(res.data);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toastService.error('Không thể tải chính sách phân quyền của cơ sở');
      },
    });

    this.teacherService.getTeachers().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.teachers.set(res.data);
        }
      },
      error: () => {},
    });
  }

  toggle(key: keyof UpdateTenantTeacherPolicyPayload, label: string): void {
    const cur = this.policy();
    const nextVal = !cur[key];
    const updated = {
      ...cur,
      [key]: nextVal,
    };
    this.policy.set(updated);
    this.savePolicy(label, nextVal);
  }

  savePolicy(label?: string, nextVal?: boolean): void {
    this.saving.set(true);
    const p = this.policy();
    const payload: UpdateTenantTeacherPolicyPayload = {
      canEditStudentFee: p.canEditStudentFee,
      canCreateClass: p.canCreateClass,
      canCreateStudent: p.canCreateStudent,
      canViewBilling: p.canViewBilling,
    };

    this.adminService.updateCenterPolicy(payload).subscribe({
      next: (res) => {
        this.saving.set(false);
        if (res.success && res.data) {
          this.policy.set(res.data);
          this.teacherPolicyService.updateLocalPolicy({
            canCreateClass: res.data.canCreateClass,
            canCreateStudent: res.data.canCreateStudent,
            canEditStudentFee: res.data.canEditStudentFee,
            canViewBilling: res.data.canViewBilling,
          });
          if (label && nextVal !== undefined) {
            this.toastService.success(`Đã cập nhật: ${label} -> ${nextVal ? 'CHO PHÉP' : 'KHÓA'}`);
          } else {
            this.toastService.success('Đã lưu cấu hình phân quyền giáo viên cơ sở');
          }
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.toastService.error(err?.error?.message || 'Có lỗi khi lưu chính sách phân quyền');
      },
    });
  }
}

