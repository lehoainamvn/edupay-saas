import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly TOKEN_KEY = 'edupay_token';
  private readonly TENANT_KEY = 'edupay_tenant_id';

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getTenantId(): string | null {
    return localStorage.getItem(this.TENANT_KEY);
  }

  setTenantId(tenantId: string): void {
    localStorage.setItem(this.TENANT_KEY, tenantId);
  }

  clear(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.TENANT_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}
