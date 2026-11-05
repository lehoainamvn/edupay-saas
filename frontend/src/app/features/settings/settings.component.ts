import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { PaymentSettingService } from '../../core/services/payment-setting.service';
import { ToastService } from '../../core/services/toast.service';
import { POPULAR_BANKS, BankInfo, PaymentSettingRequest } from '../../core/models/payment-setting.models';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {
  private readonly paymentSettingService = inject(PaymentSettingService);
  private readonly toastService = inject(ToastService);

  readonly banks: BankInfo[] = POPULAR_BANKS;

  // ─── STATE SIGNALS ──────────────────────────────────────────────────────────
  readonly selectedBankBin = signal<string>('970422');
  readonly selectedBankName = signal<string>('MBBank');
  readonly accountNumber = signal<string>('');
  readonly accountName = signal<string>('');
  readonly transferPrefix = signal<string>('HP');

  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly isConfigured = signal<boolean>(false);

  // ─── COMPUTED SIGNALS ───────────────────────────────────────────────────────
  readonly selectedBank = computed(() => {
    const bin = this.selectedBankBin();
    return this.banks.find((b) => b.bin === bin) || null;
  });

  readonly isFormValid = computed(() => {
    const bin = this.selectedBankBin().trim();
    const accNum = this.accountNumber().trim();
    const accName = this.accountName().trim();
    return bin.length === 6 && accNum.length >= 4 && accName.length >= 2;
  });

  readonly liveQrUrl = computed(() => {
    const bin = this.selectedBankBin().trim();
    const accNum = this.accountNumber().trim().replace(/\s+/g, '');
    const name = encodeURIComponent(this.accountName().trim().toUpperCase());

    if (!bin || !accNum) {
      return null;
    }

    return `https://img.vietqr.io/image/${bin}-${accNum}-compact2.png?amount=50000&addInfo=TEST%20EDUPAY&accountName=${name}`;
  });

  // ─── LIFECYCLE ──────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadPaymentSetting();
  }

  // ─── LOAD DATA ──────────────────────────────────────────────────────────────
  loadPaymentSetting(): void {
    this.loading.set(true);
    this.paymentSettingService.getPaymentSetting().subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          const data = res.data;
          this.isConfigured.set(data.configured);
          if (data.configured) {
            this.selectedBankBin.set(data.bankBin || '970422');
            this.selectedBankName.set(data.bankName || 'MBBank');
            this.accountNumber.set(data.accountNumber || '');
            this.accountName.set(data.accountName || '');
            this.transferPrefix.set(data.transferPrefix || 'HP');
          }
        }
      },
      error: () => {
        this.loading.set(false);
        this.showToast('Không thể tải cấu hình tài khoản. Vui lòng thử lại sau.', 'error');
      }
    });
  }

  // ─── USER ACTIONS ───────────────────────────────────────────────────────────
  onBankChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const bin = select.value;
    const found = this.banks.find((b) => b.bin === bin);
    if (found) {
      this.selectedBankBin.set(found.bin);
      this.selectedBankName.set(found.shortName);
    }
  }

  onAccountNameInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    this.accountName.set(upper);
  }

  onAccountNumberInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const clean = input.value.replace(/[^\d]/g, '');
    this.accountNumber.set(clean);
  }

  onPrefixInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const clean = input.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
    this.transferPrefix.set(clean);
  }

  saveSetting(): void {
    if (!this.isFormValid() || this.saving()) return;

    this.saving.set(true);
    const payload: PaymentSettingRequest = {
      bankBin: this.selectedBankBin().trim(),
      bankName: this.selectedBankName().trim(),
      accountNumber: this.accountNumber().trim().replace(/\s+/g, ''),
      accountName: this.accountName().trim().toUpperCase(),
      transferPrefix: this.transferPrefix().trim().toUpperCase() || 'HP'
    };

    this.paymentSettingService.savePaymentSetting(payload).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.isConfigured.set(true);
        this.showToast('Lưu cấu hình tài khoản VietQR thành công!', 'success');
      },
      error: (err) => {
        this.saving.set(false);
        const msg = err.error?.message || 'Có lỗi xảy ra khi lưu cấu hình';
        this.showToast(msg, 'error');
      }
    });
  }

  // ─── HELPERS ────────────────────────────────────────────────────────────────
  showToast(message: string, type: 'success' | 'error' = 'success'): void {
    if (type === 'error') {
      this.toastService.error(message);
    } else {
      this.toastService.success(message);
    }
  }
}

