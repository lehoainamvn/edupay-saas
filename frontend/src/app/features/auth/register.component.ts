import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

// Custom validator: mật khẩu phải có ít nhất 1 chữ hoa, 1 số
function strongPassword(ctrl: AbstractControl): ValidationErrors | null {
  const val: string = ctrl.value || '';
  if (!val) return null;
  const hasUpper = /[A-Z]/.test(val);
  const hasNumber = /[0-9]/.test(val);
  if (!hasUpper || !hasNumber) return { weakPassword: true };
  return null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth-page">
      <!-- Background -->
      <div class="auth-bg">
        <div class="blob blob-1"></div>
        <div class="blob blob-2"></div>
        <div class="blob blob-3"></div>
      </div>

      <div class="auth-card">
        <!-- Header -->
        <div class="auth-header">
          <div class="logo-mark">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="10" fill="url(#grad2)"/>
              <path d="M8 16L13 21L24 10" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              <defs>
                <linearGradient id="grad2" x1="0" y1="0" x2="32" y2="32">
                  <stop offset="0%" stop-color="#6366f1"/>
                  <stop offset="100%" stop-color="#8b5cf6"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 class="auth-title">Dùng thử miễn phí</h1>
          <p class="auth-subtitle">14 ngày trọn gói, không cần thẻ tín dụng</p>

          <!-- Feature badges -->
          <div class="feature-badges">
            <span class="badge">✓ Điểm danh thông minh</span>
            <span class="badge">✓ Học phí tự động</span>
            <span class="badge">✓ VietQR thanh toán</span>
          </div>
        </div>

        <!-- Form -->
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="auth-form" novalidate>

          @if (serverError()) {
            <div class="error-banner" role="alert">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <span>{{ serverError() }}</span>
            </div>
          }

          @if (successMsg()) {
            <div class="success-banner" role="status">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              <span>{{ successMsg() }}</span>
            </div>
          }

          <!-- Họ và tên -->
          <div class="field-group">
            <label class="field-label" for="fullName">Họ và tên giáo viên / quản lý</label>
            <div class="input-wrapper" [class.input-error]="isInvalid('fullName')" [class.input-focus]="focusedField() === 'fullName'">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input
                id="fullName"
                type="text"
                formControlName="fullName"
                class="field-input"
                placeholder="Nguyễn Thị Lan"
                autocomplete="name"
                (focus)="focusedField.set('fullName')"
                (blur)="focusedField.set(null)"
              />
            </div>
            @if (isInvalid('fullName')) {
              <p class="field-error">{{ getError('fullName') }}</p>
            }
          </div>

          <!-- Tên trung tâm / lớp học -->
          <div class="field-group">
            <label class="field-label" for="centerName">Tên trung tâm / lớp dạy kèm</label>
            <div class="input-wrapper" [class.input-error]="isInvalid('centerName')" [class.input-focus]="focusedField() === 'centerName'">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
              <input
                id="centerName"
                type="text"
                formControlName="centerName"
                class="field-input"
                placeholder="Trung tâm Toán Gia Sư Lan Anh"
                autocomplete="organization"
                (focus)="focusedField.set('centerName')"
                (blur)="focusedField.set(null)"
              />
            </div>
            @if (isInvalid('centerName')) {
              <p class="field-error">{{ getError('centerName') }}</p>
            }
          </div>

          <!-- Row: SĐT -->
          <div class="field-group">
            <label class="field-label" for="phone">Số điện thoại (dùng để đăng nhập)</label>
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
            <label class="field-label" for="password">Mật khẩu</label>
            <div class="input-wrapper" [class.input-error]="isInvalid('password')" [class.input-focus]="focusedField() === 'password'">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input
                id="password"
                [type]="showPassword() ? 'text' : 'password'"
                formControlName="password"
                class="field-input"
                placeholder="Tối thiểu 8 ký tự, có chữ HOA và số"
                autocomplete="new-password"
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
            <!-- Password strength indicator -->
            @if (form.get('password')?.value) {
              <div class="strength-bar">
                <div class="strength-fill" [style.width]="passwordStrength().pct + '%'" [style.background]="passwordStrength().color"></div>
              </div>
              <p class="strength-label" [style.color]="passwordStrength().color">{{ passwordStrength().label }}</p>
            }
            @if (isInvalid('password')) {
              <p class="field-error">{{ getError('password') }}</p>
            }
          </div>

          <!-- Submit -->
          <button
            type="submit"
            class="btn-primary"
            [disabled]="loading()"
            id="btn-register"
          >
            @if (loading()) {
              <span class="spinner"></span>
              <span>Đang tạo tài khoản...</span>
            } @else {
              <span>Bắt đầu dùng miễn phí</span>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            }
          </button>

          <p class="terms-note">
            Bằng cách đăng ký, bạn đồng ý với <a href="#" class="terms-link">Điều khoản dịch vụ</a> của EduPay.
          </p>
        </form>

        <!-- Footer -->
        <p class="auth-footer">
          Đã có tài khoản?
          <a routerLink="/auth/login" class="auth-link">Đăng nhập →</a>
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
      padding: 20px 16px;
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
      opacity: 0.22;
    }

    .blob-1 {
      width: 400px; height: 400px;
      background: radial-gradient(circle, #818cf8, #a78bfa);
      top: -150px; right: -100px;
      animation: float 8s ease-in-out infinite;
    }

    .blob-2 {
      width: 300px; height: 300px;
      background: radial-gradient(circle, #67e8f9, #818cf8);
      bottom: -100px; left: -80px;
      animation: float 10s ease-in-out infinite reverse;
    }

    .blob-3 {
      width: 200px; height: 200px;
      background: radial-gradient(circle, #f9a8d4, #c084fc);
      top: 40%; left: 10%;
      animation: float 12s ease-in-out infinite;
    }

    @keyframes float {
      0%, 100% { transform: translateY(0) scale(1); }
      50% { transform: translateY(-20px) scale(1.05); }
    }

    .auth-card {
      background: rgba(255, 255, 255, 0.88);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border-radius: 24px;
      padding: 36px;
      width: 100%;
      max-width: 440px;
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
      margin-bottom: 24px;
    }

    .logo-mark {
      display: inline-flex;
      margin-bottom: 12px;
      filter: drop-shadow(0 4px 12px rgba(99, 102, 241, 0.4));
    }

    .auth-title {
      font-size: 24px;
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
    }

    .feature-badges {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      justify-content: center;
      margin-top: 12px;
    }

    .badge {
      font-size: 11px;
      font-weight: 600;
      color: #6366f1;
      background: rgba(99, 102, 241, 0.08);
      border: 1px solid rgba(99, 102, 241, 0.2);
      border-radius: 100px;
      padding: 4px 10px;
    }

    .auth-form {
      display: flex;
      flex-direction: column;
      gap: 14px;
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

    .success-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-radius: 10px;
      padding: 12px 14px;
      font-size: 13px;
      color: #16a34a;
      font-weight: 500;
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
      height: 50px;
      padding: 0 14px;
      font-size: 15px;
      color: #111827;
      background: transparent;
      border: none;
      outline: none;
      min-height: unset;
      font-family: inherit;
    }

    .field-input::placeholder {
      color: #9ca3af;
      font-size: 13px;
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
      height: 50px;
      transition: color 0.2s;
    }

    .toggle-password:hover { color: #6366f1; }

    .strength-bar {
      height: 3px;
      background: #e5e7eb;
      border-radius: 100px;
      overflow: hidden;
    }

    .strength-fill {
      height: 100%;
      border-radius: 100px;
      transition: width 0.3s, background 0.3s;
    }

    .strength-label {
      font-size: 11px;
      font-weight: 600;
      transition: color 0.3s;
    }

    .field-error {
      font-size: 12px;
      color: #ef4444;
      font-weight: 500;
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
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
    }

    .btn-primary:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
    }

    .btn-primary:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 2.5px solid rgba(255,255,255,0.4);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    .terms-note {
      font-size: 12px;
      color: #9ca3af;
      text-align: center;
    }

    .terms-link {
      color: #6366f1;
      text-decoration: none;
    }

    .terms-link:hover { text-decoration: underline; }

    .auth-footer {
      text-align: center;
      font-size: 14px;
      color: #6b7280;
      margin-top: 20px;
    }

    .auth-link {
      color: #6366f1;
      font-weight: 600;
      text-decoration: none;
    }

    .auth-link:hover {
      color: #4f46e5;
      text-decoration: underline;
    }

    @media (max-width: 480px) {
      .auth-card {
        padding: 24px 18px;
      }
      .feature-badges {
        gap: 4px;
      }
    }
  `]
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  serverError = signal<string | null>(null);
  successMsg = signal<string | null>(null);
  showPassword = signal(false);
  focusedField = signal<string | null>(null);

  form = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    centerName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    phone: ['', [
      Validators.required,
      Validators.pattern(/^(0[3|5|7|8|9])+([0-9]{8})$/)
    ]],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      strongPassword
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
    if (field === 'fullName') {
      if (ctrl.errors['required']) return 'Vui lòng nhập họ và tên';
      if (ctrl.errors['minlength']) return 'Họ tên phải có ít nhất 2 ký tự';
    }
    if (field === 'centerName') {
      if (ctrl.errors['required']) return 'Vui lòng nhập tên trung tâm / lớp học';
      if (ctrl.errors['minlength']) return 'Tên trung tâm phải có ít nhất 3 ký tự';
    }
    if (field === 'phone') {
      if (ctrl.errors['required']) return 'Vui lòng nhập số điện thoại';
      if (ctrl.errors['pattern']) return 'Số điện thoại không hợp lệ (VD: 0912 345 678)';
    }
    if (field === 'password') {
      if (ctrl.errors['required']) return 'Vui lòng nhập mật khẩu';
      if (ctrl.errors['minlength']) return 'Mật khẩu phải có ít nhất 8 ký tự';
      if (ctrl.errors['weakPassword']) return 'Mật khẩu phải có ít nhất 1 chữ hoa và 1 chữ số';
    }
    return 'Giá trị không hợp lệ';
  }

  passwordStrength(): { pct: number; color: string; label: string } {
    const val: string = this.form.get('password')?.value || '';
    let score = 0;
    if (val.length >= 8) score++;
    if (/[A-Z]/.test(val)) score++;
    if (/[0-9]/.test(val)) score++;
    if (/[^A-Za-z0-9]/.test(val)) score++;
    const map = [
      { pct: 0,   color: '#e5e7eb', label: '' },
      { pct: 25,  color: '#f87171', label: 'Yếu' },
      { pct: 50,  color: '#fb923c', label: 'Trung bình' },
      { pct: 75,  color: '#facc15', label: 'Khá' },
      { pct: 100, color: '#4ade80', label: 'Mạnh' },
    ];
    return map[score];
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.serverError.set(null);
    this.successMsg.set(null);

    const { fullName, phone, password, centerName } = this.form.value;

    this.authService.register({
      fullName: fullName!,
      phone: phone!,
      password: password!,
      centerName: centerName!
    }).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        const status = err?.status;
        const detail = err?.error?.message || '';
        if (status === 409 || detail.toLowerCase().includes('duplicate') || detail.toLowerCase().includes('tồn tại')) {
          this.serverError.set('Số điện thoại này đã được đăng ký. Vui lòng đăng nhập.');
        } else if (status === 400) {
          this.serverError.set('Thông tin không hợp lệ. Vui lòng kiểm tra lại.');
        } else {
          this.serverError.set('Có lỗi xảy ra. Vui lòng thử lại sau ít phút.');
        }
      }
    });
  }
}
