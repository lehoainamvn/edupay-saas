import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of, catchError } from 'rxjs';
import { RbacService } from '../services/rbac.service';
import { ToastService } from '../services/toast.service';

/**
 * Route Guard kiểm tra quyền hạn (Permissions) và Feature Flags trước khi kích hoạt route.
 *
 * Cấu hình trong app.routes.ts:
 *   {
 *     path: 'classes',
 *     canActivate: [authGuard, permissionGuard],
 *     data: { permission: 'class:read' },
 *     loadComponent: ...
 *   }
 */
export const permissionGuard: CanActivateFn = (route, state) => {
  const rbacService = inject(RbacService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  const requiredPermission = (route.data['permission'] || route.data['permissions']) as string | string[] | undefined;
  const permissionMode = (route.data['permissionMode'] || 'OR') as 'AND' | 'OR';
  const requiredFeature = route.data['feature'] as string | undefined;

  const checkAccess = (): boolean => {
    // 1. Kiểm tra Feature Flag (nếu có yêu cầu)
    if (requiredFeature && !rbacService.hasFeature(requiredFeature)) {
      toastService.warning('Tính năng này chưa được kích hoạt cho gói dịch vụ của bạn');
      router.navigate(['/dashboard']);
      return false;
    }

    // 2. Kiểm tra Permission (nếu có yêu cầu)
    if (requiredPermission && !rbacService.hasPermission(requiredPermission, permissionMode)) {
      toastService.error('Bạn không có quyền truy cập vào mục này');
      router.navigate(['/dashboard']);
      return false;
    }

    return true;
  };

  // Nếu navigation/permissions đã được tải vào RAM
  if (rbacService.isLoaded()) {
    return checkAccess();
  }

  // Nếu chưa tải (F5 trực tiếp trang), tiến hành gọi API tải trước
  return rbacService.loadNavigation().pipe(
    map(() => checkAccess()),
    catchError(() => {
      toastService.error('Không thể xác thực quyền hạn người dùng');
      router.navigate(['/dashboard']);
      return of(false);
    })
  );
};

