import { Component, Input, OnInit, inject, signal, computed, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RbacService } from '../../../core/services/rbac.service';

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
            <a routerLink="/" fragment="features" (click)="closeAll()">Tính năng</a>
            <a routerLink="/" fragment="process" (click)="closeAll()">Cách hoạt động</a>
            <a routerLink="/" fragment="pricing" (click)="closeAll()">Bảng giá</a>
            <a routerLink="/" fragment="parents" (click)="closeAll()">Cổng Phụ Huynh</a>
            <a routerLink="/" fragment="faq" (click)="closeAll()">Câu hỏi thường gặp</a>
          } @else {
            <!-- 1. Tổng quan -->
            <a routerLink="/dashboard" routerLinkActive="active" (click)="closeAll()" class="navItemLink">
              Tổng quan
            </a>

            <!-- 2. Giảng dạy (Dropdown) -->
            <div class="navDropdown" (mouseenter)="setDropdown('teaching')" (mouseleave)="scheduleClose()">
              <button
                type="button"
                class="dropdownTrigger"
                [class.active]="isTeachingActive() || openDropdown() === 'teaching'"
                (click)="toggleDropdown('teaching', $event)"
              >
                <span>Giảng dạy</span>
                <svg class="chevron" [class.rotated]="openDropdown() === 'teaching'" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
                </svg>
              </button>

              <div class="dropdownMenu" [class.show]="openDropdown() === 'teaching'" (mouseenter)="cancelClose()" (mouseleave)="scheduleClose()">
                <a routerLink="/classes" routerLinkActive="active" (click)="closeAll()" class="dropdownItem">
                  <svg class="itemSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                  </svg>
                  <div class="itemText">
                    <strong class="itemTitle">Quản lý lớp học</strong>
                    <small class="itemDesc">Lịch học, sĩ số & biểu phí</small>
                  </div>
                </a>
                <a routerLink="/students" routerLinkActive="active" (click)="closeAll()" class="dropdownItem">
                  <svg class="itemSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="9" cy="7" r="4"></circle>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                  </svg>
                  <div class="itemText">
                    <strong class="itemTitle">Danh sách học sinh</strong>
                    <small class="itemDesc">Hồ sơ học sinh & liên hệ phụ huynh</small>
                  </div>
                </a>
                <a routerLink="/attendance" routerLinkActive="active" (click)="closeAll()" class="dropdownItem">
                  <svg class="itemSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 11l3 3L22 4"></path>
                    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
                  </svg>
                  <div class="itemText">
                    <strong class="itemTitle">Điểm danh 1 chạm</strong>
                    <small class="itemDesc">Điểm danh ca dạy trong 30s</small>
                  </div>
                </a>
              </div>
            </div>

            <!-- 2.5 Quản lý Trung tâm (Chỉ hiển thị cho Quản lý Trung tâm: Center Admin) -->
            @if (isCenterAdmin()) {
              <div class="navDropdown" (mouseenter)="setDropdown('center')" (mouseleave)="scheduleClose()">
                <button
                  type="button"
                  class="dropdownTrigger"
                  [class.active]="isCenterActive() || openDropdown() === 'center'"
                  (click)="toggleDropdown('center', $event)"
                >
                  <span>Quản lý Trung tâm</span>
                  <svg class="chevron" [class.rotated]="openDropdown() === 'center'" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
                  </svg>
                </button>

                <div class="dropdownMenu" [class.show]="openDropdown() === 'center'" (mouseenter)="cancelClose()" (mouseleave)="scheduleClose()">
                  <a routerLink="/center/teachers" routerLinkActive="active" (click)="closeAll()" class="dropdownItem">
                    <svg class="itemSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                      <circle cx="9" cy="7" r="4"></circle>
                      <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                      <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                    <div class="itemText">
                      <strong class="itemTitle">Đội ngũ giáo viên</strong>
                      <small class="itemDesc">Thêm, sửa & phân quyền nhân sự</small>
                    </div>
                  </a>
                  <a routerLink="/classes" routerLinkActive="active" (click)="closeAll()" class="dropdownItem">
                    <svg class="itemSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                    </svg>
                    <div class="itemText">
                      <strong class="itemTitle">Phân công giảng dạy</strong>
                      <small class="itemDesc">Gán giáo viên phụ trách các lớp</small>
                    </div>
                  </a>
                </div>
              </div>
            }

            <!-- 3. Tài chính VietQR (Dropdown - Chỉ hiển thị cho Quản lý / Gia sư tự do, ẩn với Giáo viên phụ) -->
            @if (canAccessFinance()) {
              <div class="navDropdown" (mouseenter)="setDropdown('finance')" (mouseleave)="scheduleClose()">
                <button
                  type="button"
                  class="dropdownTrigger"
                  [class.active]="isFinanceActive() || openDropdown() === 'finance'"
                  (click)="toggleDropdown('finance', $event)"
                >
                  <span>Học phí VietQR</span>
                  <svg class="chevron" [class.rotated]="openDropdown() === 'finance'" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
                  </svg>
                </button>

                <div class="dropdownMenu" [class.show]="openDropdown() === 'finance'" (mouseenter)="cancelClose()" (mouseleave)="scheduleClose()">
                  <a routerLink="/billing" routerLinkActive="active" (click)="closeAll()" class="dropdownItem">
                    <svg class="itemSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="2" y="5" width="20" height="14" rx="2"></rect>
                      <line x1="2" y1="10" x2="22" y2="10"></line>
                    </svg>
                    <div class="itemText">
                      <strong class="itemTitle">Học phí & Đối soát</strong>
                      <small class="itemDesc">Tạo phiếu thu & gạch nợ tự động</small>
                    </div>
                  </a>
                  <a routerLink="/settings" routerLinkActive="active" (click)="closeAll()" class="dropdownItem">
                    <svg class="itemSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="3" y="3" width="7" height="7"></rect>
                      <rect x="14" y="3" width="7" height="7"></rect>
                      <rect x="14" y="14" width="7" height="7"></rect>
                      <rect x="3" y="14" width="7" height="7"></rect>
                    </svg>
                    <div class="itemText">
                      <strong class="itemTitle">Cài đặt nhận tiền VietQR</strong>
                      <small class="itemDesc">Thiết lập tài khoản ngân hàng & QR</small>
                    </div>
                  </a>
                </div>
              </div>
            }

            <!-- 4. SuperAdmin (Chỉ hiển thị cho người dùng có quyền Quản trị tối cao) -->
            @if (isSuperAdmin()) {
              <a routerLink="/admin/platform" routerLinkActive="active" (click)="closeAll()" class="superAdminBtn">
                <svg class="adminSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
                <span>Cổng Quản trị SuperAdmin</span>
              </a>
            }
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
            <!-- Profile Dropdown Component -->
            <div class="userProfileWrap" (mouseenter)="setDropdown('profile')" (mouseleave)="scheduleClose()">
              <button
                type="button"
                class="userProfileTrigger"
                [class.active]="openDropdown() === 'profile'"
                (click)="toggleDropdown('profile', $event)"
                aria-label="Tài khoản giáo viên"
              >
                <div class="avatarCircle">
                  <span>{{ userInitials() }}</span>
                  <span class="onlineDot" title="Đang hoạt động"></span>
                </div>
                <div class="userMeta">
                  <span class="userFullName">{{ userName() }}</span>
                  <span class="userPlanBadge">{{ userRoleBadge() }}</span>
                </div>
                <svg class="chevron" [class.rotated]="openDropdown() === 'profile'" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd" />
                </svg>
              </button>

              <!-- Dropdown Card -->
              <div
                class="userDropdownMenu"
                [class.show]="openDropdown() === 'profile'"
                (mouseenter)="cancelClose()"
                (mouseleave)="scheduleClose()"
              >
                <!-- Card Header -->
                <div class="profileHeader">
                  <div class="largeAvatar">
                    <span>{{ userInitials() }}</span>
                  </div>
                  <div class="profileHeaderInfo">
                    <strong class="profileName">{{ userName() }}</strong>
                    <span class="profilePhone">{{ userPhone() || 'Chưa cập nhật SĐT' }}</span>
                    <span class="profilePlanChip">{{ userRoleTitle() }}</span>
                  </div>
                </div>

                <div class="menuDivider"></div>

                <!-- Action 1: Hồ sơ cá nhân (Mở trang riêng /profile) -->
                <a routerLink="/profile" (click)="closeAll()" class="userMenuItem">
                  <svg class="menuIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                  <div class="menuText">
                    <span class="menuTitle">Hồ sơ cá nhân</span>
                    <small class="menuDesc">Xem và cập nhật thông tin tài khoản</small>
                  </div>
                </a>

                @if (isSuperAdmin()) {
                  <a routerLink="/admin/platform" (click)="closeAll()" class="userMenuItem">
                    <svg class="menuIcon adminIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                    </svg>
                    <div class="menuText">
                      <span class="menuTitle">Quản trị nền tảng SaaS</span>
                      <small class="menuDesc">Cấu hình hệ thống & khách hàng</small>
                    </div>
                  </a>
                }

                <div class="menuDivider"></div>

                <!-- Action 3: Đăng xuất -->
                <button type="button" class="userMenuItem logoutItem" (click)="logout($event)">
                  <svg class="menuIcon logoutIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                    <polyline points="16 17 21 12 16 7"></polyline>
                    <line x1="21" y1="12" x2="9" y2="12"></line>
                  </svg>
                  <div class="menuText">
                    <span class="menuTitle logoutTitle">Đăng xuất</span>
                    <small class="menuDesc">Thoát phiên làm việc an toàn</small>
                  </div>
                </button>
              </div>
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
      font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
    .nav {
      height: 60px;
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
      font-size: 19px;
      font-weight: 800;
      letter-spacing: -0.03em;
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
      font-size: 11.5px;
      font-weight: 700;
      padding: 3px 8px;
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
    .navItemLink,
    .navLinks > a {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      height: 38px;
      padding: 0 14px;
      border-radius: 8px;
      color: #334155;
      font-size: 14px;
      font-weight: 600;
      text-decoration: none;
      line-height: 1;
      white-space: nowrap;
      transition: all 0.15s ease;
    }
    .navItemLink:hover,
    .navLinks > a:hover {
      color: #2563eb;
      background: rgba(37, 99, 235, 0.06);
    }
    .navItemLink.active,
    .navLinks > a.active {
      color: #2563eb;
      background: #eff6ff;
      font-weight: 700;
    }

    /* Dropdown Styles */
    .navDropdown {
      position: relative;
      display: inline-flex;
      align-items: center;
    }
    .dropdownTrigger {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 38px;
      padding: 0 14px;
      border: 0;
      background: transparent;
      border-radius: 8px;
      color: #334155;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      line-height: 1;
      white-space: nowrap;
      transition: all 0.15s ease;
      font-family: inherit;
    }
    .dropdownTrigger:hover {
      color: #2563eb;
      background: rgba(37, 99, 235, 0.06);
    }
    .dropdownTrigger.active {
      color: #2563eb;
      background: #eff6ff;
      font-weight: 700;
    }
    .chevron {
      width: 14px;
      height: 14px;
      transition: transform 0.2s ease;
      color: #64748b;
    }
    .dropdownTrigger.active .chevron,
    .dropdownTrigger .chevron.rotated,
    .navDropdown:hover .chevron,
    .userProfileTrigger:hover .chevron,
    .userProfileTrigger.active .chevron {
      color: #2563eb;
      transform: rotate(180deg);
    }
    .dropdownMenu {
      position: absolute;
      top: 100%;
      left: 0;
      min-width: 260px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 6px;
      box-shadow: 0 14px 34px -4px rgba(15, 23, 42, 0.14), 0 4px 12px -2px rgba(15, 23, 42, 0.05);
      display: none;
      flex-direction: column;
      gap: 2px;
      z-index: 1000;
      margin-top: 4px;
      animation: fadeInDown 0.15s cubic-bezier(0.16, 1, 0.3, 1);
    }
    /* Invisible hover bridge */
    .dropdownMenu::before,
    .userDropdownMenu::before {
      content: '';
      position: absolute;
      top: -12px;
      left: 0;
      right: 0;
      height: 12px;
      background: transparent;
    }
    .navDropdown:hover .dropdownMenu,
    .dropdownMenu.show,
    .userProfileWrap:hover .userDropdownMenu,
    .userDropdownMenu.show {
      display: flex;
    }
    @keyframes fadeInDown {
      from {
        opacity: 0;
        transform: translateY(-6px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    .dropdownItem {
      display: flex !important;
      align-items: flex-start !important;
      justify-content: flex-start !important;
      gap: 12px;
      padding: 10px 12px !important;
      height: auto !important;
      border-radius: 10px !important;
      color: #0f172a !important;
      text-decoration: none;
      transition: background 0.15s ease;
    }
    .dropdownItem:hover {
      background: #f8fafc !important;
    }
    .dropdownItem.active {
      background: #eff6ff !important;
    }
    .dropdownItem.active .itemTitle {
      color: #2563eb;
    }
    .itemSvg {
      width: 18px;
      height: 18px;
      color: #64748b;
      stroke-width: 1.8;
      flex-shrink: 0;
      margin-top: 1px;
      transition: color 0.15s ease;
    }
    .dropdownItem:hover .itemSvg,
    .dropdownItem.active .itemSvg {
      color: #2563eb;
    }
    .itemText {
      display: flex;
      flex-direction: column;
      gap: 2px;
      text-align: left;
    }
    .itemTitle {
      font-size: 14px;
      font-weight: 600;
      color: #0f172a;
      line-height: 1.3;
    }
    .itemDesc {
      font-size: 12px;
      color: #64748b;
      line-height: 1.35;
    }
    .superAdminBtn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 34px;
      padding: 0 12px;
      border-radius: 999px;
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #dc2626 !important;
      font-size: 12px !important;
      font-weight: 700 !important;
      text-decoration: none;
      transition: all 0.15s ease;
    }
    .superAdminBtn:hover {
      background: #fee2e2 !important;
      color: #b91c1c !important;
    }
    .adminSvg {
      width: 15px;
      height: 15px;
      color: #dc2626;
      stroke-width: 2;
    }

    /* Actions / Profile */
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

    /* ─── MODERN USER PROFILE PILL & DROPDOWN ─────────────────────────── */
    .userProfileWrap {
      position: relative;
      display: inline-flex;
      align-items: center;
    }
    .userProfileTrigger {
      display: inline-flex;
      align-items: center;
      gap: 9px;
      height: 42px;
      padding: 3px 12px 3px 4px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 999px;
      box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
      cursor: pointer;
      transition: all 0.15s ease;
      font-family: inherit;
    }
    .userProfileTrigger:hover,
    .userProfileTrigger.active {
      border-color: #cbd5e1;
      background: #f8fafc;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
    }
    .avatarCircle {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      color: #ffffff;
      font-size: 13px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      flex-shrink: 0;
      letter-spacing: -0.02em;
    }
    .onlineDot {
      position: absolute;
      bottom: -1px;
      right: -1px;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: #10b981;
      border: 2px solid #ffffff;
      box-shadow: 0 0 0 1px rgba(16, 185, 129, 0.2);
    }
    .userMeta {
      display: flex;
      align-items: center;
      gap: 7px;
    }
    .userFullName {
      font-size: 13.5px;
      font-weight: 600;
      color: #0f172a;
      max-width: 130px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      line-height: 1.2;
    }
    .userPlanBadge {
      font-size: 11px;
      font-weight: 700;
      padding: 2px 7px;
      border-radius: 6px;
      background: #eff6ff;
      color: #2563eb;
      border: 1px solid #dbeafe;
      line-height: 1.2;
      white-space: nowrap;
      text-transform: uppercase;
      letter-spacing: 0.02em;
    }
    .userDropdownMenu {
      position: absolute;
      top: calc(100% + 6px);
      right: 0;
      min-width: 280px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 16px;
      padding: 8px;
      box-shadow: 0 16px 36px -4px rgba(15, 23, 42, 0.14), 0 4px 12px -2px rgba(15, 23, 42, 0.05);
      display: none;
      flex-direction: column;
      gap: 2px;
      z-index: 1000;
      animation: fadeInDown 0.15s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .profileHeader {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 10px 12px;
    }
    .largeAvatar {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      color: #ffffff;
      font-size: 15px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: 0 3px 8px rgba(37, 99, 235, 0.25);
    }
    .profileHeaderInfo {
      display: flex;
      flex-direction: column;
      gap: 2px;
      overflow: hidden;
      text-align: left;
    }
    .profileName {
      font-size: 14.5px;
      font-weight: 700;
      color: #0f172a;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      line-height: 1.25;
    }
    .profilePhone {
      font-size: 12.5px;
      color: #64748b;
      font-weight: 500;
      line-height: 1.2;
    }
    .profilePlanChip {
      font-size: 11px;
      font-weight: 700;
      color: #166534;
      background: #dcfce7;
      padding: 2px 8px;
      border-radius: 999px;
      display: inline-flex;
      width: fit-content;
      margin-top: 3px;
      line-height: 1.2;
    }
    .menuDivider {
      height: 1px;
      background: #f1f5f9;
      margin: 4px 0;
    }
    .userMenuItem {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 9px 12px;
      border-radius: 10px;
      border: 0;
      background: transparent;
      width: 100%;
      text-align: left;
      cursor: pointer;
      color: #1e293b;
      text-decoration: none;
      transition: all 0.15s ease;
      font-family: inherit;
    }
    .userMenuItem:hover {
      background: #f8fafc;
      color: #2563eb;
    }
    .userMenuItem:hover .menuIcon {
      color: #2563eb;
    }
    .userMenuItem.logoutItem:hover {
      background: #fef2f2;
      color: #dc2626;
    }
    .userMenuItem.logoutItem:hover .logoutIcon,
    .userMenuItem.logoutItem:hover .logoutTitle {
      color: #dc2626;
    }
    .menuIcon {
      width: 18px;
      height: 18px;
      color: #64748b;
      stroke-width: 1.8;
      flex-shrink: 0;
      transition: color 0.15s ease;
    }
    .adminIcon {
      color: #dc2626;
    }
    .logoutIcon {
      color: #ef4444;
    }
    .menuText {
      display: flex;
      flex-direction: column;
      gap: 1px;
      text-align: left;
    }
    .menuTitle {
      font-size: 13.5px;
      font-weight: 600;
      color: #0f172a;
      line-height: 1.3;
    }
    .menuDesc {
      font-size: 11.5px;
      color: #64748b;
      line-height: 1.25;
    }
    .logoutTitle {
      color: #dc2626;
    }

    /* Mobile Responsive */
    .menuButton {
      display: none;
      border: 0;
      background: transparent;
      font-size: 22px;
      color: #0f172a;
      cursor: pointer;
      padding: 6px;
    }
    @media (max-width: 900px) {
      .menuButton {
        display: block;
      }
      .navLinks {
        position: fixed;
        top: 60px;
        left: 0;
        right: 0;
        background: white;
        border-bottom: 1px solid #e2e8f0;
        flex-direction: column;
        padding: 16px;
        gap: 6px;
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
        display: none;
        max-height: calc(100vh - 60px);
        overflow-y: auto;
      }
      .navLinks.open {
        display: flex;
      }
      .navItemLink,
      .navLinks > a,
      .dropdownTrigger {
        width: 100%;
        justify-content: space-between;
        height: 44px;
        padding: 0 16px;
        border-radius: 8px;
      }
      .navDropdown {
        width: 100%;
        flex-direction: column;
        align-items: stretch;
      }
      .dropdownMenu {
        position: static;
        box-shadow: none;
        border: 1px solid #f1f5f9;
        background: #f8fafc;
        margin: 4px 0 8px 12px;
        padding: 4px;
      }
      .userFullName {
        max-width: 90px;
      }
    }
  `]
})
export class NavbarComponent implements OnInit {
  private authService = inject(AuthService);
  private rbacService = inject(RbacService);
  private router = inject(Router);

  @Input() mode: 'public' | 'app' = 'app';

  readonly mobileMenuOpen = signal<boolean>(false);
  readonly openDropdown = signal<string | null>(null);
  private closeTimer: any = null;
  readonly navMenus = this.rbacService.menus;

  ngOnInit(): void {
    if (this.mode === 'app' && !this.rbacService.isLoaded()) {
      this.rbacService.loadNavigation().subscribe({
        error: () => {}
      });
    }
  }

  readonly userName = () => {
    const user = this.authService.currentUser();
    return user?.fullName || 'Tài khoản giáo viên';
  };

  readonly userPhone = () => {
    return this.authService.currentUser()?.phone || '';
  };

  readonly userInitials = computed(() => {
    const name = this.userName().trim();
    if (!name || name === 'Tài khoản giáo viên') return 'GV';
    const parts = name.split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  });

  readonly planName = () => {
    const user = this.authService.currentUser();
    if (user?.subscriptionPlan) {
      return user.subscriptionPlan === 'MONTHLY' ? 'Gói Thuê Tháng' : user.subscriptionPlan;
    }
    return 'Gói Thuê Tháng';
  };

  readonly planBadge = () => {
    const plan = this.authService.currentUser()?.subscriptionPlan;
    if (!plan || plan === 'TRIAL') return 'DÙNG THỬ';
    if (plan === 'MONTHLY') return 'GÓI THÁNG';
    return plan;
  };

  readonly isSuperAdmin = computed(() => {
    const user = this.authService.currentUser();
    if (!user?.roles) return false;
    return user.roles.some(r => {
      const up = r.toUpperCase();
      return up.includes('ADMIN') || up.includes('SUPER_ADMIN');
    });
  });

  readonly isCenterAdmin = computed(() => {
    const user = this.authService.currentUser();
    const type = user?.tenantType || this.rbacService.tenantType();
    const isOwner = Boolean(user?.isOwner ?? this.rbacService.isOwner());
    return isOwner && type === 'CENTER';
  });

  readonly isCenterTeacher = computed(() => {
    const user = this.authService.currentUser();
    const type = user?.tenantType || this.rbacService.tenantType();
    const isOwner = Boolean(user?.isOwner ?? this.rbacService.isOwner());
    return !isOwner && type === 'CENTER';
  });

  readonly canAccessFinance = computed(() => {
    return !this.isCenterTeacher();
  });

  readonly userRoleBadge = computed(() => {
    if (this.isSuperAdmin()) return 'SUPER_ADMIN';
    if (this.isCenterAdmin()) return 'QUẢN LÝ TRUNG TÂM';
    if (this.isCenterTeacher()) return 'GIÁO VIÊN TRUNG TÂM';
    return 'GIA SƯ TỰ DO';
  });

  readonly userRoleTitle = computed(() => {
    if (this.isSuperAdmin()) return 'Quản Trị Viên Tối Cao';
    if (this.isCenterAdmin()) return 'Quản Lý Trung Tâm (Center Admin)';
    if (this.isCenterTeacher()) return 'Giáo Viên Trực Thuộc Trung Tâm';
    return 'Gia Sư Dạy Kèm Tự Do';
  });

  isCenterActive(): boolean {
    return this.router.url.startsWith('/center');
  }

  isTeachingActive(): boolean {
    const url = this.router.url;
    return url.startsWith('/classes') || url.startsWith('/students') || url.startsWith('/attendance');
  }

  isFinanceActive(): boolean {
    const url = this.router.url;
    return url.startsWith('/billing') || url.startsWith('/settings');
  }

  setDropdown(name: string): void {
    if (this.closeTimer) {
      clearTimeout(this.closeTimer);
      this.closeTimer = null;
    }
    this.openDropdown.set(name);
  }

  scheduleClose(): void {
    if (this.closeTimer) {
      clearTimeout(this.closeTimer);
    }
    this.closeTimer = setTimeout(() => {
      this.openDropdown.set(null);
      this.closeTimer = null;
    }, 300);
  }

  cancelClose(): void {
    if (this.closeTimer) {
      clearTimeout(this.closeTimer);
      this.closeTimer = null;
    }
  }

  toggleDropdown(name: string, event: MouseEvent): void {
    event.stopPropagation();
    this.cancelClose();
    this.openDropdown.update(cur => (cur === name ? null : name));
  }

  @HostListener('document:click')
  onDocClick(): void {
    this.openDropdown.set(null);
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeAll(): void {
    this.cancelClose();
    this.openDropdown.set(null);
    this.mobileMenuOpen.set(false);
  }

  logout(event: Event): void {
    event.stopPropagation();
    this.closeAll();
    this.rbacService.clear();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
