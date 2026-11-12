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
  styleUrls: ['./admin-layout.component.css']
})
export class AdminLayoutComponent {
  private readonly tokenService = inject(TokenService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  isSidebarCollapsed = signal<boolean>(false);
  isMobileSidebarOpen = signal<boolean>(false);
  currentTitle = signal<string>('Tổng quan Nền tảng');

  userInfo = this.tokenService.getUserInfo();

  readonly navItems = [
    {
      label: 'Tổng quan Nền tảng',
      route: '/admin/platform',
      icon: 'M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z'
    },
    {
      label: 'Quản lý Trung tâm',
      route: '/admin/centers',
      icon: 'M2.25 21h19.5m-18-18v18m10.5-18v18m6-13.5V21M6.75 6.75h.75m-.75 3h.75m-.75 3h.75m3-6h.75m-.75 3h.75m-.75 3h.75M6.75 21v-3.375c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21M3 3h12m-.75 4.5H21m-3.75 3.75h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008zm0 3h.008v.008h-.008v-.008z'
    },
    {
      label: 'Quản lý Gia sư',
      route: '/admin/tutors',
      icon: 'M4.26 10.147a60.438 60.438 0 0 0-.491 6.347A48.62 48.62 0 0 1 12 20.904a48.62 48.62 0 0 1 8.232-4.41 60.46 60.46 0 0 0-.491-6.347m-15.482 0a50.636 50.636 0 0 0-2.658-.813A59.906 59.906 0 0 1 12 3.493a59.903 59.903 0 0 1 10.399 5.84c-.896.248-1.783.52-2.658.814m-15.482 0A50.717 50.717 0 0 1 12 13.489a50.702 50.702 0 0 1 7.74-3.342M6.75 15a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5zm0 0v-3.675A55.378 55.378 0 0 1 12 8.443m-5.25 6.557c0 1.637.355 3.19 1 4.582M12 8.443c1.99.39 3.89 1.02 5.674 1.868M12 8.443V6.75'
    },
    {
      label: 'Quản lý Menu Động',
      route: '/admin/menus',
      icon: 'M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5'
    },
    {
      label: 'Phân quyền RBAC',
      route: '/admin/roles',
      icon: 'M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z'
    },
    {
      label: 'Tất cả Tài khoản',
      route: '/admin/users',
      icon: 'M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z'
    },
    {
      label: 'Cấu hình Hệ thống',
      route: '/admin/system-config',
      icon: 'M10.5 6h9.75M10.5 6a1.5 1.5 0 11-3 0m3 0a1.5 1.5 0 10-3 0M3.75 6H7.5m3 12h9.75m-9.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-3.75 0H7.5m9-6h3.75m-3.75 0a1.5 1.5 0 01-3 0m3 0a1.5 1.5 0 00-3 0m-9.75 0h9.75'
    }
  ];

  constructor() {
    this.updateCurrentTitle(this.router.url);
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.updateCurrentTitle(event.urlAfterRedirects || event.url);
      this.isMobileSidebarOpen.set(false);
    });
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed.update(v => !v);
  }

  toggleMobileSidebar(): void {
    this.isMobileSidebarOpen.update(v => !v);
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
      this.currentTitle.set('Cấu hình Tham số Hệ thống');
    } else {
      this.currentTitle.set('Tổng quan Doanh thu & Nền tảng Platform');
    }
  }
}
