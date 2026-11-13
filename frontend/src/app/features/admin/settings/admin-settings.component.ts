import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { PaymentSettingService } from '../../../core/services/payment-setting.service';
import { BankInfo, PaymentSetting, POPULAR_BANKS } from '../../../core/models/payment-setting.models';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './admin-settings.component.html',
  styleUrls: ['./admin-settings.component.css'],
})
export class AdminSettingsComponent implements OnInit {
  private readonly paymentService = inject(PaymentSettingService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  loading = signal<boolean>(true);
  saving = signal<boolean>(false);
  banks = signal<BankInfo[]>(POPULAR_BANKS);
  currentSetting = signal<PaymentSetting | null>(null);

  form = this.fb.group({
    bankBin: ['970422', Validators.required],
    accountNumber: ['', [Validators.required, Validators.pattern(/^[0-9A-Za-z]{6,25}$/)]],
    accountName: ['', [Validators.required, Validators.maxLength(100)]],
    transferPrefix: ['EDUPAY', [Validators.required, Validators.maxLength(20), Validators.pattern(/^[A-Za-z0-9_-]+$/)]],
  });

  readonly selectedBank = computed(() => {
    const bin = this.form.get('bankBin')?.value;
    return this.banks().find((b) => b.bin === bin) || this.banks()[0];
  });

  readonly previewQrUrl = computed(() => {
    const bin = this.form.get('bankBin')?.value || '970422';
    const acc = this.form.get('accountNumber')?.value || '0123456789';
    const name = encodeURIComponent(this.form.get('accountName')?.value || 'TRUNG TAM');
    const prefix = this.form.get('transferPrefix')?.value || 'EDUPAY';
    return `https://img.vietqr.io/image/${bin}-${acc}-compact2.png?amount=200000&addInfo=${prefix}%20TEST&accountName=${name}`;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);

    this.paymentService.getBanks().subscribe({
      next: (res) => {
        if (res.success && res.data && res.data.length > 0) {
          this.banks.set(res.data);
        }
      },
      error: () => {},
    });

    this.paymentService.getPaymentSetting().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.currentSetting.set(res.data);
          this.form.patchValue({
            bankBin: res.data.bankBin || '970422',
            accountNumber: res.data.accountNumber || '',
            accountName: res.data.accountName || '',
            transferPrefix: res.data.transferPrefix || 'EDUPAY',
          });
        }
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  onAccountNameInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    this.form.controls.accountName.setValue(upper, { emitEvent: false });
  }

  saveSettings(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const val = this.form.value;
    const selectedBank = this.banks().find((b) => b.bin === val.bankBin);

    const payload = {
      bankBin: val.bankBin!,
      bankName: selectedBank ? selectedBank.shortName : 'MBBank',
      accountNumber: val.accountNumber!,
      accountName: val.accountName!.toUpperCase(),
      transferPrefix: val.transferPrefix ? val.transferPrefix.toUpperCase() : 'EDUPAY',
    };

    this.paymentService.savePaymentSetting(payload).subscribe({
      next: (res) => {
        this.saving.set(false);
        if (res.success && res.data) {
          this.currentSetting.set(res.data);
          this.toastService.success('Đã lưu cấu hình tài khoản VietQR nhận học phí trung tâm');
        }
      },
      error: (err) => {
        this.saving.set(false);
        const detailedError = err?.error?.errors?.[0] || err?.error?.message || 'Có lỗi khi lưu cài đặt thanh toán';
        this.toastService.error(detailedError);
      },
    });
  }
}

