import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { BillingService } from '../../core/services/billing.service';
import { ToastService } from '../../core/services/toast.service';
import { PublicInvoice } from '../../core/models/billing.models';

@Component({
  selector: 'app-public-invoice',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="publicContainer">

      <div class="invoiceCard">
        @if (loading()) {
          <div class="loadingState">
            <div class="spinner"></div>
            <p>Đang tải thông tin phiếu thu...</p>
          </div>
        } @else if (error()) {
          <div class="errorState">
            <svg class="errorIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <circle cx="12" cy="12" r="10" stroke-width="2"/>
              <line x1="12" y1="8" x2="12" y2="12" stroke-width="2"/>
              <line x1="12" y1="16" x2="12.01" y2="16" stroke-width="2"/>
            </svg>
            <h3>Không tìm thấy phiếu thu</h3>
            <p>{{ error() }}</p>
          </div>
        } @else {
          @if (invoice(); as inv) {
            <!-- Header -->
          <div class="header">
            <div class="brandTag">EDUPAY SAAS</div>
            <h1 class="title">PHIẾU BÁO HỌC PHÍ</h1>
            <p class="period">Kỳ học: {{ formatMonthVN(inv.billingMonth) }}</p>

            <div class="statusWrap">
              @if (inv.status === 'PAID') {
                <span class="statusBadge paid">Đã thanh toán</span>
              } @else {
                <span class="statusBadge pending">Chờ thanh toán</span>
              }
            </div>
          </div>

          <!-- Chi tiết học sinh & số tiền -->
          <div class="breakdownBox">
            <div class="infoRow">
              <span class="label">Học sinh:</span>
              <strong class="value">{{ inv.studentName }}</strong>
            </div>

            @if (inv.nickname) {
              <div class="infoRow">
                <span class="label">Tên ở nhà:</span>
                <span class="value nick">{{ inv.nickname }}</span>
              </div>
            }

            <div class="infoRow">
              <span class="label">Lớp học:</span>
              <span class="value">{{ inv.className }}</span>
            </div>

            <div class="infoRow">
              <span class="label">Số buổi thực học:</span>
              <strong class="value countVal">{{ inv.attendedCount }} buổi</strong>
            </div>

            <div class="infoRow">
              <span class="label">Đơn giá:</span>
              <span class="value">{{ formatCurrency(inv.unitPrice) }}</span>
            </div>

            @if (inv.extraFee && inv.extraFee > 0) {
              <div class="infoRow">
                <span class="label">Phụ phí (tài liệu, sách):</span>
                <span class="value fee">+{{ formatCurrency(inv.extraFee) }}</span>
              </div>
            }

            @if (inv.discount && inv.discount > 0) {
              <div class="infoRow">
                <span class="label">Giảm trừ / Học bổng:</span>
                <span class="value discount">-{{ formatCurrency(inv.discount) }}</span>
              </div>
            }

            <div class="totalDivider"></div>

            <div class="totalRow">
              <span class="totalLabel">Tổng thanh toán:</span>
              <strong class="totalValue">{{ formatCurrency(inv.totalAmount) }}</strong>
            </div>
          </div>

          <!-- Danh sách các buổi con đã học trong tháng -->
          @if (inv.attendedDates && inv.attendedDates.length > 0) {
            <div class="datesBox">
              <h3 class="datesTitle">Nhật ký các ngày con đã học:</h3>
              <div class="dateBadges">
                @for (d of inv.attendedDates; track d) {
                  <span class="dateBadge">{{ formatDate(d) }}</span>
                }
              </div>
            </div>
          }

          <!-- Khung Quét Mã VietQR Napas247 -->
          @if (inv.status !== 'PAID') {
            <div class="qrContainer">
              <div class="qrKicker">MÃ VIETQR NAPAS247 CHUẨN</div>
              <p class="qrHint">Mở ứng dụng ngân hàng bất kỳ để quét mã thanh toán tức thì</p>

              @if (inv.vietQrUrl) {
                <div class="qrWrap">
                  <img [src]="inv.vietQrUrl" alt="Mã VietQR nộp học phí" class="qrImage" />
                </div>
              }

              <!-- Bảng thông tin chuyển khoản sao chép 1 chạm -->
              <div class="transferDetails">
                <div class="transferItem">
                  <span class="tLabel">Ngân hàng:</span>
                  <strong class="tValue">{{ inv.bankName || 'MBBank' }}</strong>
                </div>

                <div class="transferItem">
                  <span class="tLabel">Số tài khoản:</span>
                  <div class="tActionWrap">
                    <strong class="tValue num">{{ inv.accountNumber || '0987654321' }}</strong>
                    <button
                      type="button"
                      class="copyBtn"
                      [class.copied]="copiedField() === 'accNum'"
                      (click)="copyText(inv.accountNumber || '0987654321', 'Số tài khoản', 'accNum')"
                    >
                      @if (copiedField() === 'accNum') {
                        <span>✓ Đã sao chép</span>
                      } @else {
                        <span>Sao chép</span>
                      }
                    </button>
                  </div>
                </div>

                <div class="transferItem">
                  <span class="tLabel">Chủ tài khoản:</span>
                  <strong class="tValue">{{ inv.accountName || 'GIAO VIEN' }}</strong>
                </div>

                <div class="transferItem highlight">
                  <span class="tLabel">Nội dung chuyển khoản chính xác:</span>
                  <div class="tActionWrap">
                    <code class="tCode">{{ inv.transferCode }}</code>
                    <button
                      type="button"
                      class="copyBtn"
                      [class.copied]="copiedField() === 'code'"
                      (click)="copyText(inv.transferCode, 'Nội dung chuyển khoản', 'code')"
                    >
                      @if (copiedField() === 'code') {
                        <span>✓ Đã sao chép</span>
                      } @else {
                        <span>Sao chép</span>
                      }
                    </button>
                  </div>
                </div>
              </div>

              <div class="footerActions">
                <button
                  type="button"
                  class="fullBtn primary"
                  [class.copied]="copiedField() === 'footerAcc'"
                  (click)="copyText(inv.accountNumber || '0987654321', 'Số tài khoản', 'footerAcc')"
                >
                  @if (copiedField() === 'footerAcc') {
                    <span>✓ Đã sao chép Số tài khoản</span>
                  } @else {
                    <span>Sao chép Số tài khoản</span>
                  }
                </button>
              </div>
            </div>
          } @else {
            <div class="paidNotice">
              <svg class="paidIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke-width="2"/>
                <polyline points="22 4 12 14.01 9 11.01" stroke-width="2"/>
              </svg>
              <h3>ĐÃ HOÀN TẤT THANH TOÁN</h3>
              <p>Giáo viên đã nhận đủ học phí {{ formatMonthVN(inv.billingMonth) }}. Xin chân thành cảm ơn Quý phụ huynh!</p>
            </div>
          }

            <div class="cardFooter">
              <span class="footerNote">Hệ thống quản lý học phí tự động EduPay SaaS</span>
            </div>
          }
        }
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      font-family: 'Be Vietnam Pro', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
      --ink: #0f172a;
      --muted: #64748b;
      --subtle: #94a3b8;
      --blue: #2563eb;
      --blue-soft: #eff6ff;
      --green: #059669;
      --green-soft: #dcfce7;
      --green-ink: #166534;
      --amber: #d97706;
      --amber-soft: #fef3c7;
      --amber-ink: #92400e;
      --border: #e2e8f0;
      --canvas: #f8fafc;
      --white: #ffffff;
    }

    * { box-sizing: border-box; }

    .publicContainer {
      min-height: 100vh;
      background: var(--canvas);
      padding: 24px 16px 60px;
      display: flex;
      justify-content: center;
      position: relative;
    }

    .invoiceCard {
      background: var(--white);
      border-radius: 20px;
      border: 1px solid var(--border);
      width: 100%;
      max-width: 480px;
      padding: 28px 24px;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
      align-self: flex-start;
    }

    .loadingState, .errorState {
      text-align: center;
      padding: 60px 20px;
      color: var(--muted);
    }

    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid var(--border);
      border-top-color: var(--blue);
      border-radius: 50%;
      margin: 0 auto 16px;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    .errorIcon {
      width: 48px;
      height: 48px;
      color: #ef4444;
      margin-bottom: 12px;
    }

    /* Header */
    .header {
      text-align: center;
      margin-bottom: 24px;
    }

    .brandTag {
      font-size: 11px;
      font-weight: 800;
      color: var(--blue);
      letter-spacing: 0.1em;
      margin-bottom: 4px;
    }

    .title {
      font-size: 22px;
      font-weight: 800;
      color: var(--ink);
      margin: 0 0 4px;
      letter-spacing: -0.02em;
    }

    .period {
      font-size: 14px;
      color: var(--muted);
      margin: 0 0 12px;
    }

    .statusBadge {
      display: inline-block;
      padding: 5px 14px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
    }

    .statusBadge.paid {
      background: var(--green-soft);
      color: var(--green-ink);
    }

    .statusBadge.pending {
      background: var(--amber-soft);
      color: var(--amber-ink);
    }

    /* Breakdown Box */
    .breakdownBox {
      background: #f8fafc;
      border-radius: 14px;
      padding: 18px;
      margin-bottom: 20px;
      border: 1px solid var(--border);
    }

    .infoRow {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      font-size: 14px;
    }

    .infoRow .label {
      color: var(--muted);
    }

    .infoRow .value {
      color: var(--ink);
      font-weight: 600;
    }

    .infoRow .value.nick {
      font-style: italic;
      color: var(--muted);
    }

    .infoRow .value.countVal {
      color: var(--blue);
    }

    .infoRow .value.fee {
      color: #ea580c;
    }

    .infoRow .value.discount {
      color: var(--green);
    }

    .totalDivider {
      height: 1px;
      border-top: 1px dashed #cbd5e1;
      margin: 14px 0 12px;
    }

    .totalRow {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .totalLabel {
      font-size: 15px;
      font-weight: 700;
      color: var(--ink);
    }

    .totalValue {
      font-size: 22px;
      font-weight: 800;
      color: var(--blue);
    }

    /* Dates Box */
    .datesBox {
      margin-bottom: 20px;
      padding: 14px;
      background: var(--white);
      border: 1px solid var(--border);
      border-radius: 12px;
    }

    .datesTitle {
      margin: 0 0 10px;
      font-size: 13px;
      font-weight: 700;
      color: var(--muted);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .dateBadges {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .dateBadge {
      background: var(--blue-soft);
      color: var(--blue);
      font-size: 12px;
      font-weight: 600;
      padding: 4px 8px;
      border-radius: 6px;
    }

    /* QR Box */
    .qrContainer {
      text-align: center;
      padding: 20px;
      border: 1px dashed #cbd5e1;
      border-radius: 16px;
      background: #fafaf9;
      margin-bottom: 20px;
    }

    .qrKicker {
      font-size: 12px;
      font-weight: 800;
      color: var(--muted);
      letter-spacing: 0.06em;
      margin-bottom: 4px;
    }

    .qrHint {
      font-size: 12px;
      color: var(--subtle);
      margin: 0 0 14px;
    }

    .qrWrap {
      display: inline-block;
      background: var(--white);
      padding: 12px;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      margin-bottom: 16px;
    }

    .qrImage {
      width: 210px;
      height: 210px;
      object-fit: contain;
      display: block;
    }

    .transferDetails {
      text-align: left;
      display: flex;
      flex-direction: column;
      gap: 10px;
      background: var(--white);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 14px;
      margin-bottom: 16px;
    }

    .transferItem {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .transferItem.highlight {
      background: #fffbeb;
      border: 1px solid #fef3c7;
      border-radius: 8px;
      padding: 8px;
    }

    .tLabel {
      font-size: 12px;
      color: var(--muted);
      font-weight: 600;
    }

    .tActionWrap {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
    }

    .tValue {
      font-size: 14px;
      color: var(--ink);
    }

    .tValue.num {
      font-family: monospace;
      font-size: 16px;
      font-weight: 700;
      color: var(--blue);
    }

    .tCode {
      font-size: 14px;
      font-weight: 800;
      color: #92400e;
      background: #fef3c7;
      padding: 2px 6px;
      border-radius: 4px;
      word-break: break-all;
    }

    .copyBtn {
      min-height: 32px;
      padding: 0 10px;
      border-radius: 6px;
      border: 1px solid var(--border);
      background: var(--white);
      font-size: 12px;
      font-weight: 600;
      color: var(--blue);
      cursor: pointer;
      white-space: nowrap;
    }

    .fullBtn {
      width: 100%;
      min-height: 48px;
      border: none;
      border-radius: 10px;
      font-size: 14px;
      font-weight: 700;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }

    .fullBtn.primary {
      background: var(--blue);
      color: var(--white);
    }

    /* Paid Notice */
    .paidNotice {
      text-align: center;
      padding: 30px 16px;
      background: var(--green-soft);
      border-radius: 16px;
      color: var(--green-ink);
      margin-bottom: 20px;
    }

    .paidIcon {
      width: 48px;
      height: 48px;
      margin-bottom: 10px;
    }

    .paidNotice h3 {
      margin: 0 0 6px;
      font-size: 18px;
      font-weight: 800;
    }

    .paidNotice p {
      margin: 0;
      font-size: 14px;
    }

    .cardFooter {
      text-align: center;
      padding-top: 12px;
      border-top: 1px solid var(--soft);
    }

    .footerNote {
      font-size: 11px;
      color: var(--subtle);
    }
  `]
})
export class PublicInvoiceComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly billingService = inject(BillingService);
  private readonly toastService = inject(ToastService);

  readonly invoice = signal<PublicInvoice | null>(null);
  readonly loading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly copiedField = signal<string | null>(null);
  private copyTimer: ReturnType<typeof setTimeout> | null = null;
  readonly toastMessage = signal<string | null>(null);

  ngOnInit(): void {
    const hash = this.route.snapshot.paramMap.get('hash');
    if (!hash) {
      this.loading.set(false);
      this.error.set('Mã tra cứu không hợp lệ');
      return;
    }

    this.billingService.getPublicInvoice(hash).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.invoice.set(res.data);
        } else {
          this.error.set('Không tìm thấy thông tin phiếu thu');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Không tìm thấy phiếu thu tương ứng');
      }
    });
  }

  formatCurrency(amount: number | null | undefined): string {
    if (amount == null) return '0 đ';
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
  }

  formatMonthVN(monthStr: string | undefined | null): string {
    if (!monthStr) return '';
    const parts = monthStr.split('-');
    if (parts.length === 2) {
      const y = parts[0];
      const m = parseInt(parts[1], 10);
      return `Tháng ${m}/${y}`;
    }
    return monthStr;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}`;
    }
    return dateStr;
  }

  copyText(text: string, label: string, fieldKey?: string): void {
    navigator.clipboard.writeText(text).then(() => {
      if (fieldKey) {
        if (this.copyTimer) clearTimeout(this.copyTimer);
        this.copiedField.set(fieldKey);
        this.copyTimer = setTimeout(() => {
          if (this.copiedField() === fieldKey) this.copiedField.set(null);
        }, 2200);
      }
      this.toastService.info(`Đã sao chép ${label}!`);
    });
  }
}
