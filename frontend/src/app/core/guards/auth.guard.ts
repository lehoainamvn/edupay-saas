import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '../services/token.service';

/**
 * Auth Guard kiểm tra phiên đăng nhập của người dùng.
 * Bắt buộc người dùng phải đăng nhập thành công (có Token hợp lệ trong TokenService/localStorage)
 * thì mới được phép truy cập vào Dashboard và các màn hình quản trị nội bộ.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.isAuthenticated()) {
    return true;
  }

  // Chưa đăng nhập -> Lưu lại URL muốn vào (returnUrl) và điều hướng sang trang Đăng nhập
  router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
