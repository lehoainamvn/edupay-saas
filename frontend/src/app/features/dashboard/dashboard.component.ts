import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div style="padding: 24px; max-width: 1200px; margin: 0 auto;">
      <header style="margin-bottom: 32px;">
        <h1 style="font-size: 28px; font-weight: 700; color: #0f172a;">🎓 EduPay SaaS Dashboard</h1>
        <p style="color: #64748b; margin-top: 8px;">Quản lý học sinh, điểm danh & đối soát học phí VietQR tự động</p>
      </header>

      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px;">
        <div style="background: white; border-radius: 12px; padding: 20px; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
          <h3 style="font-size: 16px; color: #64748b; font-weight: 500;">Dự thu tháng này</h3>
          <p style="font-size: 28px; font-weight: 700; color: #2563eb; margin: 8px 0;">24.500.000 đ</p>
          <span style="font-size: 13px; color: #10b981; font-weight: 500;">● 18 học sinh</span>
        </div>

        <div style="background: white; border-radius: 12px; padding: 20px; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
          <h3 style="font-size: 16px; color: #64748b; font-weight: 500;">Đã thu (VietQR & Tiền mặt)</h3>
          <p style="font-size: 28px; font-weight: 700; color: #10b981; margin: 8px 0;">18.200.000 đ</p>
          <span style="font-size: 13px; color: #10b981; font-weight: 500;">● 74% tiến độ</span>
        </div>

        <div style="background: white; border-radius: 12px; padding: 20px; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
          <h3 style="font-size: 16px; color: #64748b; font-weight: 500;">Chưa nộp</h3>
          <p style="font-size: 28px; font-weight: 700; color: #f59e0b; margin: 8px 0;">6.300.000 đ</p>
          <span style="font-size: 13px; color: #f59e0b; font-weight: 500;">● 5 học sinh chờ đóng</span>
        </div>
      </div>

      <div style="margin-top: 32px; display: flex; gap: 12px; flex-wrap: wrap;">
        <a routerLink="/attendance" style="display: inline-block; padding: 12px 24px; background: #2563eb; color: white; border-radius: 8px; text-decoration: none; font-weight: 600;">📱 Điểm danh hôm nay</a>
        <a routerLink="/billing" style="display: inline-block; padding: 12px 24px; background: #0f172a; color: white; border-radius: 8px; text-decoration: none; font-weight: 600;">⚡ Quản lý học phí & VietQR</a>
        <a routerLink="/classes" style="display: inline-block; padding: 12px 24px; background: white; color: #0f172a; border: 1px solid #cbd5e1; border-radius: 8px; text-decoration: none; font-weight: 600;">📚 Danh sách lớp học</a>
      </div>
    </div>
  `
})
export class DashboardComponent {}
