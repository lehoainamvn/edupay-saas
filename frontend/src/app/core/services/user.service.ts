import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { AuthService } from './auth.service';
import { TokenService } from './token.service';
import { UserInfo } from '../models/auth.models';

export interface UpdateProfileRequest {
  fullName: string;
  phone: string;
  email?: string;
  avatarUrl?: string;
}

export interface UserProfileResponse {
  userId: string;
  tenantId: string;
  fullName: string;
  phone: string;
  email?: string;
  avatarUrl?: string;
  isOwner?: boolean;
  roles?: string[];
  subscriptionPlan?: string;
  subscriptionStatus?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private tokenService = inject(TokenService);

  private readonly API = `${environment.apiBaseUrl}/users/profile`;

  /** Lấy thông tin hồ sơ từ backend hoặc local */
  getProfile(): Observable<UserProfileResponse> {
    return this.http.get<ApiResponse<UserProfileResponse> | UserProfileResponse>(this.API).pipe(
      map(res => {
        if (res && 'data' in res && (res as ApiResponse<UserProfileResponse>).data) {
          return (res as ApiResponse<UserProfileResponse>).data;
        }
        return res as UserProfileResponse;
      }),
      tap(profile => {
        if (profile) {
          this.syncToAuth(profile);
        }
      }),
      catchError(() => {
        // Fallback to local storage if backend is not reachable
        const cur = this.authService.currentUser();
        const fallback: UserProfileResponse = {
          userId: cur?.userId || '',
          tenantId: cur?.tenantId || '',
          fullName: cur?.fullName || 'Giáo viên',
          phone: cur?.phone || '',
          email: cur?.email || '',
          avatarUrl: cur?.avatarUrl || '',
          roles: cur?.roles || ['ROLE_TEACHER'],
          subscriptionPlan: cur?.subscriptionPlan || 'TRIAL',
          subscriptionStatus: cur?.subscriptionStatus || 'ACTIVE'
        };
        return of(fallback);
      })
    );
  }

  /** Cập nhật hồ sơ */
  updateProfile(request: UpdateProfileRequest): Observable<UserProfileResponse> {
    return this.http.put<ApiResponse<UserProfileResponse> | UserProfileResponse>(this.API, request).pipe(
      map(res => {
        if (res && 'data' in res && (res as ApiResponse<UserProfileResponse>).data) {
          return (res as ApiResponse<UserProfileResponse>).data;
        }
        return res as UserProfileResponse;
      }),
      tap(profile => {
        if (profile) {
          this.syncToAuth(profile);
        }
      }),
      catchError(err => {
        console.warn('Backend updateProfile failed or not reachable, saving locally:', err);
        // Fallback update locally so teacher always sees their edits instantly
        const cur = this.authService.currentUser();
        const updated: UserProfileResponse = {
          userId: cur?.userId || '',
          tenantId: cur?.tenantId || '',
          fullName: request.fullName.trim(),
          phone: request.phone.trim(),
          email: request.email?.trim() || cur?.email,
          avatarUrl: request.avatarUrl?.trim() || cur?.avatarUrl,
          roles: cur?.roles || ['ROLE_TEACHER'],
          subscriptionPlan: cur?.subscriptionPlan || 'TRIAL',
          subscriptionStatus: cur?.subscriptionStatus || 'ACTIVE'
        };
        this.syncToAuth(updated);
        return of(updated);
      })
    );
  }

  private syncToAuth(profile: UserProfileResponse): void {
    const cur = this.authService.currentUser();
    const updatedUser: UserInfo = {
      userId: profile.userId || cur?.userId || '',
      tenantId: profile.tenantId || cur?.tenantId || '',
      fullName: profile.fullName || cur?.fullName || 'Giáo viên',
      phone: profile.phone || cur?.phone || '',
      email: profile.email !== undefined ? profile.email : cur?.email,
      avatarUrl: profile.avatarUrl !== undefined ? profile.avatarUrl : cur?.avatarUrl,
      isOwner: profile.isOwner !== undefined ? profile.isOwner : cur?.isOwner,
      roles: profile.roles || cur?.roles || ['ROLE_TEACHER'],
      subscriptionPlan: profile.subscriptionPlan || cur?.subscriptionPlan || 'TRIAL',
      subscriptionStatus: profile.subscriptionStatus || cur?.subscriptionStatus || 'ACTIVE'
    };

    this.tokenService.setUserInfo(updatedUser);
    this.authService.currentUser.set(updatedUser);
  }
}

