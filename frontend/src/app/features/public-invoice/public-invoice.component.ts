import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-public-invoice',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="min-height: 100vh; background: #f8fafc; padding: 20px 16px; display: flex; justify-content: center;">
      <div style="background: white; border-radius: 16px; border: 1px solid #e2e8f0; width: 100%; max-width: 440px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
        <div style="text-align: center; margin-bottom: 20px;">
          <h2 style="font-size: 18px; font-weight: 700;">PHIẾU BÁO HỌC PHÍ</h2>
          <p style="font-size: 13px; color: #64748b;">Tháng 10/2026</p>
        </div>

        <div style="background: #f1f5f9; border-radius: 10px; padding: 14px; margin-bottom: 20px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
            <span style="color: #64748b; font-size: 14px;">Học sinh:</span>
            <span style="font-weight: 600;">Trần Minh Quân (Tít)</span>
          </div>
          <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
            <span style="color: #64748b; font-size: 14px;">Số buổi học:</span>
            <span style="font-weight: 600;">8 buổi</span>
          </div>
          <div style="display: flex; justify-content: space-between; padding-top: 8px; border-top: 1px dashed #cbd5e1;">
            <span style="color: #0f172a; font-weight: 700; font-size: 15px;">Tổng thanh toán:</span>
            <span style="color: #2563eb; font-weight: 700; font-size: 18px;">1.200.000 đ</span>
          </div>
        </div>

        <div style="text-align: center; margin: 20px 0;">
          <div style="background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 12px; padding: 16px; display: inline-block;">
            <p style="font-size: 13px; font-weight: 600; color: #475569; margin-bottom: 8px;">MÃ VIETQR NAPAS247</p>
            <div style="width: 200px; height: 200px; background: #e2e8f0; margin: 0 auto; display: flex; align-items: center; justify-content: center; border-radius: 8px; font-size: 12px; color: #64748b;">
              [VietQR Code 200x200]
            </div>
            <p style="font-size: 12px; color: #64748b; margin-top: 8px;">Nội dung: <b>HP T10 TIT 008</b></p>
          </div>
        </div>

        <button style="width: 100%; padding: 12px; background: #2563eb; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer;">
          Sao chép số tài khoản
        </button>
      </div>
    </div>
  `
})
export class PublicInvoiceComponent {}
