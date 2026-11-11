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

  // ─── STATE SIGNALS ──────────────────────────────────────────────────────────
  readonly banks = signal<BankInfo[]>(POPULAR_BANKS);
  readonly loadingBanks = signal<boolean>(false);
  readonly bankSearchQuery = signal<string>('');

  readonly selectedBankBin = signal<string>('970422');
  readonly selectedBankName = signal<string>('MBBank');
  readonly accountNumber = signal<string>('');
  readonly accountName = signal<string>('');
  readonly transferPrefix = signal<string>('HP');

  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly isConfigured = signal<boolean>(false);

  // ─── COMPUTED SIGNALS ───────────────────────────────────────────────────────
  readonly filteredBanks = computed(() => {
    const query = this.bankSearchQuery().trim().toLowerCase();
    const list = this.banks();
    if (!query) {
      return list;
    }
    return list.filter((b) =>
      b.shortName.toLowerCase().includes(query) ||
      b.name.toLowerCase().includes(query) ||
      b.code.toLowerCase().includes(query) ||
      b.bin.includes(query)
    );
  });

  readonly selectedBank = computed(() => {
    const bin = this.selectedBankBin();
    return this.banks().find((b) => b.bin === bin) || null;
  });

  readonly sampleTransferCode = computed(() => {
    const cleanPrefix = (this.transferPrefix() || 'HP')
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, '') || 'HP';
    return `${cleanPrefix}9A01210`;
  });

  readonly sampleSyntaxPattern = computed(() => {
    const cleanPrefix = (this.transferPrefix() || 'HP')
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, '') || 'HP';
    return `${cleanPrefix} + [MÃ LỚP] + [TÊN HS] + [THÁNG]`;
  });

  readonly isInfoComplete = computed(() => {
    const bin = this.selectedBankBin().trim();
    const accNum = this.accountNumber().trim().replace(/\s+/g, '');
    const accName = this.accountName().trim();
    return bin.length === 6 && accNum.length >= 4 && accName.length >= 2;
  });

  readonly isFormValid = computed(() => this.isInfoComplete());

  readonly liveQrUrl = computed(() => {
    if (!this.isInfoComplete()) {
      return null;
    }

    const bin = this.selectedBankBin().trim();
    const accNum = this.accountNumber().trim().replace(/\s+/g, '');
    const name = encodeURIComponent(this.accountName().trim().toUpperCase());
    const transferCode = encodeURIComponent(this.sampleTransferCode());

    return `https://img.vietqr.io/image/${bin}-${accNum}-compact2.png?amount=50000&addInfo=${transferCode}&accountName=${name}`;
  });

  // ─── LIFECYCLE ──────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadBanks();
    this.loadPaymentSetting();
  }

  loadBanks(): void {
    this.loadingBanks.set(true);
    this.paymentSettingService.getBanks().subscribe({
      next: (res) => {
        this.loadingBanks.set(false);
        if (res.success && res.data && res.data.length > 0) {
          this.banks.set(res.data);
          const current = res.data.find((b) => b.bin === this.selectedBankBin());
          if (current) {
            this.selectedBankName.set(current.shortName);
          }
        }
      },
      error: () => {
        this.loadingBanks.set(false);
      }
    });
  }

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
        this.toastService.error('Không thể tải cấu hình tài khoản. Vui lòng thử lại sau.');
      }
    });
  }

  // ─── ACTIONS ────────────────────────────────────────────────────────────────
  onBankChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const bin = select.value;
    const found = this.banks().find((b) => b.bin === bin);
    if (found) {
      this.selectedBankBin.set(found.bin);
      this.selectedBankName.set(found.shortName);
    }
  }

  onBankSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.bankSearchQuery.set(input.value);
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

  copyToClipboard(text: string, label: string): void {
    if (!text) return;
    if (navigator?.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        this.toastService.success(`Đã sao chép ${label}: ${text}`);
      }).catch(() => {
        this.fallbackCopy(text, label);
      });
    } else {
      this.fallbackCopy(text, label);
    }
  }

  private fallbackCopy(text: string, label: string): void {
    try {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      this.toastService.success(`Đã sao chép ${label}: ${text}`);
    } catch {
      this.toastService.warning(`Không thể tự động sao chép. Hãy chọn và copy thủ công: ${text}`);
    }
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
      next: () => {
        this.saving.set(false);
        this.isConfigured.set(true);
        this.toastService.success('Lưu cấu hình tài khoản VietQR thành công!');
      },
      error: (err) => {
        this.saving.set(false);
        const msg = err.error?.message || 'Có lỗi xảy ra khi lưu cấu hình';
        this.toastService.error(msg);
      }
    });
  }
}
