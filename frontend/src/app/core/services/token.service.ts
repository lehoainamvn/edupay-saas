import { Injectable } from '@angular/core';
import { UserInfo } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly ACCESS_TOKEN_KEY = 'edupay_token';
  private readonly REFRESH_TOKEN_KEY = 'edupay_refresh_token';
  private readonly TENANT_KEY = 'edupay_tenant_id';
  private readonly USER_INFO_KEY = 'edupay_user_info';

  // ─── Access Token ─────────────────────────────────────────────────────────
  getToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  setToken(token: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  // ─── Refresh Token ────────────────────────────────────────────────────────
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  // ─── Tenant ID ────────────────────────────────────────────────────────────
  getTenantId(): string | null {
    return localStorage.getItem(this.TENANT_KEY);
  }

  setTenantId(tenantId: string): void {
    localStorage.setItem(this.TENANT_KEY, tenantId);
  }

  // ─── User Info ────────────────────────────────────────────────────────────
  getUserInfo(): UserInfo | null {
    const raw = localStorage.getItem(this.USER_INFO_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserInfo;
    } catch {
      return null;
    }
  }

  setUserInfo(info: UserInfo): void {
    localStorage.setItem(this.USER_INFO_KEY, JSON.stringify(info));
  }

  // ─── Clear All ────────────────────────────────────────────────────────────
  clear(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.TENANT_KEY);
    localStorage.removeItem(this.USER_INFO_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}
