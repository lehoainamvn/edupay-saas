import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, BehaviorSubject, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenService } from './token.service';
import { ApiResponse } from '../../shared/models/api-response.model';
import { AuthResponse, LoginRequest, RegisterRequest, UserInfo } from '../models/auth.models';

export type { AuthResponse, LoginRequest, RegisterRequest, UserInfo };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private tokenService = inject(TokenService);

  private readonly API = `${environment.apiBaseUrl}/auth`;

  // Signal công khai để các component subscribe trực tiếp
  currentUser = signal<UserInfo | null>(this.tokenService.getUserInfo());

  // Subject dùng cho interceptor auto-refresh (tránh nhiều req cùng refresh)
  private refreshTokenInProgress = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);
  get refreshSubject() { return this.refreshTokenSubject; }
  get isRefreshing() { return this.refreshTokenInProgress; }
  set isRefreshing(val: boolean) { this.refreshTokenInProgress = val; }

  // ─── Đăng nhập ────────────────────────────────────────────────────────────
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse> | AuthResponse>(`${this.API}/login`, request).pipe(
      map(res => {
        // Backend bọc trong ApiResponse<AuthResponse> { success: true, data: { ... } }
        if (res && 'data' in res && (res as ApiResponse<AuthResponse>).data) {
          return (res as ApiResponse<AuthResponse>).data;
        }
        return res as AuthResponse;
      }),
      tap(authData => this.handleAuthSuccess(authData)),
      catchError(err => throwError(() => err))
    );
  }

  // ─── Đăng ký ──────────────────────────────────────────────────────────────
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse> | AuthResponse>(`${this.API}/register`, request).pipe(
      map(res => {
        if (res && 'data' in res && (res as ApiResponse<AuthResponse>).data) {
          return (res as ApiResponse<AuthResponse>).data;
        }
        return res as AuthResponse;
      }),
      tap(authData => this.handleAuthSuccess(authData)),
      catchError(err => throwError(() => err))
    );
  }

  // ─── Refresh Access Token ─────────────────────────────────────────────────
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http.post<ApiResponse<AuthResponse> | AuthResponse>(`${this.API}/refresh-token`, { refreshToken }).pipe(
      map(res => {
        if (res && 'data' in res && (res as ApiResponse<AuthResponse>).data) {
          return (res as ApiResponse<AuthResponse>).data;
        }
        return res as AuthResponse;
      }),
      tap(authData => this.handleAuthSuccess(authData)),
      catchError(err => {
        this.logout();
        return throwError(() => err);
      })
    );
  }

  // ─── Đăng xuất ────────────────────────────────────────────────────────────
  logout(): void {
    const refreshToken = this.tokenService.getRefreshToken();
    if (refreshToken) {
      this.http.post(`${this.API}/logout`, { refreshToken }).subscribe({
        error: () => {} // Fire-and-forget
      });
    }
    this.tokenService.clear();
    this.currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  isAuthenticated(): boolean {
    return this.tokenService.isAuthenticated();
  }

  private handleAuthSuccess(res: AuthResponse): void {
    if (!res) return;

    const token = res.accessToken;
    const refreshToken = res.refreshToken;
    const userObj = res.user;

    const tenantId = userObj?.tenantId || res.tenantId || '';
    const userId = userObj?.userId || res.userId || '';
    const fullName = userObj?.fullName || res.fullName || 'Giáo viên';
    const phone = userObj?.phone || res.phone || '';
    const roles = userObj?.roles || res.roles || ['ROLE_TEACHER'];
    const plan = userObj?.subscriptionPlan || res.subscriptionPlan || 'Gói Thuê Tháng';
    const status = userObj?.subscriptionStatus || res.subscriptionStatus || 'ACTIVE';

    if (token) this.tokenService.setToken(token);
    if (refreshToken) this.tokenService.setRefreshToken(refreshToken);
    if (tenantId) this.tokenService.setTenantId(tenantId);

    const userInfo: UserInfo = {
      userId,
      tenantId,
      fullName,
      phone,
      roles,
      subscriptionPlan: plan,
      subscriptionStatus: status,
    };

    this.tokenService.setUserInfo(userInfo);
    this.currentUser.set(userInfo);
    if (token) this.refreshTokenSubject.next(token);
  }
}
