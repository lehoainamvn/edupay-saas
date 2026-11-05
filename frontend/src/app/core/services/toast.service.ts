import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastItem {
  id: string;
  type: ToastType;
  message: string;
  title?: string;
  duration: number;
}

/**
 * Service quản lý thông báo (Toast) tập trung cho toàn bộ ứng dụng EduPay SaaS.
 * BẮT BUỘC tuân thủ Luật P0: Toàn bộ hệ thống chỉ sử dụng duy nhất service này.
 */
@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private readonly _toasts = signal<ToastItem[]>([]);
  readonly toasts = this._toasts.asReadonly();

  /**
   * Thông báo thành công (Màu xanh lá - Thành công trong thao tác lưu, tạo, cập nhật, xuất phiếu)
   */
  success(message: string, title: string = 'Thành công', duration: number = 3500): void {
    this.display('success', message, title, duration);
  }

  /**
   * Thông báo lỗi / thất bại (Màu đỏ - Lỗi API, lỗi mạng, dữ liệu không hợp lệ)
   */
  error(message: string, title: string = 'Có lỗi xảy ra', duration: number = 4500): void {
    this.display('error', message, title, duration);
  }

  /**
   * Thông báo cảnh báo (Màu vàng cam - Nhắc nhở, điểm danh khóa, chưa có cấu hình)
   */
  warning(message: string, title: string = 'Cảnh báo', duration: number = 4000): void {
    this.display('warning', message, title, duration);
  }

  /**
   * Thông báo thông tin (Màu xanh dương - Hướng dẫn, sao chép, gợi ý)
   */
  info(message: string, title: string = 'Thông tin', duration: number = 3500): void {
    this.display('info', message, title, duration);
  }

  /**
   * Phương thức chung hiển thị toast - Linh hoạt nhận (type, message) hoặc (message, type)
   */
  show(
    firstArg: ToastType | string,
    secondArg?: string | ToastType,
    title?: string,
    duration: number = 3500
  ): void {
    const validTypes: ToastType[] = ['success', 'error', 'warning', 'info'];
    let type: ToastType = 'info';
    let message = '';

    if (validTypes.includes(firstArg as ToastType)) {
      type = firstArg as ToastType;
      message = typeof secondArg === 'string' ? secondArg : '';
    } else {
      message = String(firstArg);
      if (secondArg && validTypes.includes(secondArg as ToastType)) {
        type = secondArg as ToastType;
      } else {
        type = 'success';
      }
    }

    this.display(type, message, title, duration);
  }

  private display(type: ToastType, message: string, title?: string, duration: number = 3500): void {
    const id = 'toast_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const item: ToastItem = {
      id,
      type,
      message,
      title,
      duration
    };

    // Giữ tối đa 4 thông báo cùng lúc trên màn hình (đẩy cái mới nhất vào)
    this._toasts.update(current => {
      const updated = [...current, item];
      return updated.slice(-4);
    });

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }
  }

  /**
   * Đóng một thông báo theo ID
   */
  remove(id: string): void {
    this._toasts.update(current => current.filter(t => t.id !== id));
  }

  /**
   * Đóng toàn bộ thông báo đang hiện
   */
  clear(): void {
    this._toasts.set([]);
  }
}
