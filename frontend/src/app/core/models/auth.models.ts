/**
 * Shared Auth Models — tách riêng để tránh circular dependency
 * giữa AuthService và TokenService.
 */

export interface LoginRequest {
  phone: string;
  password: string;
}

export interface RegisterRequest {
  phone: string;
  password: string;
  fullName: string;
  centerName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  tenantId: string;
  fullName: string;
  phone: string;
  roles: string[];
}

export interface UserInfo {
  userId: string;
  tenantId: string;
  fullName: string;
  phone: string;
  roles: string[];
}
