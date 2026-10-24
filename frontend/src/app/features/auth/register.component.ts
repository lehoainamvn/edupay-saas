import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 16px;">
      <div style="background: white; border-radius: 16px; padding: 32px; width: 100%; max-width: 400px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;">
        <h2 style="font-size: 24px; font-weight: 700; color: #0f172a; text-align: center;">Đăng ký dùng thử</h2>
        <p style="color: #64748b; font-size: 14px; text-align: center; margin-top: 6px;">14 ngày miễn phí trọn vẹn tính năng</p>
        <div style="margin-top: 24px;">
          <input type="text" placeholder="Họ và tên giáo viên" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid #cbd5e1; margin-bottom: 12px; font-size: 15px;" />
          <input type="text" placeholder="Số điện thoại Zalo" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid #cbd5e1; margin-bottom: 12px; font-size: 15px;" />
          <input type="password" placeholder="Mật khẩu" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid #cbd5e1; margin-bottom: 16px; font-size: 15px;" />
          <button style="width: 100%; padding: 12px; background: #2563eb; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 16px;">Bắt đầu dùng ngay</button>
        </div>
        <p style="text-align: center; font-size: 14px; color: #64748b; margin-top: 16px;">
          Đã có tài khoản? <a routerLink="/auth/login" style="color: #2563eb; text-decoration: none; font-weight: 600;">Đăng nhập</a>
        </p>
      </div>
    </div>
  `
})
export class RegisterComponent {}
