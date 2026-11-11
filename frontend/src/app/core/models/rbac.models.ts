export interface MenuItem {
  code: string;
  name: string;
  icon?: string;
  route: string;
  sortOrder: number;
  badge?: string;
  children?: MenuItem[];
}

export interface UserNavigation {
  userId: string;
  tenantId: string;
  fullName: string;
  phone: string;
  isOwner: boolean;
  tenantType?: 'CENTER' | 'SOLO_TUTOR' | string;
  roles: string[];
  permissions: string[];
  features: Record<string, boolean>;
  menus: MenuItem[];
}

export interface FeatureFlag {
  featureId: string;
  name: string;
  description: string;
  category: 'CORE' | 'ADDON' | 'PREMIUM';
  enabled: boolean;
  source: 'PLAN' | 'TENANT_OVERRIDE' | 'USER_OVERRIDE' | 'DEFAULT';
  reason?: string;
  expiresAt?: string;
}

export interface OverrideFeatureRequest {
  isEnabled: boolean;
  reason?: string;
  expiresAt?: string;
}

