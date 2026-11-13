import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * Admin Guard: Cho phép người dùng SUPER_ADMIN và QUẢN LÝ TRUNG TÂM (CENTER_ADMIN)
 * truy cập vào không gian Quản trị viên /admin/**.
 * Các tài khoản giáo viên/gia sư thông thường sẽ bị chặn và điều hướng về /dashboard.
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  if (!tokenService.isAuthenticated()) {
    router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  const isSuperAdmin = authService.isSuperAdmin();
  const isCenterAdmin = authService.isCenterAdmin();

  if (!isSuperAdmin && !isCenterAdmin) {
    toastService.error('Bạn không có quyền truy cập vào Cổng Quản Trị');
    router.navigate(['/dashboard']);
    return false;
  }

  // Nếu là Quản lý Trung tâm nhưng cố truy cập vào các menu Nền tảng của SuperAdmin:
  const superAdminExclusivePaths = ['/admin/centers', '/admin/tutors', '/admin/menus', '/admin/roles', '/admin/system-config'];
  if (!isSuperAdmin && superAdminExclusivePaths.some((p) => state.url.startsWith(p))) {
    toastService.warning('Chức năng này thuộc phân hệ Nền tảng, chỉ dành riêng cho SuperAdmin');
    router.navigate(['/admin/platform']);
    return false;
  }

  return true;
};

