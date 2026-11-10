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
            <!-- Header Phiếu Thu -->
            <div class="header">
              <div class="brandTag">EDUPAY SAAS · CỔNG PHỤ HUYNH</div>
              <h1 class="title">PHIẾU BÁO HỌC PHÍ</h1>
              <p class="period">Kỳ học: {{ formatMonthVN(inv.billingMonth) }}</p>

              <div class="statusWrap">
                @if (inv.status === 'PAID') {
                  <span class="statusBadge paid">✓ Đã thanh toán thành công</span>
                } @else if (hasReportedTransfer()) {
                  <span class="statusBadge reported">⏱ Đã báo chuyển khoản · Chờ đối soát</span>
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
                  <span class="label">Tên ở nhà (Biệt danh):</span>
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
                <span class="label">Đơn giá áp dụng:</span>
                <span class="value">{{ formatCurrency(inv.unitPrice) }}</span>
              </div>

              @if (inv.extraFee && inv.extraFee > 0) {
                <div class="infoRow">
                  <span class="label">Phụ phí (sách, tài liệu):</span>
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
                <h3 class="datesTitle">Lịch các buổi con đã có mặt ({{ inv.attendedDates.length }} buổi):</h3>
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
                <p class="qrHint">Mở ứng dụng ngân hàng bất kỳ để quét mã thanh toán tức thì đúng từng đồng</p>

                @if (inv.vietQrUrl) {
                  <div class="qrWrap">
                    <img [src]="inv.vietQrUrl" alt="Mã VietQR nộp học phí" class="qrImage" />
                  </div>

                  <div class="qrActionRow">
                    <button
                      type="button"
                      class="btnDownloadQr"
                      (click)="downloadQrImage(inv.vietQrUrl, inv.studentName)"
                    >
                      <svg class="btnIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" stroke-width="2"/>
                        <polyline points="7 10 12 15 17 10" stroke-width="2"/>
                        <line x1="12" y1="15" x2="12" y2="3" stroke-width="2"/>
                      </svg>
                      Tải ảnh QR về máy
                    </button>

                    <button
                      type="button"
                      class="btnCopyAll"
                      [class.copied]="copiedField() === 'allInfo'"
                      (click)="copyAllTransferInfo(inv)"
                    >
                      @if (copiedField() === 'allInfo') {
                        <span>✓ Đã sao chép</span>
                      } @else {
                        <span>Sao chép STK &amp; Nội dung</span>
                      }
                    </button>
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
                        {{ copiedField() === 'accNum' ? '✓ Đã chép' : 'Sao chép' }}
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
                        {{ copiedField() === 'code' ? '✓ Đã chép' : 'Sao chép' }}
                      </button>
                    </div>
                  </div>
                </div>

                <!-- Nút hành động chính: Tôi đã chuyển khoản -->
                <div class="footerActions">
                  <button
                    type="button"
                    class="fullBtn confirmPaidBtn"
                    (click)="openReportModal()"
                  >
                    <span>✓ Tôi đã chuyển khoản</span>
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
              <span class="footerNote">Hệ thống đối soát học phí tự động VietQR Napas247 · EduPay SaaS</span>
            </div>
          }
        }
      </div>

      <!-- Modal Xác nhận "Tôi đã chuyển khoản" -->
      @if (showReportModal()) {
        <div class="modalBackdrop" (click)="closeReportModal()">
          <div class="modalCard" (click)="$event.stopPropagation()">
            <div class="modalHeader">
              <div class="modalBadge">XÁC NHẬN CHUYỂN KHOẢN</div>
              <h3>Đã ghi nhận thông báo của ba mẹ</h3>
            </div>

            <div class="modalBody">
              <p>
                Cảm ơn Quý phụ huynh đã chuyển khoản học phí cho bé <strong>{{ invoice()?.studentName }}</strong>.
              </p>
              <div class="instructionBox">
                <ul>
                  <li>Hệ thống ngân hàng Napas247 sẽ tự động đối soát nội dung <code>{{ invoice()?.transferCode }}</code> trong 1–3 phút.</li>
                  <li>Sau khi hệ thống nhận được biến động số dư, trạng thái phiếu thu này sẽ tự động chuyển sang <strong>"Đã thanh toán"</strong>.</li>
                </ul>
              </div>
            </div>

            <div class="modalFooter">
              <button type="button" class="btnModalPrimary" (click)="confirmReportTransfer()">
                Đã hiểu &amp; Đóng
              </button>
            </div>
          </div>
        </div>
      }
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
      padding: 40px 16px;
      color: var(--muted);
    }

    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid var(--border);
      border-top-color: var(--blue);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 16px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .errorIcon {
      width: 48px;
      height: 48px;
      color: #ef4444;
      margin-bottom: 12px;
    }

    .header {
      text-align: center;
      margin-bottom: 24px;
      border-bottom: 1px dashed var(--border);
      padding-bottom: 20px;
    }

    .brandTag {
      font-size: 11px;
      font-weight: 700;
      color: var(--blue);
      letter-spacing: 0.05em;
      margin-bottom: 4px;
    }

    .title {
      font-size: 20px;
      font-weight: 800;
      color: var(--ink);
      margin: 0 0 4px;
    }

    .period {
      font-size: 14px;
      color: var(--muted);
      margin: 0 0 12px;
    }

    .statusBadge {
      display: inline-block;
      padding: 4px 12px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
    }

    .statusBadge.pending {
      background: var(--amber-soft);
      color: var(--amber-ink);
    }

    .statusBadge.reported {
      background: var(--blue-soft);
      color: var(--blue);
    }

    .statusBadge.paid {
      background: var(--green-soft);
      color: var(--green-ink);
    }

    .breakdownBox {
      background: #f8fafc;
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 16px;
      margin-bottom: 20px;
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
      background: #e2e8f0;
      padding: 2px 8px;
      border-radius: 6px;
      font-size: 13px;
    }

    .infoRow .value.fee {
      color: var(--amber-ink);
    }

    .infoRow .value.discount {
      color: var(--green);
    }

    .totalDivider {
      height: 1px;
      background: var(--border);
      margin: 12px 0;
    }

    .totalRow {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
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
      margin-bottom: 24px;
      background: #ffffff;
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 14px;
    }

    .datesTitle {
      font-size: 13px;
      font-weight: 700;
      color: var(--muted);
      margin: 0 0 10px;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .dateBadges {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .dateBadge {
      display: inline-block;
      padding: 4px 9px;
      background: var(--blue-soft);
      color: var(--blue);
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
    }

    /* QR Container */
    .qrContainer {
      text-align: center;
      margin-bottom: 24px;
    }

    .qrKicker {
      font-size: 11px;
      font-weight: 800;
      color: var(--muted);
      letter-spacing: 0.05em;
      margin-bottom: 4px;
    }

    .qrHint {
      font-size: 13px;
      color: var(--muted);
      margin: 0 0 16px;
    }

    .qrWrap {
      display: inline-block;
      padding: 12px;
      background: var(--white);
      border: 2px solid var(--border);
      border-radius: 16px;
      margin-bottom: 14px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
    }

    .qrImage {
      display: block;
      width: 250px;
      height: 250px;
      object-fit: contain;
    }

    .qrActionRow {
      display: flex;
      gap: 10px;
      justify-content: center;
      margin-bottom: 20px;
    }

    .btnDownloadQr, .btnCopyAll {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      height: 44px;
      padding: 0 16px;
      border-radius: 10px;
      font-size: 13px;
      font-weight: 700;
      cursor: pointer;
      border: 1px solid var(--border);
      background: var(--white);
      color: var(--ink);
      transition: all 0.2s;
    }

    .btnDownloadQr:hover, .btnCopyAll:hover {
      background: var(--canvas);
      border-color: var(--blue);
      color: var(--blue);
    }

    .btnCopyAll.copied {
      background: var(--green-soft);
      border-color: var(--green);
      color: var(--green-ink);
    }

    .btnIcon {
      width: 16px;
      height: 16px;
    }

    /* Transfer Details */
    .transferDetails {
      background: var(--canvas);
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 14px 16px;
      margin-bottom: 20px;
      text-align: left;
    }

    .transferItem {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      border-bottom: 1px solid var(--border);
      font-size: 14px;
    }

    .transferItem:last-child {
      border-bottom: none;
    }

    .transferItem.highlight {
      background: #eff6ff;
      margin: 8px -16px -14px;
      padding: 12px 16px;
      border-bottom-left-radius: 14px;
      border-bottom-right-radius: 14px;
      border-top: 1px solid #bfdbfe;
    }

    .tLabel {
      color: var(--muted);
      font-size: 13px;
    }

    .tValue {
      color: var(--ink);
      font-weight: 700;
    }

    .tValue.num {
      font-family: monospace;
      font-size: 15px;
      letter-spacing: 0.05em;
    }

    .tCode {
      background: var(--white);
      border: 1px solid #93c5fd;
      padding: 4px 8px;
      border-radius: 6px;
      font-family: monospace;
      font-size: 14px;
      font-weight: 800;
      color: var(--blue);
      letter-spacing: 0.05em;
    }

    .tActionWrap {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .copyBtn {
      border: 1px solid var(--border);
      background: var(--white);
      color: var(--muted);
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      min-height: 32px;
    }

    .copyBtn.copied {
      background: var(--green-soft);
      border-color: var(--green);
      color: var(--green-ink);
    }

    .footerActions {
      margin-top: 16px;
    }

    .fullBtn {
      width: 100%;
      height: 48px;
      border-radius: 12px;
      font-size: 15px;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      border: none;
      transition: background 0.2s, transform 0.1s;
    }

    .fullBtn:active {
      transform: scale(0.98);
    }

    .confirmPaidBtn {
      background: var(--green);
      color: var(--white);
    }

    .confirmPaidBtn:hover {
      background: #047857;
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
      padding-top: 14px;
      border-top: 1px solid var(--border);
    }

    .footerNote {
      font-size: 11px;
      color: var(--subtle);
    }

    /* Modal Backdrop & Card */
    .modalBackdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
      z-index: 999;
    }

    .modalCard {
      background: var(--white);
      border-radius: 18px;
      width: 100%;
      max-width: 420px;
      padding: 24px;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
      animation: modalIn 0.2s ease-out;
    }

    @keyframes modalIn {
      from { transform: scale(0.95); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }

    .modalBadge {
      font-size: 11px;
      font-weight: 800;
      color: var(--green);
      letter-spacing: 0.05em;
      margin-bottom: 6px;
    }

    .modalHeader h3 {
      margin: 0 0 12px;
      font-size: 18px;
      font-weight: 800;
      color: var(--ink);
    }

    .modalBody p {
      font-size: 14px;
      color: var(--muted);
      line-height: 1.5;
      margin: 0 0 14px;
    }

    .instructionBox {
      background: var(--canvas);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 12px 14px;
      margin-bottom: 20px;
    }

    .instructionBox ul {
      margin: 0;
      padding-left: 18px;
      font-size: 13px;
      color: var(--ink);
      line-height: 1.6;
    }

    .instructionBox code {
      background: #e2e8f0;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 700;
      color: var(--blue);
    }

    .modalFooter {
      display: flex;
      justify-content: flex-end;
    }

    .btnModalPrimary {
      height: 44px;
      padding: 0 24px;
      background: var(--blue);
      color: var(--white);
      border: none;
      border-radius: 10px;
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
    }

    .btnModalPrimary:hover {
      background: #1d4ed8;
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
  readonly hasReportedTransfer = signal<boolean>(false);
  readonly showReportModal = signal<boolean>(false);
  private copyTimer: ReturnType<typeof setTimeout> | null = null;

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
        if (err.status === 429) {
          this.error.set('Bạn đã thực hiện tra cứu quá nhiều lần trong 1 phút. Vui lòng đợi ít phút rồi tải lại trang.');
        } else {
          this.error.set(err.error?.message || 'Không tìm thấy phiếu thu tương ứng');
        }
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

  downloadQrImage(imageUrl: string, studentName: string): void {
    if (!imageUrl) return;
    this.toastService.info('Đang tải ảnh mã VietQR về thiết bị...');

    fetch(imageUrl)
      .then(res => res.blob())
      .then(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const cleanName = (studentName || 'HocSinh').replace(/[^A-Za-z0-9]/g, '_');
        a.download = `VietQR_HocPhi_${cleanName}.png`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.toastService.success('Đã tải ảnh mã QR thành công!');
      })
      .catch(() => {
        // Fallback mở tab mới nếu fetch blob bị cors chặn
        window.open(imageUrl, '_blank');
      });
  }

  copyAllTransferInfo(inv: PublicInvoice): void {
    const text = [
      `Ngân hàng: ${inv.bankName || 'MBBank'}`,
      `Số tài khoản: ${inv.accountNumber || ''}`,
      `Chủ tài khoản: ${inv.accountName || ''}`,
      `Số tiền: ${this.formatCurrency(inv.totalAmount)}`,
      `Nội dung chuyển khoản: ${inv.transferCode}`
    ].join('\n');

    this.copyText(text, 'toàn bộ thông tin chuyển khoản', 'allInfo');
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

  openReportModal(): void {
    this.showReportModal.set(true);
  }

  closeReportModal(): void {
    this.showReportModal.set(false);
  }

  confirmReportTransfer(): void {
    this.hasReportedTransfer.set(true);
    this.showReportModal.set(false);
    this.toastService.success('Đã ghi nhận! Hệ thống đang tự động đối soát giao dịch.');
  }
}
