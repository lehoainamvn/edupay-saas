import { Component, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="nav">
      <div class="navInner">
        <!-- Logo / Brand -->
        <div class="brandWrap">
          <a [routerLink]="mode === 'public' ? '/' : '/dashboard'" class="logo" aria-label="EduPay">
            <span class="logoMark">
              <svg viewBox="0 0 28 28" aria-hidden="true">
                <path d="m3 10 11-6 11 6-11 6L3 10Z" />
                <path d="M7 13.5V19c3.8 3.2 10.2 3.2 14 0v-5.5" />
              </svg>
            </span>
            <strong>EduPay</strong>
          </a>
          <span class="version">SaaS v1.0</span>
        </div>

        <!-- Center Nav Links -->
        <div class="navLinks" [class.open]="mobileMenuOpen()">
          @if (mode === 'public') {
            <a routerLink="/" fragment="features" (click)="closeMobileMenu()">Tính năng</a>
            <a routerLink="/" fragment="process" (click)="closeMobileMenu()">Cách hoạt động</a>
            <a routerLink="/" fragment="pricing" (click)="closeMobileMenu()">Bảng giá</a>
            <a routerLink="/" fragment="parents" (click)="closeMobileMenu()">Cổng Phụ Huynh</a>
            <a routerLink="/" fragment="faq" (click)="closeMobileMenu()">Câu hỏi thường gặp</a>
          } @else {
            <a routerLink="/dashboard" routerLinkActive="active" (click)="closeMobileMenu()">Tổng quan</a>
            <a routerLink="/classes" routerLinkActive="active" (click)="closeMobileMenu()">Lớp học</a>
            <a routerLink="/students" routerLinkActive="active" (click)="closeMobileMenu()">Học sinh</a>
            <a routerLink="/attendance" routerLinkActive="active" (click)="closeMobileMenu()">Điểm danh 1 chạm</a>
            <a routerLink="/billing" routerLinkActive="active" (click)="closeMobileMenu()">Học phí VietQR</a>
          }
        </div>

        <!-- Right Actions / Profile -->
        <div class="navActions">
          @if (mode === 'public') {
            <a routerLink="/auth/login" class="login">Đăng nhập</a>
            <a routerLink="/auth/register" class="button small">
              Dùng thử miễn phí 14 ngày <span aria-hidden="true">→</span>
            </a>
          } @else {
            <div class="tenant">
              <i class="liveDot"></i>
              <div class="tenantInfo">
                <span class="tenantName">{{ userName() }}</span>
                <span class="tenantPlan">{{ planName() }}</span>
              </div>
              <button type="button" class="logoutBtn" (click)="logout($event)" title="Đăng xuất">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                  <polyline points="16 17 21 12 16 7"></polyline>
                  <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
                <span>Đăng xuất</span>
              </button>
            </div>
          }
        </div>

        <!-- Mobile hamburger toggle button -->
        <button class="menuButton" (click)="toggleMobileMenu()" aria-label="Toggle navigation">
          {{ mobileMenuOpen() ? '✕' : '☰' }}
        </button>
      </div>
    </nav>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }
    .nav {
      height: 72px;
      position: sticky;
      top: 0;
      z-index: 50;
      border-bottom: 1px solid rgba(226, 232, 240, 0.85);
      background: rgba(250, 250, 249, 0.94);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
    }
    .navInner {
      max-width: 1328px;
      height: 100%;
      margin: 0 auto;
      padding: 0 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
    }
    .brandWrap {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;
    }
    .logo {
      display: inline-flex;
      align-items: center;
      gap: 9px;
      font-size: 20px;
      font-weight: 800;
      letter-spacing: -0.04em;
      color: #0f172a;
      text-decoration: none;
      line-height: 1;
    }
    .logoMark {
      width: 32px;
      height: 32px;
      display: grid;
      place-items: center;
      background: #2563eb;
      border-radius: 9px;
      box-shadow: inset 0 -2px 0 rgba(0, 0, 0, 0.13);
      flex-shrink: 0;
    }
    .logoMark svg {
      width: 20px;
      height: 20px;
      fill: none;
      stroke: white;
      stroke-width: 2.2;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
    .version {
      color: #64748b;
      font-size: 11px;
      font-weight: 700;
      padding: 4px 8px;
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 999px;
      line-height: 1;
      display: inline-flex;
      align-items: center;
    }
    .navLinks {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    .navLinks a {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      height: 38px;
      padding: 0 14px;
      border-radius: 8px;
      color: #475569;
      font-size: 13.5px;
      font-weight: 600;
      text-decoration: none;
      line-height: 1;
      white-space: nowrap;
      transition: all 0.15s ease;
    }
    .navLinks a:hover {
      color: #2563eb;
      background: rgba(37, 99, 235, 0.06);
    }
    .navLinks a.active {
      color: #2563eb;
      background: #eff6ff;
      font-weight: 700;
    }
    .navActions {
      display: inline-flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;
    }
    .login {
      border: 0;
      background: transparent;
      color: #334155;
      font-size: 13.5px;
      font-weight: 600;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      height: 38px;
      padding: 0 14px;
      border-radius: 8px;
      line-height: 1;
      white-space: nowrap;
      transition: all 0.15s ease;
    }
    .login:hover {
      color: #2563eb;
      background: rgba(37, 99, 235, 0.06);
    }
    .button {
      border: 1px solid #2563eb;
      background: #2563eb;
      color: white;
      min-height: 52px;
      padding: 0 22px;
      border-radius: 999px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      font-weight: 700;
      font-size: 14px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
      transition: 0.2s ease;
      text-decoration: none;
      line-height: 1;
    }
    .button:hover {
      background: #1d4ed8;
      transform: translateY(-1px);
    }
    .button.small {
      min-height: 40px;
      height: 40px;
      padding: 0 18px;
      font-size: 13px;
      font-weight: 600;
    }
    .tenant {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      padding: 4px 6px 4px 14px;
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 999px;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
    }
    .liveDot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #10b981;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.18);
      flex-shrink: 0;
    }
    .tenantInfo {
      display: flex;
      flex-direction: column;
      line-height: 1.25;
    }
    .tenantName {
      font-size: 12.5px;
      font-weight: 700;
      color: #0f172a;
      white-space: nowrap;
    }
    .tenantPlan {
      font-size: 10px;
      font-weight: 600;
      color: #64748b;
      white-space: nowrap;
    }
    .logoutBtn {
      border: 0;
      background: #f1f5f9;
      color: #64748b;
      padding: 6px 10px;
      border-radius: 999px;
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 11px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s ease;
      margin-left: 4px;
    }
    .logoutBtn svg {
      width: 13px;
      height: 13px;
    }
    .logoutBtn:hover {
      background: #fee2e2;
      color: #dc2626;
    }
    .menuButton {
      display: none;
      background: transparent;
      border: 0;
      font-size: 20px;
      color: #0f172a;
      cursor: pointer;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: 8px;
    }
    @media (max-width: 900px) {
      .navInner { padding: 0 16px; }
      .version { display: none; }
      .navLinks {
        display: none;
      }
      .navLinks.open {
        position: absolute;
        display: flex;
        flex-direction: column;
        align-items: stretch;
        top: 72px;
        left: 0;
        right: 0;
        padding: 16px 20px;
        background: white;
        border-bottom: 1px solid #e2e8f0;
        box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);
        gap: 6px;
      }
      .navLinks.open a {
        width: 100%;
        justify-content: flex-start;
        height: 42px;
        padding: 0 14px;
        border-radius: 6px;
      }
      .menuButton {
        display: inline-flex;
      }
      .tenantName {
        max-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .logoutBtn span {
        display: none;
      }
    }
  `]
})
export class NavbarComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  @Input() mode: 'public' | 'app' = 'app';

  readonly mobileMenuOpen = signal<boolean>(false);

  readonly userName = () => {
    const user = this.authService.currentUser();
    return user?.fullName || 'Tài khoản giáo viên';
  };

  readonly planName = () => {
    const user = this.authService.currentUser();
    if (user?.subscriptionPlan) {
      return user.subscriptionPlan === 'MONTHLY' ? 'Gói Thuê Tháng' : user.subscriptionPlan;
    }
    return 'Gói Thuê Tháng';
  };

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  logout(event: Event): void {
    event.stopPropagation();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}

