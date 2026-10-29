import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenService } from './token.service';
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
    return this.http.post<AuthResponse>(`${this.API}/login`, request).pipe(
      tap(res => this.handleAuthSuccess(res)),
      catchError(err => throwError(() => err))
    );
  }

  // ─── Đăng ký ──────────────────────────────────────────────────────────────
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register`, request).pipe(
      tap(res => this.handleAuthSuccess(res)),
      catchError(err => throwError(() => err))
    );
  }

  // ─── Refresh Access Token ─────────────────────────────────────────────────
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http.post<AuthResponse>(`${this.API}/refresh-token`, { refreshToken }).pipe(
      tap(res => this.handleAuthSuccess(res)),
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
        error: () => {} // Fire-and-forget, bỏ qua lỗi
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
    this.tokenService.setToken(res.accessToken);
    this.tokenService.setRefreshToken(res.refreshToken);
    this.tokenService.setTenantId(res.tenantId);
    const userInfo: UserInfo = {
      userId: res.userId,
      tenantId: res.tenantId,
      fullName: res.fullName,
      phone: res.phone,
      roles: res.roles,
    };
    this.tokenService.setUserInfo(userInfo);
    this.currentUser.set(userInfo);
    this.refreshTokenSubject.next(res.accessToken);
  }
}
