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
  subscriptionPlan: string;
  subscriptionStatus: 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' | string;
  subscriptionEndsAt: string;
  createdAt: string;
  totalClasses: number;
  totalStudents: number;
  isExpired: boolean;
}

export interface PlatformStats {
  mrr: number;
  newTeachersThisMonth: number;
  renewalRate: number;
  totalTenants: number;
  activeTenants: number;
  suspendedTenants: number;
  trialTenants: number;
}

export interface TenantExtendPayload {
  duration: '1_MONTH' | '1_YEAR';
  plan?: string;
  note?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

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

  getPlatformStats(): Observable<ApiResponse<PlatformStats>> {
    return this.http.get<ApiResponse<PlatformStats>>(`${this.baseUrl}/platform/stats`);
  }
}
