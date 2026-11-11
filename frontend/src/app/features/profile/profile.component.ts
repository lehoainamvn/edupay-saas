import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly toastService = inject(ToastService);

  // ─── FORM SIGNALS ─────────────────────────────────────────────────────────
  readonly fullName = signal<string>('');
  readonly phone = signal<string>('');
  readonly email = signal<string>('');
  readonly saving = signal<boolean>(false);
  readonly loading = signal<boolean>(false);

  // ─── COMPUTED DATA ────────────────────────────────────────────────────────
  readonly userInitials = computed(() => {
    const name = this.fullName().trim() || this.authService.currentUser()?.fullName || 'GV';
    const parts = name.split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  });

  readonly planLabel = computed(() => {
    const plan = (this.authService.currentUser()?.subscriptionPlan || 'TRIAL').toUpperCase();
    if (plan === 'PRO') return 'EduPay PRO';
    if (plan === 'ENTERPRISE') return 'EduPay Doanh Nghiệp';
    return 'Dùng thử 14 ngày (TRIAL)';
  });

  readonly isFormValid = computed(() => {
    return this.fullName().trim().length >= 2 && this.phone().trim().length >= 8;
  });

  ngOnInit(): void {
    this.loadUserData();
  }

  loadUserData(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.fullName.set(user.fullName || '');
      this.phone.set(user.phone || '');
      this.email.set(user.email || '');
    }

    this.loading.set(true);
    this.userService.getProfile().subscribe({
      next: (profile) => {
        this.loading.set(false);
        if (profile) {
          this.fullName.set(profile.fullName || '');
          this.phone.set(profile.phone || '');
          this.email.set(profile.email || '');
        }
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  saveProfile(): void {
    if (!this.isFormValid() || this.saving()) return;

    this.saving.set(true);
    this.userService.updateProfile({
      fullName: this.fullName().trim(),
      phone: this.phone().trim(),
      email: this.email().trim() || undefined
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success('Đã cập nhật hồ sơ giáo viên thành công!', 'Thành công');
      },
      error: (err) => {
        this.saving.set(false);
        const msg = err?.error?.message || 'Có lỗi xảy ra khi lưu thông tin hồ sơ';
        this.toastService.error(msg);
      }
    });
  }
}
