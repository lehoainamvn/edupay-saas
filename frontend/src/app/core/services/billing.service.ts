import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  BillingPreview,
  Invoice,
  InvoiceBatchGenerateRequest,
  InvoicePaymentManualRequest,
  InvoiceStatus,
  PublicInvoice
} from '../models/billing.models';

@Injectable({
  providedIn: 'root'
})
export class BillingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/billing`;
  private readonly publicApiUrl = `${environment.apiBaseUrl}/public/invoices`;

  /**
   * Lấy dữ liệu xem trước bảng tính học phí cả lớp theo tháng
   */
  getBillingPreview(classId: string, month: string): Observable<ApiResponse<BillingPreview>> {
    const params = new HttpParams()
      .set('classId', classId)
      .set('month', month);
    return this.http.get<ApiResponse<BillingPreview>>(`${this.apiUrl}/preview`, { params });
  }

  /**
   * Xuất phiếu thu hàng loạt cho các học sinh được chọn
   */
  generateInvoices(payload: InvoiceBatchGenerateRequest): Observable<ApiResponse<Invoice[]>> {
    return this.http.post<ApiResponse<Invoice[]>>(`${this.apiUrl}/invoices/generate`, payload);
  }

  /**
   * Lấy danh sách phiếu thu đã xuất (lọc theo lớp, tháng, trạng thái)
   */
  getInvoices(filters?: {
    classId?: string;
    month?: string;
    status?: InvoiceStatus;
  }): Observable<ApiResponse<Invoice[]>> {
    let params = new HttpParams();
    if (filters?.classId) params = params.set('classId', filters.classId);
    if (filters?.month) params = params.set('month', filters.month);
    if (filters?.status) params = params.set('status', filters.status);
    return this.http.get<ApiResponse<Invoice[]>>(`${this.apiUrl}/invoices`, { params });
  }

  /**
   * Chi tiết 1 phiếu thu theo ID
   */
  getInvoiceById(id: string): Observable<ApiResponse<Invoice>> {
    return this.http.get<ApiResponse<Invoice>>(`${this.apiUrl}/invoices/${id}`);
  }

  /**
   * Hủy phiếu thu (để mở khóa sửa điểm danh)
   */
  cancelInvoice(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/invoices/${id}/cancel`, {});
  }

  /**
   * Gạch nợ thủ công (tiền mặt / CK)
   */
  markAsPaid(id: string, payload: InvoicePaymentManualRequest): Observable<ApiResponse<Invoice>> {
    return this.http.post<ApiResponse<Invoice>>(`${this.apiUrl}/invoices/${id}/mark-paid`, payload);
  }

  /**
   * Tra cứu phiếu thu công khai cho phụ huynh (/p/:hash)
   */
  getPublicInvoice(publicHash: string): Observable<ApiResponse<PublicInvoice>> {
    return this.http.get<ApiResponse<PublicInvoice>>(`${this.publicApiUrl}/${publicHash}`);
  }
}
