import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { TokenService } from '../../../core/services/token.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.css'],
})
export class AdminLayoutComponent {
  private readonly tokenService = inject(TokenService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  currentTitle = signal<string>('Tổng quan Doanh thu & Nền tảng');
  userInfo = this.tokenService.getUserInfo();

  readonly navGroups = [
    {
      label: 'VẬN HÀNH NỀN TẢNG',
      items: [
        {
          icon: 'grid',
          label: 'Tổng quan Nền tảng',
          route: '/admin/platform',
          exact: true,
        },
        {
          icon: 'building',
          label: 'Quản lý Trung tâm',
          route: '/admin/centers',
          exact: false,
        },
        {
          icon: 'user',
          label: 'Quản lý Gia sư',
          route: '/admin/tutors',
          exact: false,
        },
      ],
    },
    {
      label: 'HỆ THỐNG & PHÂN QUYỀN',
      items: [
        {
          icon: 'menu',
          label: 'Quản lý Menu Động',
          route: '/admin/menus',
          exact: false,
        },
        {
          icon: 'shield',
          label: 'Phân quyền RBAC',
          route: '/admin/roles',
          exact: false,
        },
        {
          icon: 'users',
          label: 'Tất cả Tài khoản',
          route: '/admin/users',
          exact: false,
        },
        {
          icon: 'settings',
          label: 'Cấu hình Hệ thống',
          route: '/admin/system-config',
          exact: false,
        },
      ],
    },
  ];

  constructor() {
    this.updateCurrentTitle(this.router.url);
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.updateCurrentTitle(event.urlAfterRedirects || event.url);
      });
  }

  userInitials(): string {
    const name = (this.userInfo?.fullName || 'Lê Hoài Nam').trim();
    const parts = name.split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  goToTeacherPortal(): void {
    this.router.navigate(['/dashboard']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  private updateCurrentTitle(url: string): void {
    if (url.includes('/admin/centers')) {
      this.currentTitle.set('Quản lý Trung tâm Đào tạo');
    } else if (url.includes('/admin/tutors')) {
      this.currentTitle.set('Quản lý Gia sư Tự do');
    } else if (url.includes('/admin/menus')) {
      this.currentTitle.set('Quản lý Menu Động & Phân quyền Menu');
    } else if (url.includes('/admin/users')) {
      this.currentTitle.set('Quản lý Người dùng Toàn Hệ Thống');
    } else if (url.includes('/admin/roles')) {
      this.currentTitle.set('Quản lý Phân quyền & Vai trò (RBAC)');
    } else if (url.includes('/admin/system-config')) {
      this.currentTitle.set('Cấu hình Tham số Vận hành Hệ thống');
    } else {
      this.currentTitle.set('Tổng quan Doanh thu & Nền tảng');
    }
  }
}
