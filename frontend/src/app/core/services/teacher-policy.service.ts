import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, catchError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { AuthService } from './auth.service';

export interface TeacherActivePolicy {
  canCreateClass: boolean;
  canCreateStudent: boolean;
  canEditStudentFee: boolean;
  canViewBilling: boolean;
}

const DEFAULT_FULL_POLICY: TeacherActivePolicy = {
  canCreateClass: true,
  canCreateStudent: true,
  canEditStudentFee: true,
  canViewBilling: true,
};

@Injectable({
  providedIn: 'root',
})
export class TeacherPolicyService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiBase = environment.apiBaseUrl;

  private readonly policySignal = signal<TeacherActivePolicy>(DEFAULT_FULL_POLICY);
  readonly loading = signal<boolean>(false);

  readonly policy = this.policySignal.asReadonly();
  readonly canCreateClass = computed(() => this.policySignal().canCreateClass);
  readonly canCreateStudent = computed(() => this.policySignal().canCreateStudent);
  readonly canEditStudentFee = computed(() => this.policySignal().canEditStudentFee);
  readonly canViewBilling = computed(() => this.policySignal().canViewBilling);

  constructor() {
    this.loadPolicy().subscribe();
  }

  loadPolicy(): Observable<ApiResponse<TeacherActivePolicy>> {
    // Nếu là SuperAdmin, Chủ trung tâm hoặc Gia sư tự do: luôn có toàn quyền
    if (this.authService.isSuperAdmin() || this.authService.isCenterAdmin()) {
      this.policySignal.set(DEFAULT_FULL_POLICY);
      return of({
        success: true,
        message: 'Toàn quyền điều hành',
        data: DEFAULT_FULL_POLICY,
        timestamp: new Date().toISOString(),
      });
    }

    const currentUser = this.authService.currentUser();
    const isOwner = Boolean(currentUser?.isOwner);
    const tenantType = currentUser?.tenantType || '';

    if (isOwner || tenantType === 'SOLO_TUTOR') {
      this.policySignal.set(DEFAULT_FULL_POLICY);
      return of({
        success: true,
        message: 'Toàn quyền tài khoản độc lập',
        data: DEFAULT_FULL_POLICY,
        timestamp: new Date().toISOString(),
      });
    }

    this.loading.set(true);
    return this.http
      .get<ApiResponse<TeacherActivePolicy>>(`${this.apiBase}/center/policies/my-policy`)
      .pipe(
        tap((res) => {
          this.loading.set(false);
          if (res?.success && res.data) {
            this.policySignal.set({
              canCreateClass: Boolean(res.data.canCreateClass),
              canCreateStudent: Boolean(res.data.canCreateStudent),
              canEditStudentFee: Boolean(res.data.canEditStudentFee),
              canViewBilling: Boolean(res.data.canViewBilling),
            });
          }
        }),
        catchError((err) => {
          this.loading.set(false);
          // Fallback an toàn cho giáo viên trung tâm nếu lỗi mạng
          return of({
            success: false,
            message: err?.error?.message || 'Không thể tải phân quyền giáo viên',
            data: this.policySignal(),
            timestamp: new Date().toISOString(),
          });
        })
      );
  }

  updateLocalPolicy(policy: Partial<TeacherActivePolicy>): void {
    this.policySignal.update((current) => ({
      ...current,
      ...policy,
    }));
  }
}

