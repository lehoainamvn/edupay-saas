import { Component, inject, signal, computed } from '@angular/core';
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

  readonly isSuperAdmin = computed(() => this.authService.isSuperAdmin());
  readonly isCenterAdmin = computed(() => this.authService.isCenterAdmin());

  readonly brandKicker = computed(() =>
    this.isSuperAdmin() ? 'PLATFORM CONSOLE' : 'CENTER CONSOLE'
  );

  readonly portalTitle = computed(() =>
    this.isSuperAdmin() ? 'Cổng Quản trị SuperAdmin' : 'Cổng Quản trị Trung tâm'
  );

  readonly liveBadge = computed(() =>
    this.isSuperAdmin() ? 'Platform Live' : 'Trung tâm Live'
  );

  readonly roleLabel = computed(() =>
    this.isSuperAdmin() ? 'SUPER_ADMIN' : 'QUẢN LÝ TRUNG TÂM'
  );

  readonly switchBtnText = computed(() =>
    this.isSuperAdmin() ? 'Trang Dạy học' : 'Bàn làm việc Lớp học'
  );

  readonly navGroups = computed(() => {
    if (this.isCenterAdmin() && !this.isSuperAdmin()) {
      return [
        {
          label: 'QUẢN TRỊ TRUNG TÂM',
          items: [
            {
              icon: 'grid',
              label: 'Tổng quan Cơ sở',
              route: '/admin/platform',
              exact: true,
            },
            {
              icon: 'users',
              label: 'Đội ngũ Giáo viên',
              route: '/admin/users',
              exact: false,
            },
            {
              icon: 'building',
              label: 'Quản lý Lớp học',
              route: '/admin/classes',
              exact: false,
            },
            {
              icon: 'user',
              label: 'Học sinh & Điểm danh',
              route: '/admin/students',
              exact: false,
            },
            {
              icon: 'menu',
              label: 'Học phí & VietQR',
              route: '/admin/billing',
              exact: false,
            },
          ],
        },
        {
          label: 'CẤU HÌNH & CHÍNH SÁCH',
          items: [
            {
              icon: 'shield',
              label: 'Phân quyền Giáo viên',
              route: '/admin/teacher-policies',
              exact: false,
            },
            {
              icon: 'settings',
              label: 'Cài đặt VietQR Trung tâm',
              route: '/admin/settings',
              exact: false,
            },
          ],
        },
      ];
    }

    return [
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
  });

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
    const isCenter = this.isCenterAdmin() && !this.isSuperAdmin();

    if (url.includes('/admin/centers')) {
      this.currentTitle.set('Quản lý Trung tâm Đào tạo');
    } else if (url.includes('/admin/tutors')) {
      this.currentTitle.set('Quản lý Gia sư Tự do');
    } else if (url.includes('/admin/menus')) {
      this.currentTitle.set('Quản lý Menu Động & Phân quyền Menu');
    } else if (url.includes('/admin/users')) {
      this.currentTitle.set(isCenter ? 'Đội ngũ Giáo viên Cơ sở' : 'Quản lý Người dùng Toàn Hệ Thống');
    } else if (url.includes('/admin/roles')) {
      this.currentTitle.set('Quản lý Phân quyền & Vai trò (RBAC)');
    } else if (url.includes('/admin/system-config')) {
      this.currentTitle.set('Cấu hình Tham số Vận hành Hệ thống');
    } else if (url.includes('/admin/classes')) {
      this.currentTitle.set('Quản lý Lớp học Cơ sở');
    } else if (url.includes('/admin/students')) {
      this.currentTitle.set('Học sinh & Điểm danh Cơ sở');
    } else if (url.includes('/admin/billing')) {
      this.currentTitle.set('Học phí & VietQR Cơ sở');
    } else if (url.includes('/admin/teacher-policies')) {
      this.currentTitle.set('Phân quyền Giáo viên Cơ sở');
    } else if (url.includes('/admin/settings')) {
      this.currentTitle.set('Cài đặt VietQR Trung tâm');
    } else {
      this.currentTitle.set(isCenter ? 'Tổng quan Cơ sở Trung tâm' : 'Tổng quan Doanh thu & Nền tảng');
    }
  }
}
