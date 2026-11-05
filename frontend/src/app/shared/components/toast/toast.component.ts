import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastItem } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (toastService.toasts().length > 0) {
      <div class="toastContainer" role="region" aria-label="Thông báo hệ thống">
        @for (toast of toastService.toasts(); track toast.id) {
          <div
            class="toastItem"
            [class.success]="toast.type === 'success'"
            [class.error]="toast.type === 'error'"
            [class.warning]="toast.type === 'warning'"
            [class.info]="toast.type === 'info'"
            role="alert"
          >
            <!-- Biểu tượng Icon SVG đồng bộ -->
            <div class="toastIconWrap">
              @switch (toast.type) {
                @case ('success') {
                  <svg class="toastSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                    <polyline points="22 4 12 14.01 9 11.01" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                }
                @case ('error') {
                  <svg class="toastSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <circle cx="12" cy="12" r="10" stroke-width="2.2"/>
                    <line x1="15" y1="9" x2="9" y2="15" stroke-width="2.2" stroke-linecap="round"/>
                    <line x1="9" y1="9" x2="15" y2="15" stroke-width="2.2" stroke-linecap="round"/>
                  </svg>
                }
                @case ('warning') {
                  <svg class="toastSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" stroke-width="2.2"/>
                    <line x1="12" y1="9" x2="12" y2="13" stroke-width="2.2" stroke-linecap="round"/>
                    <line x1="12" y1="17" x2="12.01" y2="17" stroke-width="2.2" stroke-linecap="round"/>
                  </svg>
                }
                @case ('info') {
                  <svg class="toastSvg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <circle cx="12" cy="12" r="10" stroke-width="2.2"/>
                    <line x1="12" y1="16" x2="12" y2="12" stroke-width="2.2" stroke-linecap="round"/>
                    <line x1="12" y1="8" x2="12.01" y2="8" stroke-width="2.2" stroke-linecap="round"/>
                  </svg>
                }
              }
            </div>

            <!-- Nội dung thông báo -->
            <div class="toastContent">
              @if (toast.title) {
                <div class="toastTitle">{{ toast.title }}</div>
              }
              <div class="toastMessage">{{ toast.message }}</div>
            </div>

            <!-- Nút đóng thủ công -->
            <button
              type="button"
              class="toastCloseBtn"
              (click)="toastService.remove(toast.id)"
              aria-label="Đóng thông báo"
              title="Đóng thông báo"
            >
              ✕
            </button>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    :host {
      display: contents;
    }

    .toastContainer {
      position: fixed;
      top: 24px;
      right: 24px;
      z-index: 999999;
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-width: 420px;
      width: calc(100% - 48px);
      pointer-events: none;
    }

    @media (max-width: 640px) {
      .toastContainer {
        top: 16px;
        left: 16px;
        right: 16px;
        width: calc(100% - 32px);
      }
    }

    .toastItem {
      pointer-events: auto;
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 16px;
      background: #ffffff;
      border-radius: 14px;
      border: 1px solid #e2e8f0;
      border-left-width: 5px;
      box-shadow:
        0 10px 25px -5px rgba(15, 23, 42, 0.12),
        0 4px 6px -2px rgba(15, 23, 42, 0.04);
      animation: slideInDown 0.28s cubic-bezier(0.16, 1, 0.3, 1);
      transition: all 0.2s ease;
      box-sizing: border-box;
    }

    @keyframes slideInDown {
      from {
        opacity: 0;
        transform: translateY(-16px) scale(0.96);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }

    /* ─── CÁC BIẾN THỂ MÀU CHUẨN DESIGN SYSTEM ───────────────────────────── */
    .toastItem.success {
      border-left-color: #10b981;
    }
    .toastItem.success .toastIconWrap {
      color: #059669;
      background: #ecfdf5;
    }
    .toastItem.success .toastTitle {
      color: #065f46;
    }

    .toastItem.error {
      border-left-color: #ef4444;
    }
    .toastItem.error .toastIconWrap {
      color: #dc2626;
      background: #fef2f2;
    }
    .toastItem.error .toastTitle {
      color: #991b1b;
    }

    .toastItem.warning {
      border-left-color: #f59e0b;
    }
    .toastItem.warning .toastIconWrap {
      color: #d97706;
      background: #fffbeb;
    }
    .toastItem.warning .toastTitle {
      color: #92400e;
    }

    .toastItem.info {
      border-left-color: #3b82f6;
    }
    .toastItem.info .toastIconWrap {
      color: #2563eb;
      background: #eff6ff;
    }
    .toastItem.info .toastTitle {
      color: #1e40af;
    }

    /* ─── THÀNH PHẦN BÊN TRONG ─────────────────────────────────────────── */
    .toastIconWrap {
      width: 32px;
      height: 32px;
      border-radius: 9px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .toastSvg {
      width: 18px;
      height: 18px;
    }

    .toastContent {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 2px;
      min-width: 0;
    }

    .toastTitle {
      font-size: 13.5px;
      font-weight: 700;
      line-height: 1.3;
    }

    .toastMessage {
      font-size: 13px;
      color: #334155;
      line-height: 1.45;
      word-break: break-word;
    }

    .toastCloseBtn {
      border: 0;
      background: transparent;
      color: #94a3b8;
      width: 28px;
      height: 28px;
      border-radius: 6px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
      flex-shrink: 0;
      transition: all 0.15s ease;
      margin-top: 2px;
    }

    .toastCloseBtn:hover {
      background: #f1f5f9;
      color: #334155;
    }
  `]
})
export class ToastComponent {
  readonly toastService = inject(ToastService);
}

