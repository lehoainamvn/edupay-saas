import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth-page">
      <!-- Background decoration -->
      <div class="auth-bg">
        <div class="blob blob-1"></div>
        <div class="blob blob-2"></div>
      </div>

      <div class="auth-card">
        <!-- Logo + Header -->
        <div class="auth-header">
          <div class="logo-mark">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="10" fill="url(#grad)"/>
              <path d="M8 16L13 21L24 10" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <defs>
                <linearGradient id="grad" x1="0" y1="0" x2="32" y2="32">
                  <stop offset="0%" stop-color="#6366f1"/>
                  <stop offset="100%" stop-color="#8b5cf6"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 class="auth-title">EduPay</h1>
          <p class="auth-subtitle">Đăng nhập vào tài khoản của bạn</p>
        </div>

        <!-- Form -->
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="auth-form" novalidate>

          <!-- Thông báo lỗi server -->
          @if (serverError()) {
            <div class="error-banner" role="alert">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <span>{{ serverError() }}</span>
            </div>
          }

          <!-- Số điện thoại -->
          <div class="field-group">
            <label class="field-label" for="phone">Số điện thoại</label>
            <div class="input-wrapper" [class.input-error]="isInvalid('phone')" [class.input-focus]="focusedField() === 'phone'">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.61 3.44 2 2 0 0 1 3.6 1.27h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L7.91 9a16 16 0 0 0 7.1 7.1l.91-.91a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
              <input
                id="phone"
                type="tel"
                formControlName="phone"
                class="field-input"
                placeholder="0912 345 678"
                inputmode="numeric"
                autocomplete="tel"
                (focus)="focusedField.set('phone')"
                (blur)="focusedField.set(null)"
              />
            </div>
            @if (isInvalid('phone')) {
              <p class="field-error">{{ getError('phone') }}</p>
            }
          </div>

          <!-- Mật khẩu -->
          <div class="field-group">
            <div class="label-row">
              <label class="field-label" for="password">Mật khẩu</label>
            </div>
            <div class="input-wrapper" [class.input-error]="isInvalid('password')" [class.input-focus]="focusedField() === 'password'">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input
                id="password"
                [type]="showPassword() ? 'text' : 'password'"
                formControlName="password"
                class="field-input"
                placeholder="Nhập mật khẩu"
                autocomplete="current-password"
                (focus)="focusedField.set('password')"
                (blur)="focusedField.set(null)"
              />
              <button type="button" class="toggle-password" (click)="togglePassword()" [attr.aria-label]="showPassword() ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'">
                @if (showPassword()) {
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                } @else {
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                }
              </button>
            </div>
            @if (isInvalid('password')) {
              <p class="field-error">{{ getError('password') }}</p>
            }
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            class="btn-primary"
            [disabled]="loading()"
            id="btn-login"
          >
            @if (loading()) {
              <span class="spinner"></span>
              <span>Đang đăng nhập...</span>
            } @else {
              <span>Đăng nhập</span>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            }
          </button>
        </form>

        <!-- Footer -->
        <p class="auth-footer">
          Chưa có tài khoản?
          <a routerLink="/auth/register" class="auth-link">Đăng ký 14 ngày miễn phí →</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      background: linear-gradient(135deg, #f0f0ff 0%, #faf5ff 50%, #f0f9ff 100%);
      position: relative;
      overflow: hidden;
    }

    .auth-bg {
      position: absolute;
      inset: 0;
      pointer-events: none;
    }

    .blob {
      position: absolute;
      border-radius: 50%;
      filter: blur(80px);
      opacity: 0.25;
    }

    .blob-1 {
      width: 400px;
      height: 400px;
      background: radial-gradient(circle, #818cf8, #a78bfa);
      top: -100px;
      right: -100px;
      animation: float 8s ease-in-out infinite;
    }

    .blob-2 {
      width: 300px;
      height: 300px;
      background: radial-gradient(circle, #67e8f9, #818cf8);
      bottom: -80px;
      left: -80px;
      animation: float 10s ease-in-out infinite reverse;
    }

    @keyframes float {
      0%, 100% { transform: translateY(0px) scale(1); }
      50% { transform: translateY(-20px) scale(1.05); }
    }

    .auth-card {
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border-radius: 24px;
      padding: 40px 36px;
      width: 100%;
      max-width: 420px;
      box-shadow: 0 8px 32px rgba(99, 102, 241, 0.12), 0 2px 8px rgba(0,0,0,0.06);
      border: 1px solid rgba(255, 255, 255, 0.6);
      position: relative;
      z-index: 1;
      animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes slideUp {
      from { opacity: 0; transform: translateY(24px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .auth-header {
      text-align: center;
      margin-bottom: 28px;
    }

    .logo-mark {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;
      filter: drop-shadow(0 4px 12px rgba(99, 102, 241, 0.4));
    }

    .auth-title {
      font-size: 26px;
      font-weight: 800;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      letter-spacing: -0.5px;
      margin-bottom: 4px;
    }

    .auth-subtitle {
      font-size: 14px;
      color: #6b7280;
      font-weight: 400;
    }

    .auth-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .error-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 10px;
      padding: 12px 14px;
      font-size: 13px;
      color: #dc2626;
      font-weight: 500;
      animation: shake 0.4s ease;
    }

    @keyframes shake {
      0%, 100% { transform: translateX(0); }
      25% { transform: translateX(-4px); }
      75% { transform: translateX(4px); }
    }

    .field-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .label-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .field-label {
      font-size: 13px;
      font-weight: 600;
      color: #374151;
    }

    .input-wrapper {
      position: relative;
      display: flex;
      align-items: center;
      border: 1.5px solid #e5e7eb;
      border-radius: 12px;
      background: #ffffff;
      transition: border-color 0.2s, box-shadow 0.2s;
    }

    .input-wrapper.input-focus {
      border-color: #6366f1;
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
    }

    .input-wrapper.input-error {
      border-color: #f87171;
      box-shadow: 0 0 0 3px rgba(248, 113, 113, 0.12);
    }

    .input-icon {
      color: #9ca3af;
      flex-shrink: 0;
      margin-left: 14px;
      transition: color 0.2s;
    }

    .input-focus .input-icon {
      color: #6366f1;
    }

    .field-input {
      flex: 1;
      height: 52px;
      padding: 0 14px;
      font-size: 15px;
      color: #111827;
      background: transparent;
      border: none;
      outline: none;
      min-height: unset; /* override global 44px min-height */
      font-family: inherit;
    }

    .field-input::placeholder {
      color: #9ca3af;
      font-size: 14px;
    }

    .toggle-password {
      background: none;
      border: none;
      cursor: pointer;
      padding: 0 14px;
      color: #9ca3af;
      display: flex;
      align-items: center;
      min-height: unset;
      height: 52px;
      transition: color 0.2s;
    }

    .toggle-password:hover {
      color: #6366f1;
    }

    .field-error {
      font-size: 12px;
      color: #ef4444;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 4px;
      padding-left: 2px;
      animation: fadeIn 0.2s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .btn-primary {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      width: 100%;
      height: 52px;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      color: white;
      border: none;
      border-radius: 12px;
      font-size: 15px;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.2s;
      margin-top: 4px;
      font-family: inherit;
      letter-spacing: 0.2px;
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
    }

    .btn-primary:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
    }

    .btn-primary:active:not(:disabled) {
      transform: translateY(0);
    }

    .btn-primary:disabled {
      opacity: 0.7;
      cursor: not-allowed;
      transform: none;
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 2.5px solid rgba(255,255,255,0.4);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .auth-footer {
      text-align: center;
      font-size: 14px;
      color: #6b7280;
      margin-top: 24px;
    }

    .auth-link {
      color: #6366f1;
      font-weight: 600;
      text-decoration: none;
      transition: color 0.2s;
    }

    .auth-link:hover {
      color: #4f46e5;
      text-decoration: underline;
    }

    @media (max-width: 480px) {
      .auth-card {
        padding: 28px 20px;
        border-radius: 20px;
      }
    }
  `]
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // UI State
  loading = signal(false);
  serverError = signal<string | null>(null);
  showPassword = signal(false);
  focusedField = signal<string | null>(null);

  form = this.fb.group({
    phone: ['', [
      Validators.required,
      Validators.pattern(/^(0[3|5|7|8|9])+([0-9]{8})$/)
    ]],
    password: ['', [
      Validators.required,
      Validators.minLength(6)
    ]]
  });

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl && ctrl.invalid && (ctrl.dirty || ctrl.touched));
  }

  getError(field: string): string {
    const ctrl = this.form.get(field);
    if (!ctrl || !ctrl.errors) return '';
    if (field === 'phone') {
      if (ctrl.errors['required']) return 'Vui lòng nhập số điện thoại';
      if (ctrl.errors['pattern']) return 'Số điện thoại không hợp lệ (VD: 0912 345 678)';
    }
    if (field === 'password') {
      if (ctrl.errors['required']) return 'Vui lòng nhập mật khẩu';
      if (ctrl.errors['minlength']) return 'Mật khẩu phải có ít nhất 6 ký tự';
    }
    return 'Giá trị không hợp lệ';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.serverError.set(null);

    const { phone, password } = this.form.value;

    this.authService.login({ phone: phone!, password: password! }).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.loading.set(false);
        const status = err?.status;
        if (status === 401) {
          this.serverError.set('Số điện thoại hoặc mật khẩu không chính xác');
        } else if (status === 400) {
          this.serverError.set('Thông tin đăng nhập không hợp lệ');
        } else {
          this.serverError.set('Có lỗi xảy ra. Vui lòng thử lại sau');
        }
      }
    });
  }
}
