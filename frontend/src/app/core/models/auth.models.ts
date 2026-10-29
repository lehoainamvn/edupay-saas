/**
 * Shared Auth Models
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

export interface BackendUserDto {
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

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  accessTokenExpiresAt?: number;
  user?: BackendUserDto;
  // Fallback flat fields
  userId?: string;
  tenantId?: string;
  fullName?: string;
  phone?: string;
  roles?: string[];
  subscriptionPlan?: string;
  subscriptionStatus?: string;
}

export interface UserInfo {
  userId: string;
  tenantId: string;
  fullName: string;
  phone: string;
  roles?: string[];
  subscriptionPlan?: string;
  subscriptionStatus?: string;
}
