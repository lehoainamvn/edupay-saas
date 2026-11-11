import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';
import { ToastService } from '../services/toast.service';

/**
 * Admin Guard: Chỉ cho phép người dùng có vai trò SUPER_ADMIN
 * truy cập vào không gian Quản trị viên /admin/**.
 * Các tài khoản giáo viên/chủ cơ sở thông thường sẽ bị chặn và điều hướng về /dashboard.
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  if (!tokenService.isAuthenticated()) {
    router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  const userInfo = tokenService.getUserInfo();
  const roles = userInfo?.roles || [];
  const isSuperAdmin = roles.some(r => {
    const upper = r.toUpperCase();
    return upper.includes('SUPER_ADMIN') || upper.includes('SUPERADMIN') || upper === 'ROLE_SUPER_ADMIN';
  });

  if (!isSuperAdmin) {
    toastService.error('Bạn không có quyền truy cập vào Cổng Quản Trị Hệ Thống');
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};

