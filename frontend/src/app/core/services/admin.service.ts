import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';

export interface TenantAdminItem {
  id: string;
  name: string;
  phone: string;
  email: string;
  tenantType?: 'CENTER' | 'SOLO_TUTOR' | string;
  subscriptionPlan: string;
  subscriptionStatus: 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' | string;
  subscriptionEndsAt: string;
  createdAt: string;
  totalTeachers?: number;
  totalClasses: number;
  totalStudents: number;
  isExpired: boolean;
  representativeName?: string;
  representativePhone?: string;
}

export interface PlatformStats {
  mrr: number;
  arr?: number;
  newTeachersThisMonth: number;
  renewalRate: number;
  totalTenants: number;
  totalCenters?: number;
  totalTutors?: number;
  totalTeachers?: number;
  totalStudents?: number;
  totalClasses?: number;
  activeTenants: number;
  suspendedTenants: number;
  trialTenants: number;
}

export interface TenantExtendPayload {
  duration: '1_MONTH' | '1_YEAR';
  plan?: string;
  note?: string;
}

export interface CreateCenterPayload {
  name: string;
  phone: string;
  email?: string;
  representativeName: string;
  password?: string;
  subscriptionPlan?: string;
  durationMonths?: number;
}

export interface UpdateCenterPayload {
  name: string;
  email?: string;
  subscriptionPlan?: string;
  subscriptionStatus?: string;
  subscriptionEndsAt?: string;
  representativeName?: string;
}

export interface CenterTeacherItem {
  id: string;
  fullName: string;
  phone: string;
  email?: string;
  status: string;
  assignedClassesCount: number;
  createdAt: string;
}

export interface CreateTutorPayload {
  fullName: string;
  phone: string;
  email?: string;
  password?: string;
  subscriptionPlan?: string;
  durationMonths?: number;
}

export interface UpdateTutorPayload {
  fullName: string;
  email?: string;
  subscriptionPlan?: string;
  subscriptionStatus?: string;
  subscriptionEndsAt?: string;
}

export interface MenuItem {
  id: string;
  code: string;
  name: string;
  icon: string;
  route: string;
  parentId?: string;
  sortOrder: number;
  featureId?: string;
  isActive: boolean;
  createdAt?: string;
}

export interface CreateMenuPayload {
  code: string;
  name: string;
  icon?: string;
  route: string;
  parentId?: string;
  sortOrder?: number;
  featureId?: string;
  isActive?: boolean;
}

export interface UpdateMenuPayload {
  name: string;
  icon?: string;
  route: string;
  parentId?: string;
  sortOrder?: number;
  featureId?: string;
  isActive?: boolean;
}

export interface RoleMenuMatrixResponse {
  roles: RoleItem[];
  menus: MenuItem[];
  roleMenuMap: Record<string, string[]>;
}

export interface TenantTeacherPolicy {
  id: string;
  tenantId: string;
  canEditStudentFee: boolean;
  canCreateClass: boolean;
  canCreateStudent: boolean;
  canViewBilling: boolean;
  updatedAt: string;
}

export interface UpdateTenantTeacherPolicyPayload {
  canEditStudentFee?: boolean;
  canCreateClass?: boolean;
  canCreateStudent?: boolean;
  canViewBilling?: boolean;
}

export interface AdminUserItem {
  id: string;
  tenantId: string;
  tenantName?: string;
  phone: string;
  email?: string;
  fullName: string;
  avatarUrl?: string;
  status: 'ACTIVE' | 'SUSPENDED' | string;
  isOwner: boolean;
  roles: string[];
  lastLoginAt?: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface CreateUserPayload {
  phone: string;
  fullName: string;
  email?: string;
  password: string;
  roleCode?: string;
  tenantId?: string;
}

export interface UpdateUserPayload {
  fullName: string;
  email?: string;
  status?: string;
  roles?: string[];
}

export interface RoleItem {
  id: string;
  code: string;
  name: string;
  description: string;
  isSystem: boolean;
  isActive: boolean;
  userCount: number;
  permissions?: string[];
}

export interface PermissionItem {
  id: string;
  code: string;
  name: string;
  action: string;
}

export interface PermissionGroupItem {
  resource: string;
  resourceName: string;
  permissions: PermissionItem[];
}

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;
  private readonly apiBase = environment.apiBaseUrl;

  // ─── PLATFORM DASHBOARD STATS ─────────────────────────────────────────────
  getPlatformStats(): Observable<ApiResponse<PlatformStats>> {
    return this.http.get<ApiResponse<PlatformStats>>(`${this.baseUrl}/platform/stats`);
  }

  // ─── TENANTS CHUNG ────────────────────────────────────────────────────────
  getTenants(status?: string, search?: string): Observable<ApiResponse<TenantAdminItem[]>> {
    let params = new HttpParams();
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<ApiResponse<TenantAdminItem[]>>(`${this.baseUrl}/tenants`, { params });
  }

  updateTenantStatus(id: string, status: string): Observable<ApiResponse<TenantAdminItem>> {
    return this.http.patch<ApiResponse<TenantAdminItem>>(`${this.baseUrl}/tenants/${id}/status`, {
      status,
    });
  }

  extendSubscription(id: string, payload: TenantExtendPayload): Observable<ApiResponse<TenantAdminItem>> {
    return this.http.post<ApiResponse<TenantAdminItem>>(
      `${this.baseUrl}/tenants/${id}/extend`,
      payload
    );
  }

  // ─── TRUNG TÂM ĐÀO TẠO (CENTERS) ──────────────────────────────────────────
  getCenters(status?: string, search?: string): Observable<ApiResponse<TenantAdminItem[]>> {
    let params = new HttpParams();
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<ApiResponse<TenantAdminItem[]>>(`${this.baseUrl}/centers`, { params });
  }

  getCenterById(id: string): Observable<ApiResponse<TenantAdminItem>> {
    return this.http.get<ApiResponse<TenantAdminItem>>(`${this.baseUrl}/centers/${id}`);
  }

  createCenter(payload: CreateCenterPayload): Observable<ApiResponse<TenantAdminItem>> {
    return this.http.post<ApiResponse<TenantAdminItem>>(`${this.baseUrl}/centers`, payload);
  }

  updateCenter(id: string, payload: UpdateCenterPayload): Observable<ApiResponse<TenantAdminItem>> {
    return this.http.put<ApiResponse<TenantAdminItem>>(`${this.baseUrl}/centers/${id}`, payload);
  }

  deleteCenter(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/centers/${id}`);
  }

  getCenterTeachers(id: string): Observable<ApiResponse<CenterTeacherItem[]>> {
    return this.http.get<ApiResponse<CenterTeacherItem[]>>(`${this.baseUrl}/centers/${id}/teachers`);
  }

  // ─── GIA SƯ TỰ DO (TUTORS) ────────────────────────────────────────────────
  getTutors(status?: string, search?: string): Observable<ApiResponse<TenantAdminItem[]>> {
    let params = new HttpParams();
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<ApiResponse<TenantAdminItem[]>>(`${this.baseUrl}/tutors`, { params });
  }

  getTutorById(id: string): Observable<ApiResponse<TenantAdminItem>> {
    return this.http.get<ApiResponse<TenantAdminItem>>(`${this.baseUrl}/tutors/${id}`);
  }

  createTutor(payload: CreateTutorPayload): Observable<ApiResponse<TenantAdminItem>> {
    return this.http.post<ApiResponse<TenantAdminItem>>(`${this.baseUrl}/tutors`, payload);
  }

  updateTutor(id: string, payload: UpdateTutorPayload): Observable<ApiResponse<TenantAdminItem>> {
    return this.http.put<ApiResponse<TenantAdminItem>>(`${this.baseUrl}/tutors/${id}`, payload);
  }

  deleteTutor(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/tutors/${id}`);
  }

  // ─── MENU ĐỘNG & PHÂN QUYỀN MENU ──────────────────────────────────────────
  getMenus(): Observable<ApiResponse<MenuItem[]>> {
    return this.http.get<ApiResponse<MenuItem[]>>(`${this.baseUrl}/menus`);
  }

  createMenu(payload: CreateMenuPayload): Observable<ApiResponse<MenuItem>> {
    return this.http.post<ApiResponse<MenuItem>>(`${this.baseUrl}/menus`, payload);
  }

  updateMenu(id: string, payload: UpdateMenuPayload): Observable<ApiResponse<MenuItem>> {
    return this.http.put<ApiResponse<MenuItem>>(`${this.baseUrl}/menus/${id}`, payload);
  }

  deleteMenu(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/menus/${id}`);
  }

  getRoleMenuMatrix(): Observable<ApiResponse<RoleMenuMatrixResponse>> {
    return this.http.get<ApiResponse<RoleMenuMatrixResponse>>(`${this.baseUrl}/menus/role-matrix`);
  }

  updateRoleMenus(roleId: string, menuIds: string[]): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.baseUrl}/menus/role-matrix`, {
      roleId,
      menuIds,
    });
  }

  // ─── TRUNG TÂM: CHÍNH SÁCH GIÁO VIÊN NỘI BỘ (CENTER POLICY) ───────────────
  getCenterPolicy(tenantId?: string): Observable<ApiResponse<TenantTeacherPolicy>> {
    let params = new HttpParams();
    if (tenantId) params = params.set('tenantId', tenantId);
    return this.http.get<ApiResponse<TenantTeacherPolicy>>(`${this.apiBase}/center/policies`, { params });
  }

  updateCenterPolicy(payload: UpdateTenantTeacherPolicyPayload, tenantId?: string): Observable<ApiResponse<TenantTeacherPolicy>> {
    let params = new HttpParams();
    if (tenantId) params = params.set('tenantId', tenantId);
    return this.http.put<ApiResponse<TenantTeacherPolicy>>(`${this.apiBase}/center/policies`, payload, { params });
  }

  // ─── USER MANAGEMENT (CRUD) ───────────────────────────────────────────────
  getUsers(filters: {
    page?: number;
    size?: number;
    search?: string;
    role?: string;
    status?: string;
    tenantId?: string;
  }): Observable<ApiResponse<PageResponse<AdminUserItem>>> {
    let params = new HttpParams()
      .set('page', (filters.page ?? 0).toString())
      .set('size', (filters.size ?? 10).toString());

    if (filters.search && filters.search.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.role && filters.role !== 'ALL') {
      params = params.set('role', filters.role);
    }
    if (filters.status && filters.status !== 'ALL') {
      params = params.set('status', filters.status);
    }
    if (filters.tenantId) {
      params = params.set('tenantId', filters.tenantId);
    }

    return this.http.get<ApiResponse<PageResponse<AdminUserItem>>>(`${this.baseUrl}/users`, { params });
  }

  getUserById(id: string): Observable<ApiResponse<AdminUserItem>> {
    return this.http.get<ApiResponse<AdminUserItem>>(`${this.baseUrl}/users/${id}`);
  }

  createUser(payload: CreateUserPayload): Observable<ApiResponse<AdminUserItem>> {
    return this.http.post<ApiResponse<AdminUserItem>>(`${this.baseUrl}/users`, payload);
  }

  updateUser(id: string, payload: UpdateUserPayload): Observable<ApiResponse<AdminUserItem>> {
    return this.http.put<ApiResponse<AdminUserItem>>(`${this.baseUrl}/users/${id}`, payload);
  }

  updateUserStatus(id: string, status: string): Observable<ApiResponse<AdminUserItem>> {
    return this.http.patch<ApiResponse<AdminUserItem>>(`${this.baseUrl}/users/${id}/status`, { status });
  }

  resetPassword(id: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/users/${id}/reset-password`, { newPassword });
  }

  deleteUser(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/users/${id}`);
  }

  // ─── ROLES & PERMISSIONS MATRIX ───────────────────────────────────────────
  getRoles(): Observable<ApiResponse<RoleItem[]>> {
    return this.http.get<ApiResponse<RoleItem[]>>(`${this.baseUrl}/roles`);
  }

  getPermissions(): Observable<ApiResponse<PermissionGroupItem[]>> {
    return this.http.get<ApiResponse<PermissionGroupItem[]>>(`${this.baseUrl}/roles/permissions`);
  }

  updateRolePermissions(roleId: string, permissionCodes: string[]): Observable<ApiResponse<RoleItem>> {
    return this.http.put<ApiResponse<RoleItem>>(`${this.baseUrl}/roles/${roleId}/permissions`, {
      permissionCodes,
    });
  }
}
