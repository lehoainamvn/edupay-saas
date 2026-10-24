import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding: 24px; max-width: 1000px; margin: 0 auto;">
      <h2 style="font-size: 24px; font-weight: 700;">Học phí & Đối soát VietQR</h2>
      <p style="color: #64748b; margin-top: 4px;">Tính tiền theo số buổi thực tế, sinh VietQR và gạch nợ</p>
    </div>
  `
})
export class BillingComponent {}
